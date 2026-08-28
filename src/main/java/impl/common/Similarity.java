package impl.common;

import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import impl.metrics.Ncd;

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
    private final TaskMonitor monitor;

    public Similarity(Program program1, Program program2, SimilarityInterface metric) throws CancelledException {
        this(program1, program2, metric, TaskMonitor.DUMMY);
    }

    public Similarity(Program program1, Program program2, SimilarityInterface metric, TaskMonitor monitor) throws CancelledException {

        this.program1 = program1;
        this.program2 = program2;
        this.monitor = monitor;

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
        double normalizer1 = Math.max(totalWeight, 1.0);
        for (int i = 0; i < weights1.size(); i++) {
            weights1.set(i, weights1.get(i) / normalizer1);
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
        double normalizer2 = Math.max(totalWeight, 1.0);
        for (int i = 0; i < weights2.size(); i++) {
            weights2.set(i, weights2.get(i) / normalizer2);
        }

        similarity = new double[functions1.size()][functions2.size()];
        this.metric = metric;

        computePairwiseSimilarity();
    }

    private void computePairwiseSimilarity() throws CancelledException {
        monitor.setMaximum(Math.max(functions1.size(), 1));
        monitor.setMessage("Computing pairwise function similarity...");
        for (int i = 0; i < functions1.size(); i++) {
            if (monitor.isCancelled()) {
                throw new CancelledException();
            }
            monitor.setProgress(i);
            for (int j = 0; j < functions2.size(); j++) {
                similarity[i][j] = metric.compute(functions1.get(i), functions2.get(j));
            }
        }
    }

    private MatchingContext context() {
        return new MatchingContext(program1, program2, functions1, functions2, weights1, weights2, similarity);
    }

    public SimilarityResult getOverallSimilarity(boolean exclusive, boolean weighted, boolean symmetric) throws CancelledException {
        return getOverallSimilarity(exclusive, weighted, symmetric, TaskMonitor.DUMMY);
    }

    public SimilarityResult getOverallSimilarity(boolean exclusive, boolean weighted, boolean symmetric, TaskMonitor monitor) throws CancelledException {
        MatchingStrategy strategy = (exclusive) ? new ExclusiveMatchingStrategy() : new NonExclusiveMatchingStrategy();
        SimilarityResult result = strategy.match(context(), weighted, symmetric);

        if (monitor.isCancelled()) {
            throw new CancelledException();
        }

        // Override overall similarity result for NCD metric
        if (metric instanceof Ncd) {
            try {
                monitor.setMessage("Computing binary NCD similarity...");
                double overallSimilarity = ((Ncd) metric).computeBinarySimilarity(program1, program2, monitor);
                result.setOverallSimilarity(overallSimilarity);
            } catch (CancelledException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
