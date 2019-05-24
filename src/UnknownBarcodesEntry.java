import java.util.Map;

public class UnknownBarcodesEntry {
    private int Lane;
    private Map<String, Long> Barcodes;

    public UnknownBarcodesEntry(int lane, Map<String, Long> barcodes) {
        Lane = lane;
        Barcodes = barcodes;
    }

    public int getLane() {
        return Lane;
    }

    public Map<String, Long> getBarcodes() {
        return Barcodes;
    }
}
