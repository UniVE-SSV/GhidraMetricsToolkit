package impl.common;

import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import impl.utils.HungarianAlgorithm;

import java.util.List;

/**
 * One-to-one matching: each function of a program is matched to at most one
 * function of the other program, maximizing the total similarity (Hungarian
 * algorithm). Functions left without a match are reported with a null partner.
 */
public class ExclusiveMatchingStrategy implements MatchingStrategy {

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
        if (!weighted) {
            int[] matches = HungarianAlgorithm.compute(similarity);
            for (int i = 0; i < matches.length; i++) {
                if (i < similarity.length) {
                    if (matches[i] >= 0 && matches[i] < similarity[0].length) {
                        result.addFunctionSimilarity(functions1.get(i), functions2.get(matches[i]), similarity[i][matches[i]]);
                        overallSimilarity += similarity[i][matches[i]];
                    } else {
                        result.addFunctionSimilarity(functions1.get(i), null, null);
                    }
                } else {
                    result.addFunctionSimilarity(null, functions2.get(matches[i]), null);
                }
            }
            overallSimilarity /= functions1.size();
            if (symmetric) {
                // Compute the exclusive matching in the inverse direction (P2 -> P1)
                // and average the two scores.
                double[][] transposed = new double[similarity[0].length][similarity.length];
                for (int i = 0; i < similarity.length; i++) {
                    for (int j = 0; j < similarity[0].length; j++) {
                        transposed[j][i] = similarity[i][j];
                    }
                }
                int[] inverseMatches = HungarianAlgorithm.compute(transposed);
                double inverseSimilarity = 0;
                for (int j = 0; j < similarity[0].length; j++) {
                    if (inverseMatches[j] >= 0 && inverseMatches[j] < similarity.length) {
                        inverseSimilarity += similarity[inverseMatches[j]][j];
                    }
                }
                overallSimilarity = (overallSimilarity + inverseSimilarity / functions2.size()) / 2;
            }
            result.setOverallSimilarity(overallSimilarity);
        } else {
            double[][] weightedSimilarity = new double[similarity.length][similarity[0].length];
            for (int i = 0; i < similarity.length; i++) {
                for (int j = 0; j < similarity[0].length; j++) {
                    weightedSimilarity[i][j] = similarity[i][j] * weights1.get(i);
                }
            }
            int[] matches = HungarianAlgorithm.compute(weightedSimilarity);
            for (int i = 0; i < matches.length; i++) {
                if (i < similarity.length) {
                    if (matches[i] >= 0 && matches[i] < similarity[0].length) {
                        result.addFunctionSimilarity(functions1.get(i), functions2.get(matches[i]), similarity[i][matches[i]], weights1.get(i));
                        overallSimilarity += weightedSimilarity[i][matches[i]];
                    } else {
                        result.addFunctionSimilarity(functions1.get(i), null, null, weights1.get(i));
                    }
                } else {
                    result.addFunctionSimilarity(null, functions2.get(matches[i]), null);
                }
            }

            if (symmetric) {
                double inverseSimilarity = 0;
                double[][] inverseWeightedSimilarity = new double[similarity.length][similarity[0].length];
                for (int i = 0; i < similarity.length; i++) {
                    for (int j = 0; j < similarity[0].length; j++) {
                        inverseWeightedSimilarity[i][j] = similarity[i][j] * weights2.get(j);
                    }
                }
                int[] inverseMatches = HungarianAlgorithm.compute(inverseWeightedSimilarity);
                for (int i = 0; i < similarity.length; i++) {
                    if (inverseMatches[i] >= 0 && inverseMatches[i] < similarity[0].length)
                        inverseSimilarity += inverseWeightedSimilarity[i][inverseMatches[i]];
                }

                overallSimilarity = (overallSimilarity + inverseSimilarity) / 2;
            }
            result.setOverallSimilarity(overallSimilarity);
        }
        return result;
    }
}
