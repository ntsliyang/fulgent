package DataJson.FlowCellSummary;

public class FlowCellSummary {
    long TotalClustersRaw;
    long TotalClustersPF;
    long Yield;
    String color;

    public FlowCellSummary(long totalClustersRaw, long totalClustersPF, long yield, String color) {
        TotalClustersRaw = totalClustersRaw;
        TotalClustersPF = totalClustersPF;
        Yield = yield;
        this.color = color;
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

    public String getColor() {
        return color;
    }
}
