import java.util.List;

public class LaneJson {
    List<Accession> accessions;
    List<Unknown> unknown;

    public LaneJson(List<Accession> accessions, List<Unknown> unknown) {
        this.accessions = accessions;
        this.unknown = unknown;
    }

    public List<Accession> getAccessions() {
        return accessions;
    }

    public List<Unknown> getUnknown() {
        return unknown;
    }

    public void setAccessions(List<Accession> accessions) {
        this.accessions = accessions;
    }

    public void setUnknown(List<Unknown> unknown) {
        this.unknown = unknown;
    }
}
