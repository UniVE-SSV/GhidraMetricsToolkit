package impl.metrics;

import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;
import impl.utils.OperandMode;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

public class JaroWinkler implements SimilarityInterface {

    private static final String NAME = "Jaro Winkler Similarity";

    private final OperandMode operandMode;

    public JaroWinkler() {
        this(OperandMode.FULL);
    }

    public JaroWinkler(OperandMode operandMode) {
        this.operandMode = operandMode;
    }

    @Override
    public double compute(Function function1, Function function2) {
        FunctionUtils.SimilarityListingInput listing1 = FunctionUtils.getSimilarityListingInput(function1, operandMode);
        FunctionUtils.SimilarityListingInput listing2 = FunctionUtils.getSimilarityListingInput(function2, operandMode);
        JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
        return jaroWinklerSimilarity.apply(listing1, listing2);
    }

    @Override
    public String toString() {
        return NAME;
    }
}
