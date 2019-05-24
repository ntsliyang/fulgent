import java.util.List;

public class Undetermined {
    private long NumberReads;
    private long Yield;
    private List<ReadMetricsEntry> ReadMetrics;

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
