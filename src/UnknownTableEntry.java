import java.util.Map;

public class UnknownTableEntry {
    int Lane;
    Map<Long, String> Barcodes;

    public UnknownTableEntry(int lane, Map<Long, String> barcodes) {
        Lane = lane;
        Barcodes = barcodes;
    }
}
