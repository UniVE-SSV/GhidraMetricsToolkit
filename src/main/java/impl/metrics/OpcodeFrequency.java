package impl.metrics;

import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class OpcodeFrequency implements SimilarityInterface {

    private static final String NAME = "Opcode Frequency Histogram";

    private static double computeSimilarity(Map<String, Double> histogram_1, Map<String, Double> histogram_2) {
        Set<String> opcodeSet = new HashSet<>(histogram_1.keySet());
        opcodeSet.addAll(histogram_2.keySet());

        double distance = 0.0;
        for (String opcode : opcodeSet) {
            distance += Math.pow(histogram_1.getOrDefault(opcode, 0.0) - histogram_2.getOrDefault(opcode, 0.0), 2);
        }

        return 1 - distance;
    }

    @Override
    public double compute(Function function1, Function function2) {
        Map<String, Double> histogram1 = FunctionUtils.getHistogram(function1);
        Map<String, Double> histogram2 = FunctionUtils.getHistogram(function2);
        return computeSimilarity(histogram1, histogram2);
    }

    @Override
    public String toString() {
        return NAME;
    }
}
