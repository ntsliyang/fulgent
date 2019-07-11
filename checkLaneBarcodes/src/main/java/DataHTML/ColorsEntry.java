package DataHTML;

import java.util.Map;

public class ColorsEntry {
    int lane;
    Map<String, String> colorsMap;
    // lane summary: from sample to colors
    // unknown barcodes: from sequence to colors

    public ColorsEntry(int lane, Map<String, String> colorsMap) {
        this.lane = lane;
        this.colorsMap = colorsMap;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public Map<String, String> getColorsMap() {
        return colorsMap;
    }

    public void setColorsMap(Map<String, String> colorsMap) {
        this.colorsMap = colorsMap;
    }
}
