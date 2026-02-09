package impl.common;

import ghidra.program.model.listing.Function;

public interface SimilarityInterface {
    double compute(Function function1, Function function2);
}
