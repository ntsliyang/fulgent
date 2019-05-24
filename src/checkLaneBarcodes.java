import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import javax.sound.midi.SysexMessage;
import java.io.*;
import java.lang.Math;
import java.util.*;

import static java.lang.Double.NaN;

public class checkLaneBarcodes {

    private static boolean debug = false;
    private static boolean bIsValid = true;
    private static boolean bMatchLargeUnknown = false;
    private static boolean bLowerThanClusterMin = false;
    private static boolean bLowerThanPercentMin = false;
    private static boolean bLowerQScore = false;
    private static boolean bUnknownBarcodeHigh = false;

    private static String okColor = "#aaffaa";
    private static String errorColor = "#ffaaaa";
    private static String warningcolor = "#ffffaa";
    private static String invalidBarcodeColor = "#777777";

    private static int topLimit = 10;
    private static long ukCheckCountMax = 1_000_000;
    private static long lsCheckRawClustersMin = 1_000_000;
    private static double lsCheckpctLaneMin = 0.1;

    private static List<UnknownArrayEntry> UnknownArray = new ArrayList<>();
    private static String version = "1.1.2";
    private static String inFilePath = "test/Stats - Copy.json";
   //  private static String outFilePath = "html/html_result1";
    // private static String outFilePath = "html/html_result2.html";
    private static String outJsonPath = "test/Stats_checked.json";

    private static void swap(char c1, char c2) {
        char temp = c1;
        c1 = c2;
        c2 = temp;
    }

    private static String reverse(String s) {
        int len = s.length();
        char[] sChars = s.toCharArray();
        for (int i = 0; i < Math.floor(s.length() / 2); i++) {
            swap(sChars[i], sChars[len - 1 - i]);
        }
        return String.valueOf(sChars);
    }

