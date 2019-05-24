/*
 *  checkLaneBarcodes.js    - Check issues in the Illumina output html. Output the mtl with marked issues.
 *                   If issue found, the Flowcell Summary table has background color red, otherwise, green
 * 	Author: jbu
 *  Versions:
 *  11-20-17    jbu     1.0.0 - initial version with barcode checker.
 *  11-21-17    jbu     1.0.1 - Addvalidation for cluster count, % lane and unknown barcode count.
 *  12-27-17    jbu     1.0.2 - Fix barcode bookmark for the multiple matches of the same barcode in different lane.  Also match barcode 6 to 8, change color difinition
 *  12-29-17    aga     1.0.3 - Changed handling of input arguments; Added dynamic output of options used.
 *  01-02-18    jbu     1.0.4 - Return result code =1 for validation error.  Formating the html better.
 *  01-22-18	jbu		1.0.5 - Change err condition for barcode matching (only unknow count > lane count). Fix bug for lane gap. (e.g. 1,2,3,5,6)
 *  02-21-18	jbu		1.0.6 - Change json error output to report only barcode matching error (match and qty low)
 *  03-5-18		jbu		1.0.7 - auto detect column variation due to Illumina report column variation
 *  03-13-18	jbu		1.0.8 - a. mask out the unknown AAAA exception (if 4 repeat AAAA, CCCC, GGGG, TTTT, ignore the checker),
 *  							b. no err output json if no error.
 *  03-15-18	jbu		1.0.9 - now report N type in unknown when total exceeds threashold.
 * 	07-12-18	jbu		1.1.0 - isValid false when bUnknownBarcodeHigh (report error)
 *  12-06-18    jwang   1.1.1 - fixed a bug of handling unknown table having "unknown" in the barcode field.
 *  01-25-19    agab    1.1.2 - Added check of Q30, and mean coverage combination.
 *
 */

var version = "1.1.2";
var debug = false;
var fs = require("fs");
var path = require("path");
var jsdom = require("jsdom");

var UnknowArray = [];
var bgclridx = 0;
var bgcolors = [];
var inFilePath = "";
var outFilePath = "";
var outJsonPath = "";
var dataJson = {
    flowcell: "",
    node: "checkLaneBarcodes",
    lanes: {}
};
var flowCellId = "";
var bIsValid = true;
var bMatchLargeUnknown = false;
var bLowerThanClusterMin = false;
var bLowerThanPercentMin = false;
var bLoweQScore = false;
var bUnknownBarcodeHigh = false;
var gwindow = null;
const color = {
    ok: "#aaffaa",
    error: "#ffaaaa",
    warning: "#ffffaa",
    invalid_barcode: "#777777"
};
//lane summary table column index
const lSIdx = {
    laneNum: 0,
    project: 1,
    sample: 2,
    barcode: 3,
    rawClusters: 4,
    pctLane: 5,
    perfectBarcode: 6,
    pctOneMismatch: 7,
    filteredClusters: 8, //This is the same as rawClusters, some report may not have this column which affect the following indexes
    yield: 9, // can be 8 or 9
    pctPFClusters: 10, // can be 9 or 10
    pctQ30Bases: 11, // can be 10 or 11 (used, will be determine idx on the fly)
    MeanQCScore: 12 // can be 11 or 12
};
//lane summary threashold
const lsCheck = {
    rawClustersMin: 1000000,
    pctLaneMin: 0.1
};
const ukCheck = {
    countMax: 1000000
};

function ensureLaneJson(lane) {
    if (!dataJson.lanes[lane]) {
        dataJson.lanes[lane] = {
            accessions: [],
            unknown: []
        };
    }
}
function insertUnknown(lane, uj) {
    ensureLaneJson(lane);
    dataJson.lanes[lane].unknown.push(uj);
}
function insertAccession(lane, aj) {
    ensureLaneJson(lane);
    dataJson.lanes[lane].accessions.push(aj);
}

