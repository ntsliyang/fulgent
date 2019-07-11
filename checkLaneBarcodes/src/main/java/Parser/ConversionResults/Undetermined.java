package Parser.ConversionResults;

import Parser.ConversionResults.DemuxResults.ReadMetricsEntry;

import java.util.List;
import java.util.Objects;

public class Undetermined {
    private long NumberReads;
    private long Yield;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Undetermined)) return false;
        Undetermined that = (Undetermined) o;
        return NumberReads == that.NumberReads &&
                Yield == that.Yield &&
                Objects.equals(ReadMetrics, that.ReadMetrics);
    }

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
