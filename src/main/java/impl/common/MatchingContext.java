package impl.common;

import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;

import java.util.List;

public record MatchingContext(
        Program program1,
        Program program2,
        List<Function> functions1,
        List<Function> functions2,
        List<Double> weights1,
        List<Double> weights2,
        double[][] similarity) {
}