loadParameters(function(isSuccess) {
    if (!isSuccess) {
        console.error(path.basename(__filename) + " Version " + version);
        console.error("Usage: node " + path.basename(__filename) + " [infile] [outfile] [-r runfolder] <-c minClusterCount> <-u maxUnknowCount> <-m Min Percentage>");
        console.error("Description :");
        console.error("\tInspect the Illumina laneBarcode.HTML and automatically find matches from unknow barcode list. Highlight the possible low cluster counts.");
        console.error("");
        console.error("Parameters :");
        console.error("\t-r runfolder : required, runfolder for saving the plm_runError1.json.");
        console.error("\t-c minClusterCount : optional, specify min cluster count. Default is 1,000,000.");
        console.error("\t-u maxUnknowCount : optonal, specify max count for unknown barcode. Default is 1,000,000.");
        console.error("\t-m Percentage : optional, specify min % of lane. Default is 0.1");
        console.error("\t-h : help text");

        process.exit(1);
    }
});

if (debug) {
    inFilePath = "C:\\Fulgent\\node_scripts\\node_scripts\\checkLaneBarcodes\\test\\laneBarcode-err-rc.html";
    //	inFilePath = "C:\\Fulgent\\node_scripts\\node_scripts\\checkLaneBarcodes\\test\\\\bug20180306_laneBarcode_checked.html";
    // v1.0.9	inFilePath = "C:\\Fulgent\\node_scripts\\node_scripts\\checkLaneBarcodes\\test\\\\foo-largeNerr-v1.0.9.html";

    outFilePath = "C:\\Fulgent\\node_scripts\\node_scripts\\checkLaneBarcodes\\test\\\\foo.html";
    outJsonPath = "C:\\Fulgent\\node_scripts\\node_scripts\\checkLaneBarcodes\\test\\";
}

if (outJsonPath.length < 1) {
    console.error("ERROR: -r  runfolder is required.");
    process.exit(1);
}
if (!fs.existsSync(inFilePath)) {
    console.error("ERROR: file " + inFilePath + " not exists.");
    process.exit(1);
}

var htmlSource = fs.readFileSync(inFilePath, "utf8");

call_jsdom(htmlSource, function(window) {
    gwindow = window;
    var $ = gwindow.$;
    //fix the table id
    var i = 0;
    $("table").each(function() {
        switch (i) {
            case 0:
                $(this).attr("id", "Menu");
                break;
            case 1:
                $(this).attr("id", "FlowcellSummary");
                break;
            case 2:
                $(this).attr("id", "LaneSummary");
                break;
            case 3:
                $(this).attr("id", "UnknownTable");
                break;
        }
        i++;
    });

    //get the flowcell id
    var m = $("td:eq( 0 )")
        .text()
        .match(/\w+/)[0];
    if (m.length > 0) dataJson.flowcell = m;

    /**** check barcode begin ****/
    initColors(); // init colors
    parseUnknownTable(); //prepare the data
    processLaneSummary(); //check for any unknow matches

    //compute isValid
    if (bMatchLargeUnknown || bLowerThanClusterMin || bLowerThanPercentMin || bUnknownBarcodeHigh || bLoweQScore) {
        bIsValid = false;
    }
    /*****  check barcode end ******/

    if (bIsValid) {
        setBgColor("#FlowcellSummary", color.ok);
    } else {
        setBgColor("#FlowcellSummary", color.error);
    }

    try {
        fs.writeFileSync(outFilePath, documentToSource(window.document));
    } catch (e) {
        console.error(e.message);
        process.exit(1);
    }

    if (!bIsValid) {
        //output json only when not valid
        var jsonFPath = getFilePath(outJsonPath);
        try {
            fs.writeFileSync(jsonFPath, JSON.stringify(dataJson, null, 4));
        } catch (e) {
            console.error(e.message);
            process.exit(1);
        }
    }

    process.exit(bIsValid ? 0 : 1);
});

