import java.util.List;

public class DemuxResultsEntry {
    private String SampleId;
    private String SampleName;
    private List<IndexMetrics> IndexMetrics;
    private long NumberReads;
    private long Yield;
    private List<ReadMetricsEntry> ReadMetrics;

    public String getSampleId() {
        return SampleId;
    }

    public String getSampleName() {
        return SampleName;
    }

    public List<IndexMetrics> getIndexMetrics() {
        return IndexMetrics;
    }

    public long getNumberReads() {
        return NumberReads;
    }

    public long getYield() {
        return Yield;
    }

    public List<ReadMetricsEntry> getReadMetrics() {
        return ReadMetrics;
    }
}
