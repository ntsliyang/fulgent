package StatsHTML;

import java.util.Objects;

public class LaneSummaryEntry {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LaneSummaryEntry)) return false;
        LaneSummaryEntry that = (LaneSummaryEntry) o;
        return Lane == that.Lane &&
                PFClusters == that.PFClusters &&
                Double.compare(that.pctLane, pctLane) == 0 &&
                Double.compare(that.pctPerfectBarcode, pctPerfectBarcode) == 0 &&
                Double.compare(that.pctMismatchBarcode, pctMismatchBarcode) == 0 &&
                Yield == that.Yield &&
                Double.compare(that.pctQ30Bases, pctQ30Bases) == 0 &&
                Double.compare(that.MeanQScore, MeanQScore) == 0 &&
                Objects.equals(Project, that.Project) &&
                Objects.equals(Sample, that.Sample) &&
                Objects.equals(BarcodeSequence, that.BarcodeSequence);
    }

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