function getFilePath(runPath) {
    filepattern = /plm_runError(\d+).json/i;
    var list = fs.readdirSync(runPath).filter(function(file) {
        return file.match(filepattern);
    });
    var maxnum = 1;
    list.forEach(function(file) {
        m = filepattern.exec(file);
        if (m) {
            var c = parseInt(m[1]) || 1;
            if (c >= maxnum) maxnum = c + 1;
        }
    });
    return path.join(runPath, "plm_runError" + maxnum + ".json");
}

function documentToSource(doc) {
    // The non-standard window.document.outerHTML also exists,
    // but currently does not preserve source code structure as well

    // The following two operations are non-standard
    //return doc.doctype.toString()+doc.innerHTML;
    msg = "\n<p><b>Processed and inspected by Fulgent checkLaneBarcodes V " + version + "</b><br>\n";
    msg += "<ul>\n";
    if (bMatchLargeUnknown) {
        msg += '<li style="color:red;">ALERT: Unknown barcode match found and count is larger than sample(s).</span></li>\n';
        console.log("ALERT: Unknown barcode match found and count is larger than sample(s).");
    }
    if (bLowerThanClusterMin) {
        msg += '<li style="color:red;">ALERT: Some sample(s) has raw cluster lower than minimun of ' + lsCheck.rawClustersMin + "</li>\n";
        console.log("ALERT: Some sample(s) has raw cluster lower than minimun of " + lsCheck.rawClustersMin);
    }
    if (bLowerThanPercentMin) {
        msg += '<li style="color:red;">ALERT: Some sample(s) has percentage lower than ' + lsCheck.pctLaneMin + "</li>\n";
        console.log("ALERT: Some sample(s) has percentage lower than " + lsCheck.pctLaneMin);
    }
    if (bLoweQScore) {
        msg += '<li style="color:red;">ALERT: Some sample(s) Q30 is less than 80%, and mean quality score is less than 34.00</li>\n';
        console.log("ALERT: Some sample(s) have Q30 < 80%, and mean quality score < 34.00");
    }
    if (bUnknownBarcodeHigh) {
        msg += '<li style="color:black;">CAUTION: Some unknown barcode(s) higher than ' + ukCheck.countMax + "</li>\n";
        console.log("CAUTION: Some unknown barcode(s) higher than " + ukCheck.countMax);
    }
    msg += "</ul>\n";

    msg += "<b>Validaiton Conditions:</b><br>\n<ul>";
    msg += "<li>Cluster > " + lsCheck.rawClustersMin + "</li>\n";
    msg += "<li>% lane > " + lsCheck.pctLaneMin + "</li>\n";
    msg += "<li>Unknown barcode count < " + ukCheck.countMax + " </li>\n";
    msg += "</ul></p>";

    doc.body.innerHTML += msg;
    return doc.doctype + "\n" + doc.documentElement.outerHTML;
}

function call_jsdom(source, callback) {
    var JQUERY_PATH = "." + path.sep + "node_modules" + path.sep + "jquery" + path.sep + "dist" + path.sep + "jquery.min.js";
    jsdom.env(source, [JQUERY_PATH], function(errors, window) {
        process.nextTick(function() {
            if (errors) {
                throw new Error("There were errors: " + JSON.stringify(errors, null, 2));
            }
            callback(window);
        });
    });
}

