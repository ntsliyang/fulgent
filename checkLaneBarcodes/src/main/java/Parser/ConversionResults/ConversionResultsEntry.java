package Parser.ConversionResults;

import Parser.ConversionResults.DemuxResults.DemuxResultsEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ConversionResultsEntry {
    private int LaneNumber;
    private long TotalClustersRaw;
    private long TotalClustersPF;
    private long Yield;
    private List<DemuxResultsEntry> DemuxResults;
    private Parser.ConversionResults.Undetermined Undetermined;

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

    public void setLaneNumber(int laneNumber) {
        LaneNumber = laneNumber;
    }

    public void setTotalClustersRaw(long totalClustersRaw) {
        TotalClustersRaw = totalClustersRaw;
    }

    public void setTotalClustersPF(long totalClustersPF) {
        TotalClustersPF = totalClustersPF;
    }

    public void setYield(long yield) {
        Yield = yield;
    }

    public void setDemuxResults(List<DemuxResultsEntry> demuxResults) {
        DemuxResults = demuxResults;
    }

    public void setUndetermined(Parser.ConversionResults.Undetermined undetermined) {
        Undetermined = undetermined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversionResultsEntry)) return false;
        ConversionResultsEntry that = (ConversionResultsEntry) o;
        return LaneNumber == that.LaneNumber &&
                TotalClustersRaw == that.TotalClustersRaw &&
                Yield == that.Yield &&
                Objects.equals(DemuxResults, that.DemuxResults) &&
                Objects.equals(Undetermined, that.Undetermined);
    }
}
