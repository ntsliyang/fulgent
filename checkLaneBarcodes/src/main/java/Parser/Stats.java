package Parser;

import Parser.ConversionResults.ConversionResultsEntry;
import Parser.LaneNumberReadInfos.LaneNumberReadInfos;
import Parser.UnknownBarcodes.UnknownBarcodesEntry;

import java.util.List;
import java.util.Objects;

public class Stats {
    private String Flowcell;
    private int RunNumber;
    private String RunId;
    private List<LaneNumberReadInfos> ReadInfosForLanes;
    private List<ConversionResultsEntry> ConversionResults;
    private List<UnknownBarcodesEntry> UnknownBarcodes;

    public String getFlowcell() {
        return Flowcell;
    }

    public int getRunNumber() {
        return RunNumber;
    }

    public String getRunId() {
        return RunId;
    }

    public List<LaneNumberReadInfos> getReadInfosForLanes() {
        return ReadInfosForLanes;
    }

    public List<ConversionResultsEntry> getConversionResults() {
        return ConversionResults;
    }

    public List<UnknownBarcodesEntry> getUnknownBarcodes() {
        return UnknownBarcodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stats)) return false;
        Stats stats = (Stats) o;
        return RunNumber == stats.RunNumber &&
                Objects.equals(Flowcell, stats.Flowcell) &&
                Objects.equals(RunId, stats.RunId) &&
                Objects.equals(ReadInfosForLanes, stats.ReadInfosForLanes) &&
                Objects.equals(ConversionResults, stats.ConversionResults) &&
                Objects.equals(UnknownBarcodes, stats.UnknownBarcodes);
    }
}
