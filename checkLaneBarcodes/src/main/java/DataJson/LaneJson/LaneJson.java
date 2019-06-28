package DataJson.LaneJson;

import DataJson.LaneJson.LaneInfos.Accession;
import DataJson.LaneJson.LaneInfos.Unknown;

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

    public void setAccessions(List<Accession> accessions) {
        this.accessions = accessions;
    }

    public List<Unknown> getUnknown() {
        return unknown;
    }

    public void setUnknown(List<Unknown> unknown) {
        this.unknown = unknown;
    }
}
