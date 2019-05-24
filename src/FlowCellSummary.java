public class FlowCellSummary {
    long TotalClustersRaw;
    long TotalClustersPF;
    long Yield;
    String color;

    public FlowCellSummary(long totalClustersRaw, long totalClustersPF, long yield, String c) {
        TotalClustersRaw = totalClustersRaw;
        TotalClustersPF = totalClustersPF;
        Yield = yield;
        color = c;
    }
}
