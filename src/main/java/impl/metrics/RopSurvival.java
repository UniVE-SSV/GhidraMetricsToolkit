package impl.metrics;

import generic.stl.Pair;
import ghidra.program.model.listing.Program;
import impl.common.MetricInterface;
import impl.common.ResultInterface;
import impl.utils.RopGadgetWrapper;

import java.util.*;

public class RopSurvival implements MetricInterface {

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

    private double bagOfGadgetsSimilarity() {
        try {
            HashMap<Long, String> gadgets1 = RopGadgetWrapper.getRops(program1, depth);
            HashMap<Long, String> gadgets2 = RopGadgetWrapper.getRops(program2, depth);

            Set<String> intersection = new HashSet<>(gadgets1.values());
            int size = intersection.size();
            intersection.retainAll(gadgets2.values());

            return (double) intersection.size() / size;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double survivorSimilarity() {
        try {
            HashMap<Long, String> gadgets1 = RopGadgetWrapper.getRops(program1, depth);
            HashMap<Long, String> gadgets2 = RopGadgetWrapper.getRops(program2, depth);

            int len = gadgets1.size();
            gadgets1.entrySet().retainAll(gadgets2.entrySet());

            return (double) gadgets1.size() / len;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResultInterface compute() {
        if (program1.getLanguage().getProcessor() != program2.getLanguage().getProcessor())
            return null;
        return new Result(this.program1, this.program2, bagOfGadgetsSimilarity(), survivorSimilarity());
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
