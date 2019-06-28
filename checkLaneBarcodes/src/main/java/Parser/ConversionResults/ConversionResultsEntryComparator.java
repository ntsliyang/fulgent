package Parser.ConversionResults;

import java.util.Comparator;

public class ConversionResultsEntryComparator implements Comparator<ConversionResultsEntry> {

    @Override
    public int compare(ConversionResultsEntry c1, ConversionResultsEntry c2) {
        return c1.getLaneNumber() - c2.getLaneNumber();
    }
}
