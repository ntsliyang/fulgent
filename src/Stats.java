import java.util.List;

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
}
