package Parser.UnknownBarcodes;

import java.util.Map;
import java.util.Objects;

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

    public void setLane(int lane) {
        Lane = lane;
    }

    public void setBarcodes(Map<String, Long> barcodes) {
        Barcodes = barcodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnknownBarcodesEntry)) return false;
        UnknownBarcodesEntry that = (UnknownBarcodesEntry) o;
        return Lane == that.Lane &&
                Objects.equals(Barcodes, that.Barcodes);
    }
}
