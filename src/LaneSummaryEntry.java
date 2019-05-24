public class LaneSummaryEntry {
    int Lane;
    String Project;
    String Sample;
    String BarcodeSequence;
    long PFClusters;
    double pctLane;
    double pctPerfectBarcode;
    double pctMismatchBarcode;
    int Yield;
    // double pctPFClusters;
    double pctQ30Bases;
    double MeanQScore;

    public LaneSummaryEntry(int lane, String project, String sample, String barcodeSequence, long PFClusters, double pctLane, double pctPerfectBarcode, double pctMismatchBarcode, int yield, double pctQ30Bases, double meanQScore) {
        Lane = lane;
        Project = project;
        Sample = sample;
        BarcodeSequence = barcodeSequence;
        this.PFClusters = PFClusters;
        this.pctLane = pctLane;
        this.pctPerfectBarcode = pctPerfectBarcode;
        this.pctMismatchBarcode = pctMismatchBarcode;
        Yield = yield;
        this.pctQ30Bases = pctQ30Bases;
        MeanQScore = meanQScore;
    }

    public int getLane() {
        return Lane;
    }

    public String getProject() {
        return Project;
    }

    public String getSample() {
        return Sample;
    }

    public String getBarcodeSequence() {
        return BarcodeSequence;
    }

    public long getPFClusters() {
        return PFClusters;
    }

    public double getPctLane() {
        return pctLane;
    }

    public double getPctPerfectBarcode() {
        return pctPerfectBarcode;
    }

    public double getPctMismatchBarcode() {
        return pctMismatchBarcode;
    }

    public int getYield() {
        return Yield;
    }

    public double getPctQ30Bases() {
        return pctQ30Bases;
    }

    public double getMeanQScore() {
        return MeanQScore;
    }
}
