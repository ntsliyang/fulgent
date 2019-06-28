package Parser.ConversionResults.DemuxResults;

public class ReadMetricsEntry {
    private long ReadNumber;
    private long Yield;
    private long YieldQ30;
    private long QualityScoreSum;
    private long TrimmedBases;

    public long getReadNumber() {
        return ReadNumber;
    }

    public long getYield() {
        return Yield;
    }

    public long getYieldQ30() {
        return YieldQ30;
    }

    public long getQualityScoreSum() {
        return QualityScoreSum;
    }

    public long getTrimmedBases() {
        return TrimmedBases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadMetricsEntry)) return false;
        ReadMetricsEntry that = (ReadMetricsEntry) o;
        return ReadNumber == that.ReadNumber &&
                Yield == that.Yield &&
                YieldQ30 == that.YieldQ30 &&
                QualityScoreSum == that.QualityScoreSum &&
                TrimmedBases == that.TrimmedBases;
    }
}
