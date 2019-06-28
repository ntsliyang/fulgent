package Parser.ConversionResults;

import java.util.Map;
import java.util.Objects;

public class IndexMetrics {
    private String IndexSequence;
    private Map<String, Long> MismatchCounts;

    public String getIndexSequence() {
        return IndexSequence;
    }

    public Map<String, Long> getMismatchCounts() {
        return MismatchCounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexMetrics)) return false;
        IndexMetrics that = (IndexMetrics) o;
        return Objects.equals(IndexSequence, that.IndexSequence) &&
                Objects.equals(MismatchCounts, that.MismatchCounts);
    }

}
