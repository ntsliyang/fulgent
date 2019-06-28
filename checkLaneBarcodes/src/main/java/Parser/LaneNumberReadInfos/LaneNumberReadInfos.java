package Parser.LaneNumberReadInfos;

import java.util.List;
import java.util.Objects;

public class LaneNumberReadInfos {
    private int LaneNumber;
    private List<ReadInfos> ReadInfos;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LaneNumberReadInfos)) return false;
        LaneNumberReadInfos that = (LaneNumberReadInfos) o;
        return LaneNumber == that.LaneNumber &&
                Objects.equals(ReadInfos, that.ReadInfos);
    }

}
