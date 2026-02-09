package impl.metrics;

import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

public class JaroWinkler implements SimilarityInterface {

    private static final String NAME = "Jaro Winkler Similarity";

    @Override
    public double compute(Function function1, Function function2) {
        FunctionUtils.SimilarityListingInput listing1 = FunctionUtils.getSimilarityListingInput(function1);
        FunctionUtils.SimilarityListingInput listing2 = FunctionUtils.getSimilarityListingInput(function2);
        JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
        return jaroWinklerSimilarity.apply(listing1, listing2);
    }

    @Override
    public String toString() {
        return NAME;
    }
}
