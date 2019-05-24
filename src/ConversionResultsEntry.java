import java.util.List;

public class ConversionResultsEntry {
    private int LaneNumber;
    private long TotalClustersRaw;
    private long TotalClustersPF;
    private long Yield;
    private List<DemuxResultsEntry> DemuxResults;
    private Undetermined Undetermined;

    public int getLaneNumber() {
        return LaneNumber;
    }

    public long getTotalClustersRaw() {
        return TotalClustersRaw;
    }

    public long getTotalClustersPF() {
        return TotalClustersPF;
    }

    public long getYield() {
        return Yield;
    }

    public List<DemuxResultsEntry> getDemuxResults() {
        return DemuxResults;
    }

    public Undetermined getUndetermined() {
        return Undetermined;
    }
}
