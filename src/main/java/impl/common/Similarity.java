package impl.common;

import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import impl.metrics.Ncd;
import impl.utils.HungarianAlgorithm;

import java.util.ArrayList;
import java.util.List;

public class Similarity {

    private final double[][] similarity;
    private final List<Double> weights1;
    private final List<Double> weights2;
    private final List<Function> functions1;
    private final List<Function> functions2;
    private final Program program1;
    private final Program program2;
    private final SimilarityInterface metric;

    public Similarity(Program program1, Program program2, SimilarityInterface metric) {

        this.program1 = program1;
        this.program2 = program2;

        functions1 = new ArrayList<>();
        functions2 = new ArrayList<>();

        weights1 = new ArrayList<>();
        weights2 = new ArrayList<>();

        // Get functions of program1 and their size
        long totalWeight = 0;
        for (Function f1 : program1.getFunctionManager().getFunctions(true)) {
            if (f1.isExternal() || f1.isThunk()) continue;

            functions1.add(f1);
            long size = f1.getBody().getNumAddresses();
            weights1.add((double) size);
            totalWeight += size;
        }

        // Normalize the size of the functions of program1
        for (int i = 0; i < weights1.size(); i++) {
            weights1.set(i, weights1.get(i) / totalWeight);
        }

        // Get functions of program2 and their size
        totalWeight = 0;
        for (Function f2 : program2.getFunctionManager().getFunctions(true)) {
            if (f2.isExternal() || f2.isThunk()) continue;

            functions2.add(f2);
            long size = f2.getBody().getNumAddresses();
            weights2.add((double) size);
            totalWeight += size;
        }

        // Normalize the size of the functions of program2
        for (int i = 0; i < weights2.size(); i++) {
            weights2.set(i, weights2.get(i) / totalWeight);
        }

        similarity = new double[functions1.size()][functions2.size()];
        this.metric = metric;

        computePairwiseSimilarity();
    }

    private void computePairwiseSimilarity() {
        for (int i = 0; i < functions1.size(); i++) {
            for (int j = 0; j < functions2.size(); j++) {
                similarity[i][j] = metric.compute(functions1.get(i), functions2.get(j));
            }
        }
    }

    private SimilarityResult nonExclusiveMatching(boolean weighted, boolean symmetric) {

        SimilarityResult result = new SimilarityResult(program1, program2);

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

    private SimilarityResult exclusiveMatching(boolean weighted, boolean symmetric) {

        SimilarityResult result = new SimilarityResult(program1, program2);

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
            overallSimilarity = (symmetric) ? (2 * overallSimilarity) / (functions1.size() + functions2.size()) : overallSimilarity / functions1.size();
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

    // TODO Try other exclusive weighted method
    public SimilarityResult getOverallSimilarity(boolean exclusive, boolean weighted, boolean symmetric) {
        SimilarityResult result = (exclusive) ? exclusiveMatching(weighted, symmetric) : nonExclusiveMatching(weighted, symmetric);

        // Override overall similarity result for NCD metric
        if (metric instanceof Ncd) {
            try {
                double overallSimilarity = ((Ncd) metric).computeBinarySimilarity(program1, program2);
                result.setOverallSimilarity(overallSimilarity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
