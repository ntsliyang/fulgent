package StatsHTML;

import java.util.Map;
import java.util.Objects;

public class UnknownTableEntry {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnknownTableEntry that = (UnknownTableEntry) o;
        return Lane == that.Lane &&
                Objects.equals(Barcodes, that.Barcodes);
    }

    int Lane;
    Map<Long, String> Barcodes;

    public UnknownTableEntry(int lane, Map<Long, String> barcodes) {
        Lane = lane;
        Barcodes = barcodes;
    }

}
