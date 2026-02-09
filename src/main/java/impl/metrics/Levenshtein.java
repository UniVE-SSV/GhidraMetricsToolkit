package impl.metrics;

import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class Levenshtein implements SimilarityInterface {

    public static final String NAME = "Levenshtein Similarity";

    @Override
    public double compute(Function function1, Function function2) {
        FunctionUtils.SimilarityListingInput listing1 = FunctionUtils.getSimilarityListingInput(function1);
        FunctionUtils.SimilarityListingInput listing2 = FunctionUtils.getSimilarityListingInput(function2);
        LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();
        double dist = distance.apply(listing1, listing2);
        return 1 - dist / Math.max(listing1.length(), listing2.length());
    }

    @Override
    public String toString() {
        return NAME;
    }
}