    private static String complement(String s) {
        StringBuilder str = new StringBuilder(s);
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == 'A') str.setCharAt(i, 'T');
            else if (s.charAt(i) == 'T') str.setCharAt(i, 'A');
            else if (s.charAt(i) == 'G') str.setCharAt(i, 'C');
            else if (s.charAt(i) == 'C') str.setCharAt(i, 'G');
        }
        return str.toString();
    }

    private static String isMatchBarcode(String b1, String b2) {
        if (b1 == b2) return "exact";
        if (reverse(b1).equals(b2)) return "reverse";
        if (complement(b1).equals(b2)) return "complement";
        if (complement(reverse(b1)).equals(b2)) return "reverse-complement";
        return "";
    }

    private static UnknownArrayEntry matchSample(int lane, String barcode) {
        String[] bc = barcode.split("[-+]");
        UnknownArrayEntry matchedUi = null;

        for (int i = 0; i < UnknownArray.size(); i++) {
            UnknownArrayEntry ui = UnknownArray.get(i);
            if (lane == ui.getLane()) {
                // compare
                String [] ubc = ui.getBarcode().split("[-+]");
                if (!isMatchBarcode(bc[0], ubc[0]).isEmpty() && (bc.length < 2 || !isMatchBarcode(bc[1], ubc[1]).isEmpty())) {
                    String msg = "Found " + ui.getBarcode() + " f:" + isMatchBarcode(bc[0], ubc[0]) + (bc.length > 1 ? " e:" + isMatchBarcode(bc[1], ubc[1]) : "");
                    String uimsg = "Match " + barcode + " on lane:" + lane;

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

    private static Accession processLaneSummary(int LaneNumber, String Sample, String BarcodeSequence, long PFClusters, double pctLane, double pctQ30Bases, double MeanQScore) {
        /* process the lane summary data */
        boolean bValidSampleRow = false;
        UnknownArrayEntry matchedUi = null;
        String message = "";
        String status = "SUCCESS";
        String fbc = "";
        String ebc = "";
        String color = "white";

        /* process barcode sequence  */
        if (BarcodeSequence.matches("[ACGT]{6,12}([-+][ACGT]{6,12})?")) {
            bValidSampleRow = true;
            matchedUi = matchSample(LaneNumber, BarcodeSequence);

            if (matchedUi != null) {
                message += matchedUi.getItem_matching();
            }

            String[] bc = BarcodeSequence.split("[-+]");
            fbc = bc[0];
            ebc = bc.length > 1 ? bc[1] : "";

        } else {
            // set color
            if (BarcodeSequence.equals("unknown")) {
                if (color.contains("white")) color = "Barcode Sequence: invalid barcode\n";
                else color += "Barcode Sequence: invalid barcode\n";
            }
        }

        /* process pf clusters (count) */
        if (matchedUi != null && matchedUi.getCount() > PFClusters && !Sample.contains("EXCLUDED")) {
            bMatchLargeUnknown = true; // error
            status = "ERROR";
        }

        /* process pf clusters (min) */
        if (bValidSampleRow && PFClusters < lsCheckRawClustersMin && !Sample.contains("EXCLUDED")) {
            if (color.contains("white")) color = "PF Clusters: warning\n"; // set this very cell to warning color
            else color += "PF Clusters: warning\n";
            bLowerThanClusterMin = true; // error
        }

        if (bValidSampleRow && pctLane < lsCheckpctLaneMin && !Sample.contains("EXCLUDED")) {
            if (color.contains("white")) color = "% of the lane: warning\n"; // set this very cell to warning color
            else color += "% of the lane: warning\n";
            bLowerThanPercentMin = true; // error
        }

        if (pctQ30Bases < 80 && MeanQScore < 34 && !Sample.contains("EXCLUDED")) {
            if (color.contains("white")) color = "Mean Quality Score: warning\n";
            else color += "Mean Quality Score: warning\n";
            bLowerQScore = true;
            status = "ERROR";
        }

        return new Accession(Sample, PFClusters, pctLane, fbc, ebc, pctQ30Bases, status, message, color);
    }


    public static void main(String[] args) {
        /* check that the input file exists */
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

        /* read in json file as FileReader */
        FileReader reader = null;
        try {
            reader = new FileReader(infile);
        } catch (FileNotFoundException fnfe) {
            System.out.println("ERROR: file not found ");
        }

        /* create Gson object based on FileReader object */
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Stats stats = gson.fromJson(reader, Stats.class);

        /* Part 1: Flowcell Summary */
        String flowcell = stats.getFlowcell();

        Map<String, LaneJson> lanes = new LinkedHashMap<>();
        List<Accession> accessionList = null;
        /* Part 2: Top Unknown Barcodes (fetch top 10 UnknownBarcodesEntry objects)
        *  (parse unknown table before lane summary to build unknown array)
        */
        List<UnknownBarcodesEntry> unknownBarcodesEntries = stats.getUnknownBarcodes();
        List<UnknownBarcodesEntry> TopUnknownTableEntries = new ArrayList<>();
        Map<String, Long> Barcodes;
        for (int i = 0; i < unknownBarcodesEntries.size(); i++) {
            int lane = unknownBarcodesEntries.get(i).getLane();
            Map<String, Long> TopBarcodes = new LinkedHashMap<>();
            int counter = 0;
            for (Map.Entry<String, Long> entry : unknownBarcodesEntries.get(i).getBarcodes().entrySet()) {
                if (counter < 10) {
                    TopBarcodes.put(entry.getKey(), entry.getValue());
                    counter++;
                } else break;
            }
            UnknownBarcodesEntry un = new UnknownBarcodesEntry(lane, TopBarcodes);
            TopUnknownTableEntries.add(un);
        }

        /* process unknown table */
        for (int i = 0; i < TopUnknownTableEntries.size(); i++) {
            List<Unknown> unknowns = new ArrayList<>();
            LaneJson laneJson = new LaneJson(accessionList, unknowns);

            int lane = TopUnknownTableEntries.get(i).getLane();
            Map<String, Long> barcodes = TopUnknownTableEntries.get(i).getBarcodes();

            for (Map.Entry<String, Long> entry : barcodes.entrySet()) {
                String barcode = entry.getKey();
                long c = entry.getValue();

                Unknown uj = null;
                if (barcode.matches("[ACGTN]{6,12}([-+][ACGTN]{6,12})?") || barcode.matches("unknown") || barcode.length() < 1) {
                    // if barcode is empty, or unknown, or a combination of ACGTN, we create an Unknown object and ready to insert into data
                    String[] bc = barcode.split("[-+]");
                    String fbc = bc[0];
                    String ebc = bc.length > 1 ? bc[1] : "";
                    uj = new Unknown(c, fbc, ebc, "SUCCESS", "", "white");
                    unknowns.add(uj);
                }

                if (c > ukCheckCountMax && !barcode.matches("/AAAA|CCCC|GGGG|TTTT|NNNN/")) {
                    bUnknownBarcodeHigh = true;
                    if (uj != null) uj.setColor("Count: warning\n");
                }

                if (barcode.matches("[ACGT]{6,12}([-+][ACGT]{6,12})?") && !barcode.matches("N")) {
                    // create UnknownArray for valid barcodes (ACGT)
                    UnknownArrayEntry uae = new UnknownArrayEntry(lane, barcode, c, uj, "");
                    UnknownArray.add(uae);
                } else {
                    // set this very cell to invalidBarcodeColor
                    if (uj != null) {
                        if (!uj.getColor().contains("Count: warning\n")) uj.setColor("Sequence: invalid barcode\n");
                        else {
                            String ujc = uj.getColor();
                            uj.setColor(ujc + "Sequence: invalid barcode\n");
                        }
                    }
                }
            }

            /* update laneJson and lanes object */
            laneJson.setUnknown(unknowns);
            lanes.put(String.valueOf(lane), laneJson);
        }

        /* Part 3: Lane Summary */
         List<ConversionResultsEntry> conversionResultsEntries = stats.getConversionResults();
         List<LaneSummaryEntry> laneSummaryEntries = new ArrayList<>();
         long TotalPFClustersRaw = 0;
         long TotalPFClusters = 0;
         long TotalYield = 0;
        /* initialize map lanes (for json output generation) */
         for (int i = 0; i < conversionResultsEntries.size(); i++) {
             ConversionResultsEntry c = conversionResultsEntries.get(i);
             int LaneNumber = c.getLaneNumber();
             TotalPFClustersRaw += c.getTotalClustersRaw();
             TotalPFClusters += c.getTotalClustersPF();
             TotalYield += c.getYield();

             accessionList = new ArrayList<>();

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
                 laneSummaryEntries.add(laneSummaryEntry);

                 /* process determined samples that have demux results */
                 accessionList.add(processLaneSummary(LaneNumber, Sample, BarcodeSequence, PFClusters, pctLane, pctQ30Bases, MeanQScore));
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
             laneSummaryEntries.add(laneSummaryEntry);

             /* process undetermined samples */
             accessionList.add(processLaneSummary(LaneNumber, "Undetermined", "unknown", PFClusters, pctLane, pctQ30Bases, MeanQScore));

             /* update lanes object */
             LaneJson laneJson = lanes.get(String.valueOf(LaneNumber));
             laneJson.setAccessions(accessionList);
             lanes.replace(String.valueOf(LaneNumber), laneJson);
             // lanes.put(String.valueOf(LaneNumber), laneJson);
             //contents += LaneNumber + " Undetermined unknown " + PFClusters + " 100.00 NaN " + Yield + " " + pctQ30Bases + " " + MeanQScore + "\n";
         }

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

        FlowCellSummary flowCellSummary = new FlowCellSummary(TotalPFClustersRaw, TotalPFClusters, Math.round(TotalYield / 1_000_000.00), FlowCellSummaryColor);

        /* output to the resulting html page */

        /* output to the resulting json file (only when not valid)  */
        if (!bIsValid) {
            Data data = new Data(flowcell, "checkLaneBarcodes", flowCellSummary, lanes);
            try {
                // FileWriter writer = new FileWriter(outJsonPath);
                String json = gson.toJson(data);
                PrintWriter pw = new PrintWriter(outJsonPath);
                pw.println(json);
                pw.close();
            } catch (IOException ioe) {
                System.out.println("ioe while writing to checked json file: " + ioe.getMessage());
            }
        }

        System.out.println("Processed and inspected by Fulgent checkLaneBarcodes " + version);
        if (bMatchLargeUnknown) {
            System.out.println("ALERT: Unknown barcode match found and count is larger than sample(s).");
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

        /* [DEBUG] write to file */
//        try {
//            FileWriter fileWriter = new FileWriter("test/output.txt");
//            fileWriter.write(contents);
//            fileWriter.close();
//        } catch (IOException ioe) {
//            System.out.println("ioe while writing to file: " + ioe.getMessage());
//        }
    }
}

/* input json contents */

/* output json contents */

/* output webpage contents */