function parseUnknownTable() {
    var $ = gwindow.$;
    var rowIdx = 0;

    //resolve lane label (lane # may have gap)
    var laneLabel = [];
    $("#UnknownTable th[rowspan]").each(function() {
        laneLabel.push(parseInt($(this).text()) || 0);
    });

    $("#UnknownTable tr").each(function() {
        var laneIdx = 0;
        var prevText = "";
        var prevTd = null;
        $(this)
            .find("td")
            .each(function() {
                var text = $(this).text();
                if (text.match(/[ACGTN]{6,12}([-+][ACGTN]{6,12})?/) || text.match("unknown") || text.length < 1) { //bacode code <td> could be empty, unknown or combination of ACGTN
                    // match (ACGTN)
                    var c = parseInt(prevText.replace(/,/g, "")) || 0;
                    //---- for json out generation
                    var bc = text.split(/[-+]/);
                    var fbc = bc[0];
                    var ebc = bc.length > 1 ? bc[1] : "";
                    var uj = {
                        numberOfReads: c,
                        frontBarcode: fbc,
                        endBarcode: ebc,
                        status: "SUCCESS",
                        message: ""
                    };
                    //check if uk is greater than max allowed (and barcode not contain AAAA CCCC GGGG TTTT), report error
                    //also include barcode with N in it
                    if (c > ukCheck.countMax && !text.match(/AAAA|CCCC|GGGG|TTTT|NNNN/)) {
                        bUnknownBarcodeHigh = true;
                        setBgColor(prevTd, color.warning);
                        //add some note for find match issues
                        var uk = text.split(/[-+]/);
                        var msg = "";
                        msg = "f(" + uk[0] + ")Rev=" + reverse(uk[0]);
                        msg += ", Comp=" + complement(uk[0]);
                        msg += ", RevComp=" + complement(reverse(uk[0]));
                        if (uk.length > 1) {
                            msg += "f(" + uk[1] + ")Rev=" + reverse(uk[1]);
                            msg += ", Comp=" + complement(uk[1]);
                            msg += ", RevComp=" + complement(reverse(uk[1]));
                        }
                        $(prevTd).attr("title", msg);

                        // //for json out generation (detection handled by caller, comment out)
                        // uj.status = "ERROR";
                        // uj.message = "CAUTION: Some unknown barcode(s) higher than "+ ukCheck.countMax;
                        // uj.message += ". " + msg;
                    }

                    //---- for json out generation
                    if (text.match(/[ACGT]{6,12}([-+][ACGT]{6,12})?/) && !text.match(/N/)) {
                        //add only valid barcodes to array (ACGT)
                        var ui = {
                            lane: laneLabel[laneIdx],
                            row: rowIdx,
                            barcode: text,
                            element: this,
                            count: c,
                            jdata_cell: uj,
                            item_matchmsg: ""
                        };
                        UnknowArray.push(ui);
                    } else {
                        setBgColor(this, color.invalid_barcode); //Not a valid barcode in unknown
                    }
                    insertUnknown(laneLabel[laneIdx], uj); //for json out generation
                    laneIdx++;
                }
                prevText = text; //save for unknown barcode count.
                prevTd = this;
            });
        rowIdx++;
    });
}

