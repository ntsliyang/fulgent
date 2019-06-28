package Parser.UnknownBarcodes;

import java.util.Comparator;

public class UnknownBarcodesEntryComparator implements Comparator<UnknownBarcodesEntry> {

    @Override
    public int compare(UnknownBarcodesEntry u1, UnknownBarcodesEntry u2) {
        return u1.getLane() - u2.getLane();
    }
}
