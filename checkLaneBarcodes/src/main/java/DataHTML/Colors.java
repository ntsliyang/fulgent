package DataHTML;


import java.util.List;
import java.util.Map;

public class Colors {
    List<ColorsEntry> LaneSummaryColors;
    List<ColorsEntry> UnknownColors;

    public Colors(List<ColorsEntry> laneSummaryColors, List<ColorsEntry> unknownColors) {
        LaneSummaryColors = laneSummaryColors;
        UnknownColors = unknownColors;
    }

    public List<ColorsEntry> getLaneSummaryColors() {
        return LaneSummaryColors;
    }

    public void setLaneSummaryColors(List<ColorsEntry> laneSummaryColors) {
        LaneSummaryColors = laneSummaryColors;
    }

    public List<ColorsEntry> getUnknownColors() {
        return UnknownColors;
    }

    public void setUnknownColors(List<ColorsEntry> unknownColors) {
        UnknownColors = unknownColors;
    }

//    public String findColor(int lane, int sample) {
//        for (int i = 0; i < colors.size(); i++) {
//            if (colors.get(i).getLane() == lane) {
//                for (Map.Entry entry : colors.get(i).getColors().entrySet()) {
//                    if (entry.getKey().equals(sample)) {
//                        return (String) entry.getValue();
//                    }
//                }
//            }
//        }
//        return "";
//    }
}