function processLaneSummary() {
    var $ = gwindow.$;
    var row = 0;
    $("#LaneSummary tr").each(function() {
        var lane = "";
        var col = 0;
        var bValidSampleRow = false;
        var matchedUi = undefined;
        var sj = {
            id: undefined,
            numberOfReads: undefined,
            lanePercentage: undefined,
            frontBarcode: undefined,
            endBarcode: undefined,
            q30: undefined,
            status: "SUCCESS",
            message: ""
        };

        //Detect column variation and correction due to some Illumina lanebarcode report missing filteredClusters column
        $(this)
            .find("th")
            .each(function() {
                var text = $(this).text();
                if (col >= 8) {
                    if (text.match(/yield/i)) lSIdx.yield = col;
                    if (text.match(/pf/i)) lSIdx.pctPFClusters = col;
                    if (text.match(/q30/i)) lSIdx.pctQ30Bases = col;
                    if (text.match(/mean/i)) lSIdx.MeanQCScore = col;
                }
                col++;
            });

        //Process report data
        $(this)
            .find("td")
            .each(function() {
                var text = $(this).text();
                if (text.match(/[ACGT]{6,12}([-+][ACGT]{6,12})?/)) {
                    // barcode column, let's find possible match
                    bValidSampleRow = true; //indicate this row is a valid sample row
                    matchedUi = matchSample(lane, row, text, this);
                    if (matchedUi) {
                        //matched unknown, check count in the switch()
                        sj.message += matchedUi.item_matchmsg; // json out generation
                    }

                    //---- for json out generation
                    var bc = text.split(/[-+]/);
                    var fbc = bc[0];
                    var ebc = bc.length > 1 ? bc[1] : "";
                    sj.frontBarcode = fbc;
                    sj.endBarcode = ebc;
                } else {
                    if (col == lSIdx.barcode) {
                        //darken invalid barcode
                        setBgColor($(this), color.invalid_barcode); //not a valid barcode
                    }
                }
                switch (col) {
                    case lSIdx.sample:
                        sj.id = text; // json out generation
                        break;
                    case lSIdx.laneNum:
                        lane = text; //note the land #
                        insertAccession(lane, sj); //for json out generation
                        break;
                    case lSIdx.rawClusters:
                        text = text.replace(/,/g, "");
                        var c = parseInt(text) || 0;
                        sj.numberOfReads = c; // json out generation
                        if (matchedUi && matchedUi.count > c && !sj.id.includes("EXCLUDED")) {
                            bMatchLargeUnknown = true; // error
                            sj.status = "ERROR";
                            //sj.message += " Found unknown barcode match this sample."; (no need to add this msg as it was added in the matchSample scope)
                        }
                        if (bValidSampleRow && c < lsCheck.rawClustersMin && !sj.id.includes("EXCLUDED")) {
                            setBgColor(this, color.warning);
                            bLowerThanClusterMin = true; //error
                            // // json out generation (detection handled by caller, comment out)
                            // sj.status = "ERROR";
                            // sj.message += (" Number of reads is lower than "+ lsCheck.rawClustersMin + ".");
                        }
                        break;
                    case lSIdx.pctLane:
                        var c = parseFloat(text) || 0.0;
                        sj.lanePercentage = c; // json out generation
                        //console.error(text + "=" + c);
                        if (bValidSampleRow && c < lsCheck.pctLaneMin && !sj.id.includes("EXCLUDED")) {
                            setBgColor(this, color.warning);
                            bLowerThanPercentMin = true;

                            // // json out generation (detection handled by caller, comment out)
                            // sj.status = "ERROR";
                            // sj.message += (" Lane percentage is lower than " + lsCheck.pctLaneMin + ".");
                        }
                        break;
                    case lSIdx.pctQ30Bases:
                        var q30 = parseFloat(text) || 0.0;
                        sj.q30 = q30; // json out generation
                        break;
                    case lSIdx.MeanQCScore:
                        var meanQScore = parseFloat(text) || 0.0;
                        if (sj.q30 < 80 &&  meanQScore < 34 && !sj.id.includes("EXCLUDED")) {
                            setBgColor(this, color.warning);
                            bLoweQScore = true; // error
                            sj.status = "ERROR";
                            //sj.message += " Found unknown barcode match this sample."; (no need to add this msg as it was added in the matchSample scope)
                        }
                        break;
                }
                col++;
            });
        row++;
    });
}
function insertAccession(lane, sj) {
    ensureLaneJson(lane);
    dataJson.lanes[lane].accessions.push(sj);
}
function matchSample(lanetx, row, sample, item) {
    var $ = gwindow.$;
    var matchedUi = undefined;

    var lane = parseInt(lanetx);
    var b = sample.split(/[-+]/);
    $.each(UnknowArray, function(idx, ui) {
        if (lane == ui.lane) {
            // compare starts
            //console.error("check unknown lane=" + lane+ "ui.lane" + ui.lane);
            var ub = ui.barcode.split(/[-+]/);
            if (isMatchBarcode(b[0], ub[0]) && (b.length < 2 || isMatchBarcode(b[1], ub[1]))) {
                var color = getColor();
                var itemId = "S_" + lane + $(item).text(); //making html marker
                var uiId = "U_" + lane + $(ui.element).text(); //making html marker
                $(item).attr("id", itemId);
                setBgColor(item, color);
                var msg = "Found " + ui.barcode + " f:" + isMatchBarcode(b[0], ub[0]) + (b.length > 1 ? " e:" + isMatchBarcode(b[1], ub[1]) : "");
                $(item).attr("title", msg);
                $(item).html($(item).html() + " (<a href='#" + uiId + "' style='font-size:small'>" + $(ui.element).text() + "</a>)");
                $(ui.element).attr("id", uiId);
                setBgColor(ui.element, color);
                var uimsg = "Match " + sample + " on lane:" + lanetx + "row:" + row;
                $(ui.element).attr("title", uimsg);
                $(ui.element).html($(ui["element"]).html() + " (<a href='#" + itemId + "'>[go]</a>)");
                matchedUi = ui;

                //for json out generation
                ui.jdata_cell.message += uimsg;
                ui.item_matchmsg = msg;
                //console.error("lane="+lane+ " ui.lane="+ui.lane);
            }
        }
    });
    return matchedUi;
}

