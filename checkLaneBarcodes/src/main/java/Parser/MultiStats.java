package Parser;

import Parser.ConversionResults.ConversionResultsEntry;
import Parser.LaneNumberReadInfos.LaneNumberReadInfos;
import Parser.UnknownBarcodes.UnknownBarcodesEntry;

import java.util.List;

public class MultiStats {
    private List<String> Flowcell;
    private List<Integer> RunNumber;
    private List<String> RunId;
    private List<LaneNumberReadInfos> ReadInfosForLanes;
    private List<ConversionResultsEntry> ConversionResults;
    private List<UnknownBarcodesEntry> UnknownBarcodes;

    public MultiStats(List<String> flowcell, List<Integer> runNumber, List<String> runId, List<LaneNumberReadInfos> readInfosForLanes, List<ConversionResultsEntry> conversionResults, List<UnknownBarcodesEntry> unknownBarcodes) {
        Flowcell = flowcell;
        RunNumber = runNumber;
        RunId = runId;
        ReadInfosForLanes = readInfosForLanes;
        ConversionResults = conversionResults;
        UnknownBarcodes = unknownBarcodes;
    }

    public List<String> getFlowcell() {
        return Flowcell;
    }

    public List<Integer> getRunNumber() {
        return RunNumber;
    }

    public List<String> getRunId() {
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
}
