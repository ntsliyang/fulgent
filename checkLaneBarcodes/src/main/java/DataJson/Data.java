package DataJson;

import DataJson.FlowCellSummary.FlowCellSummary;
import DataJson.LaneJson.LaneJson;

import java.util.Map;

public class Data {
    String flowcell;
    String node;
    // FlowCellSummary flowcellsummary;
    Map<String, LaneJson> lanes;

    public Data(String flowcell, String node, Map<String, LaneJson> lanes) {
        this.flowcell = flowcell;
        this.node = node;
        this.lanes = lanes;
    }

    public String getFlowcell() {
        return flowcell;
    }

    public void setFlowcell(String flowcell) {
        this.flowcell = flowcell;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public Map<String, LaneJson> getLanes() {
        return lanes;
    }

    public void setLanes(Map<String, LaneJson> lanes) {
        this.lanes = lanes;
    }
}
