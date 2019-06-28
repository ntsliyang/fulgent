package Parser.ConversionResults.DemuxResults;

import java.util.List;
import java.util.Objects;

public class DemuxResultsEntry {
    private String SampleId;
    private String SampleName;
    private List<Parser.ConversionResults.IndexMetrics> IndexMetrics;
    private long NumberReads;
    private long Yield;
    private List<ReadMetricsEntry> ReadMetrics;

    public String getSampleId() {
        return SampleId;
    }

    public String getSampleName() {
        return SampleName;
    }

    public List<Parser.ConversionResults.IndexMetrics> getIndexMetrics() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DemuxResultsEntry)) return false;
        DemuxResultsEntry that = (DemuxResultsEntry) o;
        return NumberReads == that.NumberReads &&
                Yield == that.Yield &&
                Objects.equals(SampleId, that.SampleId) &&
                Objects.equals(SampleName, that.SampleName) &&
                Objects.equals(IndexMetrics, that.IndexMetrics) &&
                Objects.equals(ReadMetrics, that.ReadMetrics);
    }
}
