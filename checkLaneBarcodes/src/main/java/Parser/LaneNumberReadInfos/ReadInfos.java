package Parser.LaneNumberReadInfos;

public class ReadInfos {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadInfos readInfos = (ReadInfos) o;
        return Number == readInfos.Number &&
                NumCycles == readInfos.NumCycles &&
                IsIndexedRead == readInfos.IsIndexedRead;
    }

    private int Number;
    private int NumCycles;
    private boolean IsIndexedRead;
}
