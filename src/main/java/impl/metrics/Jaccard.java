package impl.metrics;

import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;
import impl.utils.OperandMode;
import org.apache.commons.text.similarity.JaccardSimilarity;

public class Jaccard implements SimilarityInterface {

    private static final String NAME = "Jaccard Index";

    private final OperandMode operandMode;

    public Jaccard() {
        this(OperandMode.FULL);
    }

    public Jaccard(OperandMode operandMode) {
        this.operandMode = operandMode;
    }

    @Override
    public double compute(Function function1, Function function2) {
        FunctionUtils.SimilarityListingInput listing1 = FunctionUtils.getSimilarityListingInput(function1, operandMode);
        FunctionUtils.SimilarityListingInput listing2 = FunctionUtils.getSimilarityListingInput(function2, operandMode);
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        return jaccardSimilarity.apply(listing1, listing2);
    }

    @Override
    public String toString() {
        return NAME;
    }
}
