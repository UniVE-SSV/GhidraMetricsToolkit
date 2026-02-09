package impl.metrics;

import generic.algorithms.ReducingListBasedLcs;
import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;

import java.util.List;


public class Lcs implements SimilarityInterface {

    public static final String NAME = "Longest Common Subsequence";

    @Override
    public double compute(Function function1, Function function2) {
        List<String> l1 = FunctionUtils.getOpcodeListing(function1);
        List<String> l2 = FunctionUtils.getOpcodeListing(function2);
        ReducingListBasedLcs<String> rlcs = new ReducingListBasedLcs<>(l1, l2);
        rlcs.setSizeLimit(Integer.MAX_VALUE);
        return rlcs.getLcs().size() * 2.0 / (l1.size() + l2.size());
    }

    @Override
    public String toString() {
        return NAME;
    }
}
