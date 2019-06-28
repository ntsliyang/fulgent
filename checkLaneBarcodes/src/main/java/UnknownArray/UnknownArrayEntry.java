package UnknownArray;

import DataJson.LaneJson.LaneInfos.Unknown;

public class UnknownArrayEntry {
    private int lane;
    private String barcode;
    private long count;
    private Unknown jdata_cell;
    private String item_matching;

    public UnknownArrayEntry(int lane, String barcode, long count, Unknown jdata_cell, String item_matching) {
        this.lane = lane;
        this.barcode = barcode;
        this.count = count;
        this.jdata_cell = jdata_cell;
        this.item_matching = item_matching;
    }

    public int getLane() {
        return lane;
    }

    public String getBarcode() {
        return barcode;
    }

    public long getCount() {
        return count;
    }

    public Unknown getJdata_cell() {
        return jdata_cell;
    }

    public String getItem_matching() {
        return item_matching;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setJdata_cell(Unknown jdata_cell) {
        this.jdata_cell = jdata_cell;
    }

    public void setItem_matching(String item_matching) {
        this.item_matching = item_matching;
    }
}