function setBgColor(item, color) {
    var $ = gwindow.$;
    $(item).css("background-color", color);
}

function isMatchBarcode(b1, b2) {
    if (b1 == b2) return "exact";
    if (reverse(b1) == b2) return "reverse";
    if (complement(b1) == b2) return "complement";
    if (complement(reverse(b1)) == b2) return "reverse-complement";
    return "";
}

function initColors() {
    var letters = "789ABCDEF".split("");
    while (bgcolors.length < 200) {
        do {
            var color = "#";
            for (var i = 0; i < 11; i++) {
                color += letters[Math.floor(Math.random() * letters.length)];
            }
        } while (bgcolors.indexOf(color) >= 0);
        bgcolors.push("#" + ("000000" + color.toString(16)).slice(-6));
    }
}

function getColor() {
    var c = bgcolors[bgclridx];
    bgclridx++;
    if (bgclridx >= bgcolors.length) bgclridx = 0;
    return c;
}

function reverse(s) {
    return s
        .split("")
        .reverse()
        .join("");
}

function complement(s) {
    var result = [];
    s.split("").forEach(function(c) {
        switch (c) {
            case "A":
                result.push("T");
                break;
            case "T":
                result.push("A");
                break;
            case "G":
                result.push("C");
                break;
            case "C":
                result.push("G");
                break;
        }
    });
    return result.join("");
}

function loadParameters(callback) {
    let unrecognizedArgument = false;
    if (!debug && process.argv.length < 4) return callback(false);

    inFilePath = process.argv[2];
    outFilePath = process.argv[3];

    if (inFilePath === "-h") return callback(false); //deal with some legacy parameter issue

    if (process.argv.length > 4) {
        let currentArgumentPosition = 4;
        for (currentArgumentPosition; currentArgumentPosition < process.argv.length; currentArgumentPosition++) {
            let argv = process.argv[currentArgumentPosition];
            if (
                ["-c", "-u", "-m", "-h", "-r"].find(value => {
                    return value === argv;
        })
        ) {
                currentArgumentPosition++;
                if (argv == "-c") lsCheck.rawClustersMin = parseInt(process.argv[currentArgumentPosition]);
                if (argv == "-u") ukCheck.countMax = parseInt(process.argv[currentArgumentPosition]);
                if (argv == "-m") lsCheck.pctLaneMin = parseInt(process.argv[currentArgumentPosition]);
                if (argv == "-r") outJsonPath = process.argv[currentArgumentPosition];
                if (argv == "-h") {
                    unrecognizedArgument = true;
                    break;
                }
                continue;
            }

            console.error("Unknown parameter: " + argv);
            unrecognizedArgument = true;
            break;
        }
    }

    return callback(!unrecognizedArgument);
}
