import DataHTML.Colors;
import DataHTML.ColorsEntry;
import DataJson.MultiData;
import Parser.LaneNumberReadInfos.LaneNumberReadInfos;
import StatsHTML.LaneSummaryEntry;
import DataJson.Data;
import DataJson.FlowCellSummary.FlowCellSummary;
import DataJson.LaneJson.LaneInfos.Accession;
import DataJson.LaneJson.LaneJson;
import DataJson.LaneJson.LaneInfos.Unknown;
import Parser.*;
import Parser.ConversionResults.ConversionResultsEntry;
import Parser.ConversionResults.DemuxResults.DemuxResultsEntry;
import Parser.ConversionResults.IndexMetrics;
import Parser.ConversionResults.Undetermined;
import Parser.UnknownBarcodes.UnknownBarcodesEntry;
import UnknownArray.UnknownArrayEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.*;
import java.lang.Math;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.NaN;

public class checkLaneBarcodes {

    private static boolean debug = false;
    private static boolean isDirectory = false;
    private static boolean bIsValid = true;
    private static boolean bMatchLargeUnknown = false;
    private static boolean bLowerThanClusterMin = false;
    private static boolean bLowerThanPercentMin = false;
    private static boolean bLowerQScore = false;
    private static boolean bUnknownBarcodeHigh = false;

    private static String laneSummaryColor = "";

    private static int topLimit = 10;
    private static long ukCheckCountMax = 1_000_000;
    private static long lsCheckRawClustersMin = 1_000_000;
    private static double lsCheckpctLaneMin = 0.1;

    private static long TotalPFClustersRaw = 0;
    private static long TotalPFClusters = 0;
    private static long TotalYield = 0;

    private static List<UnknownArrayEntry> UnknownArray = new ArrayList<>();
    private static String version = "1.1.2";
    private static String inFilePath = "";
    //  private static String outFilePath = "html/html_result1";
    // private static String outFilePath = "html/html_result2.html";
    private static String outJsonPath = "";
    private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static MultiStats multiStats = null;
    private static Stats stats = null;
    private static MultiData multiData = null;
    private static Data data = null;
    private static FlowCellSummary flowCellSummary = null;
    private static Colors colors = null;
    private static Map<String, LaneJson> lanes = new LinkedHashMap<>();
    // private static List<UnknownBarcodesEntry> TopUnknownTableEntries = new ArrayList<>();

    public static String getInFilePath() {
        return inFilePath;
    }

    public static String getOutJsonPath() {
        return outJsonPath;
    }

    public static Stats getStats() {
        return stats;
    }

    public static Data getData() {
        return data;
    }

    public static FlowCellSummary getFlowCellSummary() {
        return flowCellSummary;
    }

    public static Colors getColors() {
        return colors;
    }

