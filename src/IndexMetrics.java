import java.util.Map;

public class IndexMetrics {
    private String IndexSequence;
    private Map<String, Long> MismatchCounts;

    public String getIndexSequence() {
        return IndexSequence;
    }

    public Map<String, Long> getMismatchCounts() {
        return MismatchCounts;
    }
}
