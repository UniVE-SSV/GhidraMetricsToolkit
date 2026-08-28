package impl.common;

import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;

import java.util.List;

/**
 * One-to-many matching: every function of program 1 is matched to its most
 * similar function in program 2 (a function of program 2 can be matched by
 * several functions of program 1).
 */
public class NonExclusiveMatchingStrategy implements MatchingStrategy {

    @Override
    public SimilarityResult match(MatchingContext context, boolean weighted, boolean symmetric) {
        Program program1 = context.program1();
        Program program2 = context.program2();
        List<Function> functions1 = context.functions1();
        List<Function> functions2 = context.functions2();
        List<Double> weights1 = context.weights1();
        List<Double> weights2 = context.weights2();
        double[][] similarity = context.similarity();

        SimilarityResult result = new SimilarityResult(program1, program2);

        if (functions1.isEmpty() || functions2.isEmpty()) {
            result.setOverallSimilarity(0.0);
            return result;
        }

        double overallSimilarity = 0;
        for (int i = 0; i < functions1.size(); i++) {
            double maxSimilarity = 0;
            int maxIndex = -1;
            for (int j = 0; j < functions2.size(); j++) {
                if (similarity[i][j] >= maxSimilarity) {
                    maxSimilarity = similarity[i][j];
                    maxIndex = j;
                }
            }
            if (weighted) {
                result.addFunctionSimilarity(functions1.get(i), functions2.get(maxIndex), maxSimilarity, weights1.get(i));
                overallSimilarity += maxSimilarity * weights1.get(i);
            } else {
                result.addFunctionSimilarity(functions1.get(i), functions2.get(maxIndex), maxSimilarity);
                overallSimilarity += maxSimilarity;
            }
        }
        if (!weighted) {
            overallSimilarity /= functions1.size();
        }
        if (symmetric) {
            double inverseSimilarity = 0;
            for (int j = 0; j < functions2.size(); j++) {
                double maxSimilarity = 0;
                for (int i = 0; i < functions1.size(); i++) {
                    maxSimilarity = Math.max(maxSimilarity, similarity[i][j]);
                }
                inverseSimilarity += maxSimilarity * ((weighted) ? weights2.get(j) : 1);
            }
            if (!weighted) {
                inverseSimilarity /= functions2.size();
            }
            overallSimilarity = (overallSimilarity + inverseSimilarity) / 2;
        }
        result.setOverallSimilarity(overallSimilarity);
        return result;
    }
}
