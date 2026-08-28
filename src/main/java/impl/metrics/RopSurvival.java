package impl.metrics;

import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import impl.common.MetricInterface;
import impl.common.Pair;
import impl.common.ResultInterface;
import impl.utils.RopGadgetWrapper;

import java.util.*;

public class RopSurvival implements MetricInterface<RopSurvival.Result> {

    private final Program program1;
    private final Program program2;
    private final int depth;

    public RopSurvival(Program program1, Program program2, int depth) {
        this.program1 = program1;
        this.program2 = program2;
        this.depth = depth;
    }

    public RopSurvival(Program program1, Program program2) {
        this(program1, program2, 10);
    }

    @Override
    public Result compute() {
        try {
            return compute(TaskMonitor.DUMMY);
        } catch (CancelledException e) {
            throw new RuntimeException(e);
        }
    }

    public Result compute(TaskMonitor monitor) throws CancelledException {
        if (!program1.getLanguage().getProcessor().equals(program2.getLanguage().getProcessor()))
            return null;

        // The gadget sets are extracted once and reused for both variants
        monitor.setMaximum(2);
        monitor.setMessage("Extracting gadgets from " + program1.getName() + "...");
        HashMap<Long, String> gadgets1 = extractRops(program1, monitor);
        monitor.setProgress(1);
        monitor.setMessage("Extracting gadgets from " + program2.getName() + "...");
        HashMap<Long, String> gadgets2 = extractRops(program2, monitor);
        monitor.setProgress(2);

        monitor.setMessage("Comparing gadgets...");
        double bag = bagOfGadgetsSimilarity(gadgets1, gadgets2);
        double survivor = survivorSimilarity(gadgets1, gadgets2);
        return new Result(this.program1, this.program2, bag, survivor);
    }

    private HashMap<Long, String> extractRops(Program program, TaskMonitor monitor) throws CancelledException {
        try {
            return RopGadgetWrapper.getRops(program, depth, monitor);
        } catch (CancelledException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double bagOfGadgetsSimilarity(HashMap<Long, String> gadgets1, HashMap<Long, String> gadgets2) {
        Set<String> intersection = new HashSet<>(gadgets1.values());
        int size = intersection.size();
        if (size == 0) {
            return 0.0;
        }
        intersection.retainAll(gadgets2.values());

        return (double) intersection.size() / size;
    }

    private double survivorSimilarity(HashMap<Long, String> gadgets1, HashMap<Long, String> gadgets2) {
        int len = gadgets1.size();
        if (len == 0) {
            return 0.0;
        }
        gadgets1.entrySet().retainAll(gadgets2.entrySet());

        return (double) gadgets1.size() / len;
    }

    public static class Result implements ResultInterface {

        public final double bagOfGadgets;
        public final double survivor;
        private final Program program1;
        private final Program program2;

        public Result(Program program1, Program program2, double bagOfGadgets, double survivor) {
            this.program1 = program1;
            this.program2 = program2;
            this.bagOfGadgets = bagOfGadgets;
            this.survivor = survivor;
        }

        @Override
        public List<Pair<String, String>> export() {
            List<Pair<String, String>> exportedData = new ArrayList<>();

            String data = program1.getName() + "," +
                    program2.getName() + "," +
                    bagOfGadgets + "," +
                    survivor;
            exportedData.add(new Pair<>("Program 1,Program 2,BagOfGadgets,Survivor", data));
            return exportedData;
        }

        @Override
        public String toString() {
            return String.format("ROP Survival [%s, %s]:\nBag of Gadgets: %.2f\nSurvivor: %.2f\n", program1.getName(), program2.getName(), bagOfGadgets, survivor);
        }
    }
}