    public static boolean match(String text, String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    public static String reverse(String s) {
        int len = s.length();
        char[] sChars = s.toCharArray();
        for (int i = 0; i < Math.floor(s.length() / 2); i++) {
            // swap
            char temp = sChars[i];
            sChars[i] = sChars[len - 1 - i];
            sChars[len - 1 - i] = temp;
        }
        return String.valueOf(sChars);
    }

    public static String complement(String s) {
        StringBuilder str = new StringBuilder(s);
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == 'A') str.setCharAt(i, 'T');
            else if (s.charAt(i) == 'T') str.setCharAt(i, 'A');
            else if (s.charAt(i) == 'G') str.setCharAt(i, 'C');
            else if (s.charAt(i) == 'C') str.setCharAt(i, 'G');
        }
        return str.toString();
    }

    public static String isMatchBarcode(String b1, String b2) {
        if (b1.equals(b2)) return "exact";
        if (reverse(b1).equals(b2)) return "reverse";
        if (complement(b1).equals(b2)) return "complement";
        if (complement(reverse(b1)).equals(b2)) return "reverse-complement";
        return "";
    }

    public static UnknownArrayEntry matchSample(int lane, String Sample, String barcode, long PFClusters) {
        String[] bc = barcode.split("[-+]");
        UnknownArrayEntry matchedUi = null;

        for (int i = 0; i < UnknownArray.size(); i++) {
            UnknownArrayEntry ui = UnknownArray.get(i);
            if (lane == ui.getLane()) {
                // compare
                String [] ubc = ui.getBarcode().split("[-+]");
                if (!isMatchBarcode(bc[0], ubc[0]).isEmpty() && (bc.length < 2 || !isMatchBarcode(bc[1], ubc[1]).isEmpty()) && ui.getCount() >= PFClusters) {
                    // unknown table is considered as a "database" that contain the counts more than that of any individual sample
                    String msg = "Found " + ui.getBarcode() + "[f:" + isMatchBarcode(bc[0], ubc[0]) + (bc.length > 1 ? ";e:" + isMatchBarcode(bc[1], ubc[1]) : "") + "]";
                    String uimsg = "Match " + barcode + "[lane:" + lane + ";sample id:" + Sample + "]";

                    matchedUi = ui;

                    // json output message generation
                    String jdata_cell_msg = ui.getJdata_cell().getMessage();
                    String item_matching = ui.getItem_matching();
                    ui.getJdata_cell().setMessage(jdata_cell_msg + uimsg);
                    ui.setItem_matching(item_matching + msg);
                }
            }
        }
        return matchedUi;
    }

    public static Accession processLaneSummary(int LaneNumber, String Sample, String BarcodeSequence, long PFClusters, double pctLane, double pctQ30Bases, double MeanQScore) {
        /* process the lane summary data */
        boolean bValidSampleRow = false;
        UnknownArrayEntry matchedUi = null;
        String message = "";
        String status = "SUCCESS";
        String fbc = "";
        String ebc = "";
        laneSummaryColor = "white";

        /* process barcode sequence  */
        //if (BarcodeSequence.matches("[ACGT]{6,12}([-+][ACGT]{6,12})?")) {
        if (match(BarcodeSequence, "[ACGT]{6,12}([-+][ACGT]{6,12})?")) {
            bValidSampleRow = true;
            matchedUi = matchSample(LaneNumber, Sample, BarcodeSequence, PFClusters);

            if (matchedUi != null) {
                message += matchedUi.getItem_matching();
            }

            String[] bc = BarcodeSequence.split("[-+]");
            fbc = bc[0];
            ebc = bc.length > 1 ? bc[1] : "";

        } else {
            // set color
            if (BarcodeSequence.equals("unknown")) {
                laneSummaryColor = "barcode sequence:invalid barcode;";
            }
        }

        /* process pf clusters (count) */
        if (matchedUi != null && matchedUi.getCount() > PFClusters && !Sample.contains("EXCLUDED") && !Sample.equals("Undetermined")) {
            // only for determined samples
            bMatchLargeUnknown = true; // error
            status = "ERROR";
        }

        /* process pf clusters (min) */
        if (bValidSampleRow && PFClusters < lsCheckRawClustersMin && !Sample.contains("EXCLUDED") && !Sample.equals("Undetermined")) {
            // only for determined samples
            laneSummaryColor = "pf clusters:warning;";
            bLowerThanClusterMin = true; // error
        }

        if (bValidSampleRow && pctLane < lsCheckpctLaneMin && !Sample.contains("EXCLUDED") && !Sample.equals("Undetermined")) {
            // only for determined samples
            if (laneSummaryColor.contains("white")) laneSummaryColor = "% of the lane:warning;"; // set this very cell to warning color
            else laneSummaryColor += "% of the lane:warning;";
            bLowerThanPercentMin = true; // error
        }

        if (pctQ30Bases < 80 && MeanQScore < 34 && !Sample.contains("EXCLUDED") && !Sample.equals("Undetermined")) {
            if (laneSummaryColor.contains("white")) laneSummaryColor = "mean quality score:warning;";
            else laneSummaryColor += "mean quality score:warning;";
            bLowerQScore = true;
            status = "ERROR";
        }

        return new Accession(Sample, PFClusters, pctLane, fbc, ebc, pctQ30Bases, status, message);
    }

    public static void processUnknownTable() {
        List<UnknownBarcodesEntry> unknownBarcodesEntries = (isDirectory) ? multiStats.getUnknownBarcodes() : stats.getUnknownBarcodes();
        List<ColorsEntry> UnknownColors = new LinkedList<>();
        List<ColorsEntry> LaneSummaryColors = null;
        colors = new Colors(LaneSummaryColors, UnknownColors);

        for (int i = 0; i < unknownBarcodesEntries.size(); i++) {
            List<Unknown> unknowns = new ArrayList<>();
            Map<String, String> colorsMap = new LinkedHashMap<>();

            List<Accession> accessions = null;
            LaneJson laneJson = new LaneJson(accessions, unknowns);

            int lane = unknownBarcodesEntries.get(i).getLane();
            Map<String, Long> barcodes = unknownBarcodesEntries.get(i).getBarcodes();

            for (Map.Entry<String, Long> entry : barcodes.entrySet()) {
                String barcode = entry.getKey();
                long c = entry.getValue();
                colorsMap.put(barcode, "");

                Unknown uj = null;
                // if (barcode.matches("[ACGTN]{6,12}([-+][ACGTN]{6,12})?") || barcode.matches("unknown") || barcode.length() < 1) {
                if (match(barcode, "[ACGTN]{6,12}([-+][ACGTN]{6,12})?") || match(barcode, "unknown") || barcode.length() < 1) {
                    // if barcode is empty, or unknown, or a combination of ACGTN, we create an DataJson.LaneJson.LaneJson.Unknown object and ready to insert into data
                    String[] bc = barcode.split("[-+]");
                    String fbc = bc[0];
                    String ebc = bc.length > 1 ? bc[1] : "";
                    // uj = new Unknown(c, fbc, ebc, "SUCCESS", "", "white");
                    uj = new Unknown(c, fbc, ebc, "SUCCESS", "");
                    colorsMap.replace(barcode, "white");
                    unknowns.add(uj);
                }

                // if (c > ukCheckCountMax && !barcode.matches("/AAAA|CCCC|GGGG|TTTT|NNNN/")) {
                if (c > ukCheckCountMax && !match(barcode, "/AAAA|CCCC|GGGG|TTTT|NNNN/")) {
                    bUnknownBarcodeHigh = true;
                    //if (uj != null) uj.setColor("Count: warning\n");
                    colorsMap.replace(barcode, "count:warning;");
                }

                // if (barcode.matches("[ACGT]{6,12}([-+][ACGT]{6,12})?") && !barcode.matches("N")) {
                if (match(barcode, "[ACGT]{6,12}([-+][ACGT]{6,12})?") && !match(barcode, "N")) {
                    // create UnknownArray for valid barcodes (ACGT)
                    UnknownArrayEntry uae = new UnknownArrayEntry(lane, barcode, c, uj, "");
                    UnknownArray.add(uae);
                } else {
//                    set this very cell to invalidBarcodeColor
                    String originalColor = colorsMap.get(barcode);
                    if (originalColor.equals("white")) {
                        colorsMap.replace(barcode, "sequence:invalid barcode;");
                    } else if (originalColor.equals("count:warning;")) {
                        colorsMap.replace(barcode, originalColor + "sequence:invalid barcode;");
                    }
                }
            }

            /* process colors */
            UnknownColors.add(new ColorsEntry(lane, colorsMap));

            /* update laneJson and lanes object */
            laneJson.setUnknown(unknowns);
            lanes.put(String.valueOf(lane), laneJson);
        }
        colors.setUnknownColors(UnknownColors);
    }

    public static void parseLaneSummary() {
        List<ConversionResultsEntry> conversionResultsEntries = (isDirectory) ? multiStats.getConversionResults() : stats.getConversionResults();
        // List<DataHTML.LaneSummaryEntry> laneSummaryEntries = new ArrayList<>();
        List<ColorsEntry> LaneSummaryColors = new LinkedList<>();

        /* initialize map lanes (for json output generation) */
        for (int i = 0; i < conversionResultsEntries.size(); i++) {
            ConversionResultsEntry c = conversionResultsEntries.get(i);
            int LaneNumber = c.getLaneNumber();
            TotalPFClustersRaw += c.getTotalClustersRaw();
            TotalPFClusters += c.getTotalClustersPF();
            TotalYield += c.getYield();

            List<Accession> accessionList = new ArrayList<>();
            Map<String, String> colorsMap = new LinkedHashMap<>();

            /* determined samples that have demux results   */
            for (int j = 0; j < c.getDemuxResults().size(); j++) {
                /* read in and store in class object */
                DemuxResultsEntry demuxEntry = c.getDemuxResults().get(j);
                long demuxEntryYield = demuxEntry.getYield();
                IndexMetrics demuxEntryIndexMetrics = demuxEntry.getIndexMetrics().get(0);

                String Sample = demuxEntry.getSampleId();
                String BarcodeSequence = demuxEntryIndexMetrics.getIndexSequence();
                long PFClusters = demuxEntry.getNumberReads();
                double pctLane = Math.round(demuxEntryYield * 100.00 / c.getYield() * 100.0) / 100.0 ;

                long correct = demuxEntryIndexMetrics.getMismatchCounts().get("0");
                long incorrect = demuxEntryIndexMetrics.getMismatchCounts().get("1");
                double pctPerfectBarcode = Math.round(correct * 100.00 / (correct + incorrect) * 100.0) / 100.0;
                double pctMismatchBarcode = Math.round((100.00 - pctPerfectBarcode) * 100.0 / 100.0);

                int Yield = (int) Math.round(demuxEntryYield / 1_000_000.00);

                long Q30R1 = demuxEntry.getReadMetrics().get(0).getYieldQ30();
                long Q30R2 = demuxEntry.getReadMetrics().get(1).getYieldQ30();
                double pctQ30Bases = Math.round((Q30R1 + Q30R2) * 100.00 / demuxEntryYield * 100.0) / 100.0;

                long QScoreR1 = demuxEntry.getReadMetrics().get(0).getQualityScoreSum();
                long QScoreR2 = demuxEntry.getReadMetrics().get(1).getQualityScoreSum();
                double MeanQScore = Math.round((QScoreR1 + QScoreR2) * 100.00 / demuxEntryYield) / 100.0;

                LaneSummaryEntry laneSummaryEntry = new LaneSummaryEntry(LaneNumber, "default", Sample, BarcodeSequence, PFClusters, pctLane, pctPerfectBarcode, pctMismatchBarcode, Yield, pctQ30Bases, MeanQScore);
                /* process determined samples that have demux results */
                accessionList.add(processLaneSummary(LaneNumber, Sample, BarcodeSequence, PFClusters, pctLane, pctQ30Bases, MeanQScore));

                /* process color */
                colorsMap.put(demuxEntry.getSampleId(), laneSummaryColor);
                // contents += LaneNumber + " " + Sample + " " + BarcodeSequence + " " + PFCluste, rs + " " + pctLane + " " + pctPerfectBarcode + " " + pctMismatchBarcode + " " + Yield + " " + pctQ30Bases + " " + MeanQScore + "\n";
            }


            /* undetermined samples */
            Undetermined u = c.getUndetermined();
            long uYield = u.getYield();
            long PFClusters = u.getNumberReads();
            double pctLane = Math.round(uYield * 100.00 / c.getYield() * 100.0) / 100.0;

            int Yield = (int) Math.round(uYield / 1_000_000.00);

            long Q30R1 = u.getReadMetrics().get(0).getYieldQ30();
            long Q30R2 = u.getReadMetrics().get(1).getYieldQ30();
            double pctQ30Bases = Math.round((Q30R1 + Q30R2) * 100.00 / uYield * 100.0) / 100.0;

            long QScoreR1 = u.getReadMetrics().get(0).getQualityScoreSum();
            long QScoreR2 = u.getReadMetrics().get(1).getQualityScoreSum();
            double MeanQScore = Math.round((QScoreR1 + QScoreR2) * 100.00 / uYield) / 100.0;

            LaneSummaryEntry laneSummaryEntry = new LaneSummaryEntry(LaneNumber, "default", "Undetermined", "unknown", PFClusters, pctLane, 100.00, NaN, Yield, pctQ30Bases, MeanQScore);
            // laneSummaryEntries.add(laneSummaryEntry);

            /* process undetermined samples */
            accessionList.add(processLaneSummary(LaneNumber, "Undetermined", "unknown", PFClusters, pctLane, pctQ30Bases, MeanQScore));

            /* process colors */
            colorsMap.put("Undetermined", laneSummaryColor);
            LaneSummaryColors.add(new ColorsEntry(c.getLaneNumber(), colorsMap));

            /* update lanes object */
            LaneJson laneJson = lanes.get(String.valueOf(LaneNumber));
            laneJson.setAccessions(accessionList);
            lanes.replace(String.valueOf(LaneNumber), laneJson);

            //contents += LaneNumber + " Parser.ConversionResults.Undetermined unknown " + PFClusters + " 100.00 NaN " + Yield + " " + pctQ30Bases + " " + MeanQScore + "\n";
        }
        colors.setLaneSummaryColors(LaneSummaryColors);
    }

    public static MultiStats readMultiReports() {
        List<Stats> statsList = new LinkedList<>();
        try {
            for (File file : new File(inFilePath).listFiles()) {
                FileReader reader = new FileReader(file);
                Stats s = gson.fromJson(reader, Stats.class);
                statsList.add(s);
            }

            List<String> flowcells = new LinkedList<>();
            List<Integer> runNumbers = new LinkedList<>();
            List<String> runIds = new LinkedList<>();
            List<LaneNumberReadInfos> laneNumberReadInfos = new LinkedList<>();
            List<ConversionResultsEntry> conversionResults = new LinkedList<>();
            List<UnknownBarcodesEntry> unknownBarcodes = new LinkedList<>();

            for (Stats st : statsList) {
                // flowcell portion
                flowcells.add(st.getFlowcell());
                runNumbers.add(st.getRunNumber());
                runIds.add(st.getRunId());

                // readinfosforlanes portion


                // conversion results portion
                for (ConversionResultsEntry c1 : st.getConversionResults()) {
                    boolean cflag = false;
                    for (ConversionResultsEntry c2 : conversionResults) {
                        if (c1.getLaneNumber() == c2.getLaneNumber()) {
                            List<DemuxResultsEntry> merged = ListUtils.union(c1.getDemuxResults(), c2.getDemuxResults());
                            conversionResults.get(c1.getLaneNumber()).setDemuxResults(merged);
                            cflag = true;
                            break;
                        }
                    }
                    if (!cflag) {
                        conversionResults.add(c1);
                    }
                }

                // unknown barcodes portion
                for (UnknownBarcodesEntry u1 : st.getUnknownBarcodes()) {
                    boolean uflag = false;
                    for (UnknownBarcodesEntry u2 : unknownBarcodes) {
                        if (u1.getLane() == u2.getLane()) {
                            Map<String, Long> merged = new LinkedHashMap<>();
                            merged.putAll(u1.getBarcodes());
                            merged.putAll(u2.getBarcodes());
                            unknownBarcodes.get(u1.getLane()).setBarcodes(merged);
                            uflag = true;
                            break;
                        }
                    }
                    if (!uflag) {
                        unknownBarcodes.add(u1);
                    }
                }
            }

            return new MultiStats(flowcells, runNumbers, runIds, laneNumberReadInfos, conversionResults, unknownBarcodes);

        } catch (IOException ioe) {
            System.out.println("ioe while parsing multiple reports: " + ioe.getMessage());
        }

        return null;
    }

    public static Stats readSingleReport() {
        try {
            FileReader reader = new FileReader(new File(inFilePath));
            return gson.fromJson(reader, Stats.class);
        } catch (IOException ioe) {
            System.out.println("ioe while parsing single report: " + ioe.getMessage());
        }

        return null;
    }

    public static void processJsonOutput() {
        String json = "";
        if (isDirectory) {
            List<String> flowcells = multiStats.getFlowcell();
            multiData = new MultiData(flowcells, "checkLaneBarcodes", lanes);
            json = gson.toJson(multiData);
        } else {
            String flowcell = stats.getFlowcell();
            data = new Data(flowcell, "checkLaneBarcodes", lanes);
            json = gson.toJson(data);
        }

        try {
            // FileWriter writer = new FileWriter(outJsonPath);
            PrintWriter pw = new PrintWriter(outJsonPath);
            pw.println(json);
            pw.close();
        } catch (IOException ioe) {
            System.out.println("ioe while writing to checked json file: " + ioe.getMessage());
        }
    }

    public static void main(String[] args) {

        /* solution 3: use System.getProperty */
        inFilePath = System.getProperty("input");
        outJsonPath = System.getProperty("output");
        if (System.getProperty("rawClustersMin") != null) lsCheckRawClustersMin = Long.valueOf(System.getProperty("rawClustersMin"));
        if (System.getProperty("countMax") != null) ukCheckCountMax = Long.valueOf(System.getProperty("countMax"));
        if (System.getProperty("pctLaneMin") != null) lsCheckpctLaneMin = Double.valueOf(System.getProperty("pctLaneMin"));

        /* check that the input file (folder) exists */
        File infile = new File(inFilePath);
        if (!infile.exists()) {
            System.out.println("ERROR: " + inFilePath + " does not exist.");
            System.exit(1);
        }

        /* make sure that runfolder exists */
        if (outJsonPath.length() < 1) {
            System.out.println("ERROR: -r runfolder is required.");
            System.exit(1);
        }

        isDirectory = infile.isDirectory();
        if (isDirectory) {
            // parse multiple reports
            multiStats = readMultiReports();
        } else {
            // parse different reports
            stats = readSingleReport();
        }

        /* read in json file as FileReader */
        /* create Gson object based on FileReader object */


        /* Part 1: Flowcell Summary */
        // String flowcell = stats.getFlowcell();

        /* Part 2: Top Unknown Barcodes (fetch top 10 UnknownBarcodesEntry objects)
         *  (parse unknown table before lane summary to build unknown array)
         */
        // parseUnknownTable();
        processUnknownTable();

        /* Part 3: Lane Summary */
        parseLaneSummary();

        String FlowCellSummaryColor = "";

        // compute isValid
        if (bMatchLargeUnknown || bLowerThanClusterMin || bLowerThanPercentMin || bUnknownBarcodeHigh || bLowerQScore) {
            bIsValid = false;
        }

        if (bIsValid) {
            // set flowcell summary to okColor
            FlowCellSummaryColor = "ok";
        } else {
            // set flowcell summary to errorColor
            FlowCellSummaryColor = "error";
        }

        flowCellSummary = new FlowCellSummary(TotalPFClustersRaw, TotalPFClusters, Math.round(TotalYield / 1_000_000.00), FlowCellSummaryColor);
        //FlowCellSummary flowCellSummary = new FlowCellSummary(TotalPFClustersRaw, TotalPFClusters, Math.round(TotalYield / 1_000_000.00));

        /* output to the resulting html page */

        /* output to the resulting json file (only when not valid)  */
        if (!bIsValid) {
            processJsonOutput();
        }

        System.out.println("Processed and inspected by Fulgent checkLaneBarcodes " + version);
        if (bMatchLargeUnknown) {
            System.out.println("ALERT: DataJson.LaneJson.LaneJson.Unknown barcode match found and count is larger than sample(s).");
        }

        if (bLowerThanClusterMin) {
            System.out.println("ALERT: Some sample(s) has raw cluster lower than minimun of " + lsCheckRawClustersMin);
        }

        if (bLowerThanPercentMin) {
            System.out.println("ALERT: Some sample(s) has percentage lower than " + lsCheckpctLaneMin);
        }

        if (bLowerQScore) {
            System.out.println("ALERT: Some sample(s) Q30 is less than 80%, and mean quality score is less than 34.00");
        }

        if (bUnknownBarcodeHigh) {
            System.out.println("CAUTION: Some unknown barcode(s) higher than " + ukCheckCountMax);
        }

        System.out.println();
        System.out.println("Validation Conditions: ");
        System.out.println("Cluster > " + lsCheckRawClustersMin);
        System.out.println("% lane > " + lsCheckpctLaneMin);
        System.out.println("Unknown barcode count < " + ukCheckCountMax);

    }
}

