import Parser.ConversionResults.ConversionResultsEntry;
import Parser.ConversionResults.DemuxResults.DemuxResultsEntry;
import Parser.ConversionResults.Undetermined;
import Parser.LaneNumberReadInfos.LaneNumberReadInfos;
import Parser.MultiStats;
import Parser.Stats;
import Parser.UnknownBarcodes.UnknownBarcodesEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class checkMultiLaneBarcodesTest {

    private static checkLaneBarcodes c = new checkLaneBarcodes();
    private static MultiStats multiStats = null;
    private static List<Stats> statsList = new LinkedList<>();

    class RepeatException extends Exception {
        public RepeatException(String msg) {
            super(msg);
        }
    }

    @Test
    void parseTest() throws Exception {
        // check that all entries have been loaded
        File infile = new File(c.getInFilePath());
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        for (File file : infile.listFiles()) {
            FileReader reader = new FileReader(file);
            Stats s = gson.fromJson(reader, Stats.class);
            statsList.add(s);
        }

        List<String> flowcells = new LinkedList<>();
        List<Integer> runNumbers = new LinkedList<>();
        List<String> runIds = new LinkedList<>();
        List<LaneNumberReadInfos> laneNumberReadInfos = new LinkedList<>();
        List<ConversionResultsEntry> conversionResults = new LinkedList<>();
        Set<ConversionResultsEntry> conversionResultsEntrySet = new HashSet<>(statsList.get(0).getConversionResults());
        Map<Integer, Set<DemuxResultsEntry>> demuxResultsEntryMapSet = new LinkedHashMap<>();
        Map<Integer, Undetermined> undeterminedMapSet = new LinkedHashMap<>();
        Map<Integer, Long> TotalClustersRaw = new LinkedHashMap<>();
        Map<Integer, Long> TotalClustersPF = new LinkedHashMap<>();
        Map<Integer, Long> Yield = new LinkedHashMap<>();

        if (!statsList.isEmpty() && !statsList.get(0).getConversionResults().isEmpty()) {
        }
        List<UnknownBarcodesEntry> unknownBarcodes = new LinkedList<>();
        Set<UnknownBarcodesEntry> unknownBarcodesEntrySet = new HashSet<>();

        for (Stats st : statsList) {
            // flowcell portion
            flowcells.add(st.getFlowcell());
            runNumbers.add(st.getRunNumber());
            runIds.add(st.getRunId());

            // readinfosforlanes portion

            // conversion results portion
            for (ConversionResultsEntry c : st.getConversionResults()) {
                // 1. check repeated DemuxResults and merge
                if (demuxResultsEntryMapSet.containsKey(c.getLaneNumber())) {
                    // lane number already added:

                    Set<DemuxResultsEntry> s1 = demuxResultsEntryMapSet.get(c.getLaneNumber());
                    List<DemuxResultsEntry> d2 = st.getConversionResults().get(c.getLaneNumber()).getDemuxResults();
                    Set<DemuxResultsEntry> combined = merge(s1, d2);
                    demuxResultsEntryMapSet.replace(c.getLaneNumber(), combined);

                } else {
                    // lane number hasn't been added:
                    // add the lane number and its corresponding demux results set into map

                    // as we can assume each entry is unique, we can directly convert this
                    // list directly to hashset. The size should be equivalent between list and set.

                    Set<DemuxResultsEntry> s = new HashSet<>(c.getDemuxResults());
                    assert s.size() == c.getDemuxResults().size() : "repeated entries in lane " + c.getLaneNumber() + " from source " + st.getFlowcell();
                    demuxResultsEntryMapSet.put(c.getLaneNumber(), s);
                }

                // 2. checked repeated Undetermined and merge
                if (undeterminedMapSet.containsKey(c.getLaneNumber())) {
                    // lane number already added:
                    Undetermined u1 = undeterminedMapSet.get(c.getLaneNumber());
                    Undetermined u2 = st.getConversionResults().get(c.getLaneNumber()).getUndetermined();
                    if (!u1.equals(u2)) {
                        // replace the one with maximum pf clusters
                        // when undetermined objects are different
                        Undetermined u = (u1.getNumberReads() > u2.getNumberReads()) ? u1 : u2;
                        undeterminedMapSet.replace(c.getLaneNumber(), u);
                    }
                } else {
                    // lane number hasn't been added:
                    // put current 'Undetermined' from current 'ConversionResultsEntry'
                    undeterminedMapSet.put(c.getLaneNumber(), c.getUndetermined());
                }

                // 3. update TotalClustersRaw, TotalClustersPF, Yield

                if (TotalClustersPF.containsKey(c.getLaneNumber())) {
                    // lane number already added:
                    st.getFlowcell()
                } else {
                    // lane number hasn't been added:
                    TotalClustersPF.put(c.getLaneNumber(), c.getTotalClustersPF());
                }

                // 4. update
                if ()
                // 5. update

            }
            for (ConversionResultsEntry c1 : st.getConversionResults()) {
                boolean cflag = false;
                for (ConversionResultsEntry c2 : conversionResults) {
                    if (c1.getLaneNumber() == c2.getLaneNumber()) {
                        List<DemuxResultsEntry> merged = ListUtils.union(c1.getDemuxResults(), c2.getDemuxResults());
                        conversionResults.get(c1.getLaneNumber()).setDemuxResults(merged);
                        cflag = true;
                        break;
                    }
                }
                if (!cflag) {
                    conversionResults.add(c1);
                }
            }

            // unknown barcodes portion
            for (UnknownBarcodesEntry u1 : st.getUnknownBarcodes()) {
                boolean uflag = false;
                for (UnknownBarcodesEntry u2 : unknownBarcodes) {
                    if (u1.getLane() == u2.getLane()) {
                        Map<String, Long> merged = new LinkedHashMap<>();
                        merged.putAll(u1.getBarcodes());
                        merged.putAll(u2.getBarcodes());
                        unknownBarcodes.get(u1.getLane()).setBarcodes(merged);
                        uflag = true;
                        break;
                    }
                }
                if (!uflag) {
                    unknownBarcodes.add(u1);
                }
            }
        }

        for (int i = 0; i < conversionResults.size(); i++) {
            for (int j = 0; j < conversionResults.size() && j != i; j++) {
                if (conversionResults.get(i).getLaneNumber() == conversionResults.get(j).getLaneNumber()) {
                    // same lane number => check if they have the same demux results and undetermined
                    List<DemuxResultsEntry> l1 = conversionResults.get(i).getDemuxResults();
                    List<DemuxResultsEntry> l2 = conversionResults.get(j).getDemuxResults();

                }
            }
        }

        multiStats = new MultiStats(flowcells, runNumbers, runIds, laneNumberReadInfos, conversionResults, unknownBarcodes);

        // check the total number is equivalent
        int files = multiStats.getFlowcell().size();
        int infos = multiStats.getReadInfosForLanes().size();
        int ids = multiStats.getRunId().size();

        assertEquals(files, statsList.size());
        assertEquals(infos, statsList.size());
        assertEquals(ids, statsList.size());

        int countC = multiStats.getConversionResults().size();
        int countU = multiStats.getUnknownBarcodes().size();

        Set<Integer> countConversionLanes = new HashSet<>();
        Set<Integer> countUnknownLanes = new HashSet<>();

        for (Stats s : statsList) {
            for (ConversionResultsEntry c : s.getConversionResults()) {
                countConversionLanes.add(c.getLaneNumber());
            }
            for (UnknownBarcodesEntry u : s.getUnknownBarcodes()) {
                countUnknownLanes.add(u.getLane());
            }
        }

        assertEquals(countC, countConversionLanes.size());
        assertEquals(countU, countUnknownLanes);
        assertEquals(countC, countUnknownLanes);
        assertEquals(countU, countConversionLanes.size());

        // check all the contents are equivalent
        // the contents are unique (maybe unordered) and can be mapped to
        // individual entry in the original json file
        Set<ConversionResultsEntry> hashConversion = new HashSet<>();

        for (ConversionResultsEntry c : multiStats.getConversionResults()) {
            hashConversion.add(c);
        }
        // make sure that when added to multiStats, there are no repeats
        assertEquals(hashConversion.size(), multiStats.getConversionResults().size());
    }

    public static <T> Set<T> merge(Set<T> s1, List<T> d2) {
        Set<T> s2 = new HashSet<>(d2);
        assert d2.size() == s2.size() : "repeated elements during merging " + s1.getClass().getSimpleName() + " from different sources";
        Set<T> combined = new HashSet<T>() {{addAll(s1); addAll(s2);}};
        assert combined.size() == s1.size() + s2.size() : "repeated elements during merging";
        return combined;
    }
}