package Parser.ConversionResults.DemuxResults;

import java.util.Comparator;

public class DemuxResultsEntryComparator implements Comparator<DemuxResultsEntry> {

    @Override
    public int compare(DemuxResultsEntry d1, DemuxResultsEntry d2) {
        return d1.getSampleId().compareTo(d2.getSampleId());
    }
}
