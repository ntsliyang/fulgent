import DataHTML.Colors;
import DataHTML.ColorsEntry;
import DataJson.Data;
import DataJson.FlowCellSummary.FlowCellSummary;
import DataJson.LaneJson.LaneInfos.Accession;
import DataJson.LaneJson.LaneInfos.Unknown;
import Parser.Stats;
import Parser.UnknownBarcodes.UnknownBarcodesEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class checkLaneBarcodesTest {

    private static checkLaneBarcodes c = new checkLaneBarcodes();
    private static Stats stats = null;
    private static Data data = null;
    private static FlowCellSummary flowCellSummary = null;
    private static Colors colors = null;
    private static Document doc = null;

    private static String outTestJsonPath = "temp/Stats_test_output.json";
    private static String expectedOutputHTMLPath = "";
    private static String expectedOutputJsonPath = "";

    private static int colorCounts = 0;
    private static int messageCounts = 0;

    private static Map<String, String> colorItems = new HashMap<String, String>() {
        {
            put("rgb(170,255,170)", "ok"); // green
            put("rgb(255,170,170)", "error"); // red
            put("rgb(255,255,170)", "warning"); // yellow
            put("rgb(119,119,119)", "invalid barcode"); // grey
        }
    };

    @Test
    void matchBarcodeTest() {
        assertEquals("reverse", c.isMatchBarcode("ACGTCCGTA", "ATGCCTGCA"));
        assertEquals("complement", c.isMatchBarcode("ACTG", "TGAC"));
        assertEquals("reverse-complement", c.isMatchBarcode("ACGTCCGTA", "TACGGACGT"));
        assertEquals("", c.isMatchBarcode("CAGCGA", ""));
    }

    @Test
    void parseTest() throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        FileReader reader = new FileReader(new File(c.getInFilePath()));
        stats = gson.fromJson(reader, Stats.class);

        PrintWriter pw = new PrintWriter(outTestJsonPath);
        pw.println(gson.toJson(stats));
        pw.close();

        JSONObject j1 = (JSONObject) new JSONParser().parse(new FileReader(c.getInFilePath()));
        JSONObject j2 = (JSONObject) new JSONParser().parse(new FileReader(outTestJsonPath));
        final String expected = j1.toJSONString();
        final String output = j2.toJSONString();
        assertEquals(expected, output);
    }

    @Test
    void dataJsonOutputTest() throws Exception {
        String[] args = null;
        c.main(args);
        data = c.getData();

        // read in expect datajson file
        expectedOutputJsonPath = System.getProperty("expectedOutputJsonPath");
        JSONObject jo = (JSONObject) new JSONParser().parse(new FileReader(expectedOutputJsonPath));

        // System.out.println((data == null) + " " + (jo == null));
        // System.out.println();
        // compare fields
        assertEquals(jo.get("flowcell"), data.getFlowcell());
        assertEquals(jo.get("node"), data.getNode());

        // iterating through lanes to compare every field
        JSONObject LaneJson = (JSONObject) jo.get("lanes");

        for (int i = 0; i < LaneJson.size(); i++) {
            JSONObject Lists = (JSONObject) LaneJson.get(String.valueOf(i + 1));

            DataJson.LaneJson.LaneJson laneJson = data.getLanes().get(String.valueOf(i + 1));
            for (int j = 0; j < Lists.size(); j++) {
                JSONArray accessions = (JSONArray) Lists.get("accessions");
                JSONArray unknowns = (JSONArray) Lists.get("unknown");

                List<Accession> accessionList = laneJson.getAccessions();
                List<Unknown> unknownList = laneJson.getUnknown();

                // accessions
                for (int k = 0; k < accessions.size() - 1; k++) {
                    JSONObject expect = (JSONObject) accessions.get(k);
                    Accession output = accessionList.get(k);

                    assertEquals(expect.get("id"), output.getId());
                    assertEquals(expect.get("numberOfReads"), output.getNumberOfReads());

                    /* check for possible two data types for 'lanepct' */
                    if (expect.get("lanePercentage") instanceof java.lang.Long) {
                        long lanepct = (long) expect.get("lanePercentage");
                        assertEquals(lanepct, (long) output.getLanePercentage());
                    }
                    else {
                        double lanepct = (double) expect.get("lanePercentage");
                        assertEquals(Math.round(lanepct * 10) / 10.0, Math.round(output.getLanePercentage() * 10) / 10.0);
                    }

                    /* front and end barcode: differentiate between determined and undetermined cases */
                    if (expect.get("id").equals("Undetermined")) {
                        assertEquals(null, expect.get("frontBarcode"));
                        assertEquals(null, expect.get("endBarcode"));
                        assertEquals("", output.getFrontBarcode());
                        assertEquals("", output.getEndBarcode());
                    } else {
                        assertEquals(expect.get("frontBarcode"), output.getFrontBarcode());
                        assertEquals(expect.get("endBarcode"), output.getEndBarcode());
                    }

                    /* check for possible two data types for 'q30' */
                    if (expect.get("q30") instanceof java.lang.Long) {
                        long q30 = (long) expect.get("q30");
                        assertEquals(q30, (long) output.getQ30());
                    }
                    else {
                        double q30 = (double)expect.get("q30");
                        assertEquals(Math.round(q30 * 10) / 10.0, Math.round(output.getQ30() * 10) / 10.0);
                    }

                    // System.out.println(output.getId());
                    assertEquals(expect.get("status"), output.getStatus());
                   // assertEquals(expect.get("message"), output.getMessage());
                }

                // unknown
                for (int k = 0; k < unknowns.size(); k++) {
                    JSONObject expect = (JSONObject) unknowns.get(k);
                    Unknown output = unknownList.get(k);

                    assertEquals(expect.get("numberOfReads"), output.getNumberOfReads());
                    assertEquals(expect.get("frontBarcode"), output.getFrontBarcode());
                    assertEquals(expect.get("endBarcode"), output.getEndBarcode());
                    assertEquals(expect.get("status"), output.getStatus());
                    // assertEquals(expect.get("message"), output.getMessage());
                }
            }
        }
    }

    @Test
    void matchFlowcellSummary() throws Exception {
        expectedOutputHTMLPath = System.getProperty("expectedOutputHTMLPath");
        File input = new File(expectedOutputHTMLPath);
        doc = Jsoup.parse(input, "UTF-8");

        // check flowcell information
        Elements flowcellTable = doc.select("table#FlowcellSummary");
        String TotalClustersRawComma = flowcellTable.select("tr").get(1).select("td").get(0).text().replaceAll(",","");
        Long TotalClustersRaw = Long.valueOf(TotalClustersRawComma); // clusters (raw)
        String TotalClustersPFComma = flowcellTable.select("tr").get(1).select("td").get(1).text().replaceAll(",","");
        Long TotalClustersPF = Long.valueOf(TotalClustersPFComma); // clusters (PF)
        String YieldComma = flowcellTable.select("tr").get(1).select("td").get(2).text().replaceAll(",","");
        Long Yield = Long.valueOf(YieldComma); // Yield (MBases)
        String color = flowcellTable.attr("style").split(":")[1].replaceAll("[\\s+;]", ""); // background color of table

        flowCellSummary = c.getFlowCellSummary();
        assertEquals(TotalClustersRaw, flowCellSummary.getTotalClustersRaw());
        assertEquals(TotalClustersPF, flowCellSummary.getTotalClustersPF());
        assertEquals(Yield, flowCellSummary.getYield());
        assertEquals(colorItems.get(color), flowCellSummary.getColor());
    }

    @Test
    void matchColorTest() {
        // check colors (only for warning and invalid barcode cases, not for matching)
        Elements laneTable = doc.select("table#LaneSummary");
        Elements rows = laneTable.select("tr");

        colors = c.getColors();
        List<ColorsEntry> LaneSummaryColors = colors.getLaneSummaryColors();

        // for lane summary
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            // only the following selected fields are possible to have different colors other than white,
            // so that only these following fields will be tested.

            // extract the color info from the given lane number and sample id
            int lane = Integer.valueOf(cols.get(0).text()); // lane number
            Map<String, String> colorsMap = LaneSummaryColors.get(lane - 1).getColorsMap();
            String color = colorsMap.get(cols.get(2).text());

            //System.out.println();
            //System.out.print(lane + " " + cols.get(2).text());
            matchAttributeColor(color, cols.get(3).attr("style"), "barcode sequence"); // barcode sequence
            //System.out.println();
            //System.out.print(lane + " " + cols.get(2).text());
            matchAttributeColor(color, cols.get(4).attr("style"), "pf clusters"); // pf clusters
            //System.out.println();
            //System.out.print(lane + " " + cols.get(2).text());
            matchAttributeColor(color, cols.get(5).attr("style"), "% of the lane"); // % of the lane
            //System.out.println();
            //System.out.print(lane + " " + cols.get(2).text());
            matchAttributeColor(color, cols.get(cols.size() - 1).attr("style"), "mean quality score"); // mean q score
        }

        // for unknown table
        Elements unknownTable = doc.select("table#UnknownTable");
        Elements unknownRows = unknownTable.select("tr");
        // for 'tr', unknownTable follows the order (Lane -> Count -> Sequence -> ...)

        List<ColorsEntry> UnknownColors = colors.getUnknownColors();

        // rows determine which lane number
        for (int i = 0; i < unknownRows.get(1).select("th").size() * 2; i++) {

            // order would be count->sequence->count->sequence->....
            // the outer loop is processing through columns
            if(i >= unknownRows.get(1).select("th").size() * 2) break;
            int lane = Integer.valueOf(unknownRows.get(1).select("th").get((int) Math.floor(i/2)).text());
            Map<String, String> colorsMap = UnknownColors.get(lane - 1).getColorsMap();
            for (int j = 1; j < unknownRows.size(); j++) {
                // order would be barcode sequences of decreasing magnitude for the same lane number
                // the inner loop is processing through rows

                // if the current row is only "<tr></tr>", then we skip this one
                if (unknownRows.get(j).select("tr").text().trim().equals("")) continue;

                // given the jth row and ith column, we can fetch count and barcode
                long count = Long.valueOf(unknownRows.get(j).select("td").get(i).text().replaceAll(",",""));
                String barcode = unknownRows.get(j).select("td").get(i+1).text();

                // given the barcode and lane, we can fetch colorInfo from colorsMap
                String color = colorsMap.get(barcode.replace("([go])", "").trim());
                //System.out.println();
                //System.out.print(count + " " + barcode);
                matchAttributeColor(color, unknownRows.get(j).select("td").get(i).attr("style"), "count"); // count
                //System.out.println();
                //System.out.print(count + " " + barcode);
                matchAttributeColor(color, unknownRows.get(j).select("td").get(i+1).attr("style"), "sequence"); // barcode sequence
            }
            i+=1;

        }
    }

    @Test
    void matchMessageTest() {
        // check cases of matching between barcodes in lane summary and unknown table

        Elements laneTable = doc.select("table#LaneSummary");
        Elements rows = laneTable.select("tr");

        // 1. (in html) correspondence between 'background-color' and 'barcode' attributes
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");
            if (!cols.get(3).attr("style").equals("") && !cols.get(3).attr("id").equals("")) {
                // the barcode matching to one of the unknowns has color and id
                String encodedId = cols.get(3).select("a").attr("href");

                Element unknownCounterpart = doc.getElementById(encodedId.replace("#",""));

                // make sure the color is the same
                String color = cols.get(3).attr("style").split(":")[1].replaceAll("[\\s+;]", "");
                String unknownCounterpartColor = unknownCounterpart.attr("style").split(":")[1].replaceAll("[\\s+;]", "");
                assertEquals(color, unknownCounterpartColor);

                // make sure the transformed barcode in lane summary is equivalent to unknownCounterpartBarcode in unknown table
                String bothBarcodes = cols.get(3).text();
                String originalBarcode = bothBarcodes.substring(0, bothBarcodes.indexOf("(")).trim();
                String transformedBarcode = bothBarcodes.substring(bothBarcodes.indexOf("(") + 1, bothBarcodes.indexOf(")"));
                String unknownCounterpartBarcode = unknownCounterpart.text().replace("([go])", "").trim();
                assertEquals(transformedBarcode, unknownCounterpartBarcode);

                // make sure the original barcode in lane summary is the same as truncated id in unknown table
                assertEquals(originalBarcode, unknownCounterpart.select("a").attr("href").replace("#S_" + cols.get(0).text(), ""));

                // make sure the truncated id of transformed barcode in lane summary is the same as unknownCounterpartBarcode
                assertEquals(unknownCounterpartBarcode, cols.get(3).select("a").attr("href").replace("#U_" + cols.get(0).text(), ""));
            }
        }

        // 2. (in json) by the same lane number and sample_id, we are expect to find
        // the relation (from f:{};e:{}) to check it is the case
        for (int i = 0; i < data.getLanes().size(); i++) {
            for (int j = 0; j < data.getLanes().get(String.valueOf(i + 1)).getAccessions().size(); j++) {
                String msg = data.getLanes().get(String.valueOf(i + 1)).getAccessions().get(j).getMessage();
                if (!msg.equals("")) {
                    // verify that the relation matches in json file
                    String barcode = msg.substring(0, msg.indexOf("[")).split(" ")[1];
                    String[] bc = barcode.split("[-+]");
                    String fbc = bc[0];
                    String ebc = bc.length > 1 ? bc[1] : "";
                    String[] match = msg.substring(msg.indexOf("[") + 1, msg.indexOf("]")).split(";");
                    String fmatch = match[0].split(":")[1];
                    String ematch = match[1].split(":")[1];

                    String unknownBarcode = searchMatchBarcodeinUnknown(data.getLanes().get(String.valueOf(i + 1)).getUnknown(), data.getLanes().get(String.valueOf(i + 1)).getAccessions().get(j).getId());
                    String[] unknownbc = unknownBarcode.split("[-+]");
                    String unknownfbc = unknownbc[0];
                    String unknownebc = unknownbc.length > 1 ? unknownbc[1] : "";
                    assertEquals(fmatch, c.isMatchBarcode(fbc, unknownfbc));
                    assertEquals(ematch, c.isMatchBarcode(ebc, unknownebc));

                    // verify that corresponding id corresponds to the correct barcodes
                    Element original = doc.getElementById("S_" + (i + 1) + unknownBarcode); // original cell id
                    Element unknownCounterpart = doc.getElementById("U_" + (i + 1) + barcode); // unknown counterpart cell id
                    if (original != null && unknownCounterpart != null) {
                        // original and unknownCounterpart are not null only if there is a match
                        String originalBarcode = original.text().substring(0, original.text().indexOf("(")).trim();
                        String transformedBarcode = original.text().substring(original.text().indexOf("(") + 1, original.text().indexOf(")"));
                        assertEquals(originalBarcode, unknownBarcode);
                        String front = data.getLanes().get(String.valueOf(i + 1)).getAccessions().get(j).getFrontBarcode();
                        String end = data.getLanes().get(String.valueOf(i + 1)).getAccessions().get(j).getEndBarcode();
                        assertEquals(originalBarcode, front + "+" + end);
                        assertEquals(unknownCounterpart.text().replace("([go])", "").trim(), barcode);

                        // verify that the relation outlined in json also applies in html
                        String[] originalbc = originalBarcode.replace("([go])", "").split("[-+]");
                        String originalfbc = originalbc[0];
                        String originalebc = originalbc.length > 1 ? originalbc[1] : "";
                        String[] transformedbc = transformedBarcode.split("[-+]");
                        String transformedfbc = transformedbc[0];
                        String transformedebc = transformedbc.length > 1 ? transformedbc[1] : "";
                        assertEquals(transformedfbc, matchedBarcode(originalfbc, fmatch));
                        assertEquals(transformedebc, matchedBarcode(originalebc, ematch));
                    }

                }

            }
        }
    }

    @Test
    void verifyAllColorsChecked() {
        // make sure all cases of warning and invalid barcode have been checked

        // count color in html
        Elements laneTable = doc.select("table#LaneSummary");
        Elements rows = laneTable.select("tr");

        int htmlColorCounts = 0;
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            if (verifyAttributesColor(cols.get(3).attr("style"))) htmlColorCounts += 1;
            if (verifyAttributesColor(cols.get(4).attr("style"))) htmlColorCounts += 1;
            if (verifyAttributesColor(cols.get(5).attr("style"))) htmlColorCounts += 1;
            if (verifyAttributesColor(cols.get(cols.size() - 1).attr("style"))) htmlColorCounts += 1;
        }

        Elements unknownTable = doc.select("table#UnknownTable");
        Elements unknownRows = unknownTable.select("tr");
        // for 'tr', unknownTable follows the order (Lane -> Count -> Sequence -> ...)

        // rows determine which lane number
        for (int i = 0; i < unknownRows.get(1).select("th").size() * 2; i++) {

            for (int j = 1; j < unknownRows.size(); j++) {
                if (unknownRows.get(j).select("tr").text().trim().equals("")) continue;
                if (verifyAttributesColor(unknownRows.get(j).select("td").get(i).attr("style"))) htmlColorCounts += 1;
                if (verifyAttributesColor(unknownRows.get(j).select("td").get(i+1).attr("style"))) htmlColorCounts += 1;
            }
            i+=1;

        }

        assertEquals(htmlColorCounts, colorCounts);

        // count color in json
        int jsonColorCounts = 0;

        List<ColorsEntry> laneSummaryColors = colors.getLaneSummaryColors();
        List<ColorsEntry> unknownColors = colors.getUnknownColors();

        jsonColorCounts += calculateTotalColorsinJson(laneSummaryColors, false);
        jsonColorCounts += calculateTotalColorsinJson(unknownColors, true);
        assertEquals(jsonColorCounts, colorCounts);
    }

    @Test
    void verifyAllMessagesChecked() {
        // make sure all cases of matching have been checked

        // in html
        Elements laneTable = doc.select("table#LaneSummary");
        Elements rows = laneTable.select("tr");

        int htmlLaneSummaryMessageCounts = 0;
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");
            if (!cols.get(3).attr("style").equals("") && !cols.get(3).attr("id").equals("")) {
                // the barcode matching to one of the unknowns has color and id
                htmlLaneSummaryMessageCounts += 1;
            }
        }

        Elements unknownTable = doc.select("table#UnknownTable");
        Elements unknownRows = unknownTable.select("tr");

        int htmlUnknownMessageCounts = 0;
        for (int i = 0; i < unknownRows.get(1).select("th").size() * 2; i++) {

            for (int j = 1; j < unknownRows.size(); j++) {
                if (unknownRows.get(j).select("tr").text().trim().equals("")) continue;
                String attributeString = unknownRows.get(j).select("td").get(i+1).attr("style");
                if (!attributeString.isEmpty()) {
                    String[] attributes = attributeString.split(";");
                    for (int k = 0; k < attributes.length; k++) {
                        if (attributes[k].split(":")[0].contains("background-color")) {
                            if (!colorItems.containsKey(attributes[k].split(":")[1].replaceAll("[\\s+;]", ""))) htmlUnknownMessageCounts += 1;
                        }
                    }
                }
            }
            i+=1;

        }

        assertEquals(htmlLaneSummaryMessageCounts, htmlUnknownMessageCounts);

        // in json
        int jsonLaneSummaryMessageCounts = 0;
        int jsonUnknownMessageCounts = 0;

        for (int i = 0; i < data.getLanes().size(); i++) {
            List<Accession> accessions = data.getLanes().get(String.valueOf(i + 1)).getAccessions();
            List<Unknown> unknowns = data.getLanes().get(String.valueOf(i + 1)).getUnknown();

            for (int j = 0; j < accessions.size(); j++) {
                if (!accessions.get(j).getMessage().isEmpty()) jsonLaneSummaryMessageCounts += 1;
            }

            for (int j = 0; j < unknowns.size(); j++) {
                if (!unknowns.get(j).getMessage().isEmpty()) jsonUnknownMessageCounts += 1;
            }
        }

        assertEquals(jsonLaneSummaryMessageCounts, jsonUnknownMessageCounts);
        assertEquals(jsonLaneSummaryMessageCounts, htmlLaneSummaryMessageCounts);
        assertEquals(jsonUnknownMessageCounts, htmlUnknownMessageCounts);
    }

    @Test
    void matchUnknownEntriesSize() {
        List<UnknownBarcodesEntry> unknownBarcodesEntries = stats.getUnknownBarcodes();

        for (int i = 0; i < unknownBarcodesEntries.size(); i++) {
            int s1 = unknownBarcodesEntries.get(i).getBarcodes().size();
            int s2 = data.getLanes().get(String.valueOf(i + 1)).getUnknown().size();
            assertEquals(s1, s2);
        }
    }

    public int calculateTotalColorsinJson(List<ColorsEntry> colorsEntries, boolean isUnknown) {
        // as we compare the results with those in html, we only extract the top ten for unknown

        int cnt = 0;
        for (int i = 0; i < colorsEntries.size(); i++) {
            int counter = 0;
            for (Map.Entry entry : colorsEntries.get(i).getColorsMap().entrySet()) {
                if (counter < 10 || !isUnknown) {
                    counter++;
                    String c = (String) entry.getValue();
                    if (!c.equals("white")) {
                        //System.out.println(colorsEntries.get(i).getLane() + " " + entry.getKey() + " " + c + "......" + c.split(";").length);
                        cnt += c.split(";").length;
                    }
                } else break;
            }
        }
        return cnt;
    }

    public boolean verifyAttributesColor(String attributeString) {
        if (attributeString.equals("")) return false;
        String[] attributes = attributeString.split(";");
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].split(":")[0].contains("background-color")) {
                String c = attributes[i].split(":")[1].replaceAll("[\\s+;]", "");
                return colorItems.containsKey(c) && (colorItems.get(c).equals("warning") || colorItems.get(c).equals("invalid barcode"));
            }
        }
        return false;
    }

    public String matchedBarcode(String s, String relation) {
        if (relation.equals("exact")) return s;
        else if (relation.equals("reverse")) return c.reverse(s);
        else if (relation.equals("complement")) return c.complement(s);
        else if (relation.equals("reverse-complement")) return c.complement(c.reverse(s));
        return "";
    }

    public String searchMatchBarcodeinUnknown(List<Unknown> unknowns, String sample) {
        for (int i = 0; i < unknowns.size(); i++) {
            String msg = unknowns.get(i).getMessage();
            if (!msg.equals("")) {
                String[] infos = msg.substring(msg.indexOf("[") + 1, msg.indexOf("]")).split(";");
                String unknownSample = infos[1].split(":")[1];
                if (sample.equals(unknownSample)) {
                    return msg.substring(0, msg.indexOf("[")).split(" ")[1];
                }
            }
        }
        return "";
    }

    public void matchAttributeColor(String colorInfo, String attributeInfo, String attribute) {
        if (colorInfo.equals("white")) {
            // else if processed attribute info is an empty string,
            // background color must be white and hence c is supposed to be "white"
            assertEquals("", attributeInfo);
            return;
        }

        // example: "pf clusters:warning;% of the lane:warning;"
        String[] colorsEntry = colorInfo.split(";");
        for (int i = 0; i < colorsEntry.length; i++) {

            // make sure that the same attributes are being compared
            if (colorsEntry[i].split(":")[0].equals(attribute)) {
                // System.out.println("colorsInfo: " + colorInfo + " attributeInfo: " + attributeInfo );
                // String c is either 'warning' or 'invalid barcode'
                String c = colorsEntry[i].split(":")[1];
                if (!processAttributeInfo(attributeInfo).equals("")) {
                    // if processed attribute info is not an empty string,
                    // use colorItems to map to meanings of RGB Info
                    assertEquals(colorItems.get(processAttributeInfo(attributeInfo)), c);
                    //System.out.println("......it is checked!");
                    colorCounts += 1;
                }
                // return;
            }
        }
    }

    public String processAttributeInfo(String attributeInfo) {
        // if attributeInfo does not have 'style' attribute, the string would return empty
        if (attributeInfo.equals("")) return "";
        // else we extract the RGB info from 'background-color'
        else {
            String[] styleAttributes = attributeInfo.split(";");
            for (int i = 0; i < styleAttributes.length; i++) {
                if (styleAttributes[i].contains("background-color")) {
                    return styleAttributes[i].split(":")[1].replaceAll("[\\s+;]", "");
                }
            }
        }
        return "";
    }

//    public String searchColorById(String lane, String id) {
//        // assumes accessions
//        Accessions accessions = data.getLanes().get(lane).getAccessions();
//        if (!id.equals("Undetermined")) {
//            List<Accession> determinedAccessions = accessions.getDeterminedAccessions();
//            for (int i = 0; i < determinedAccessions.size(); i++) {
//                if (determinedAccessions.get(i).getId().equals(id)) return determinedAccessions.get(i).getColor();
//            }
//        } else {
//            UndeterminedAccession undeterminedAccession = accessions.getUndeterminedAccession();
//            if (undeterminedAccession.getId().equals(id)) return undeterminedAccession.getColor();
//        }
//        return "";
//    }

//    public String searchColorByBarcode(String lane, String frontBarcode, String endBarcode) {
//        // assumes unknowns
//        List<Unknown> unknowns = data.getLanes().get(lane).getUnknown();
//        for (int i = 0; i < unknowns.size(); i++) {
//            if (unknowns.get(i).getFrontBarcode().equals(frontBarcode) &&
//            unknowns.get(i).getEndBarcode().equals(endBarcode))
//                return unknowns.get(i).getColor();
//        }
//        return "";
//    }
}
