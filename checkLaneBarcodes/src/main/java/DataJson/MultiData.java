package DataJson;

import DataJson.LaneJson.LaneJson;

import java.util.List;
import java.util.Map;

public class MultiData {
    List<String> flowcell;
    String node;
    Map<String, LaneJson> lanes;

    public MultiData(List<String> flowcell, String node, Map<String, LaneJson> lanes) {
        this.flowcell = flowcell;
        this.node = node;
        this.lanes = lanes;
    }


}
