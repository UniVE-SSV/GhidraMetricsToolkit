package impl.metrics;

import generic.stl.Pair;
import ghidra.program.model.listing.*;
import impl.common.MetricInterface;
import impl.common.ResultInterface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class Halstead implements MetricInterface {

    private final Program program;
    private final Function function;

    public Halstead(Program program, Function function) {
        this.program = program;
        this.function = function;
    }

    public Halstead(Program program) {
        this.program = program;
        this.function = null;
    }

    @Override
    public ResultInterface compute() {
        int[] ops = halsteadByProgram();
        int[] fOps = null;
        if (this.function != null) {
            fOps = halsteadByFunction(this.function);
        }
        return new Result(this.program, this.function, ops, fOps);
    }

    public static class Result implements ResultInterface {

        private final Program program;
        private final Function function;
        public List<Pair<String, Double>> programHalstead;
        public List<Pair<String, Double>> functionHalstead;

        private List<Pair<String, Double>> generateMetrics(int[] ops) {
            List<Pair<String, Double>> metrics = new ArrayList<>();
            int n_1 = ops[0];
            metrics.add(new Pair<>("Dist. Operators (n1)", (double) n_1));
            int n_2 = ops[1];
            metrics.add(new Pair<>("Dist. Operands (n2)", (double) n_2));
            int N_1 = ops[2];
            metrics.add(new Pair<>("Tot. Operators (N1)", (double) N_1));
            int N_2 = ops[3];
            metrics.add(new Pair<>("Tot. Operands (N2)", (double) N_2));

            int programVocab = n_1 + n_2;
            metrics.add(new Pair<>("Program Vocabulary (n)", (double) programVocab));
            int programLength = N_1 + N_2;
            metrics.add(new Pair<>("Program Length (N)", (double) programLength));

            double estimatedLength = n_1 * Math.log(n_1) / Math.log(2) + n_2 * Math.log(n_2) / Math.log(2);
            metrics.add(new Pair<>("Estimated Length (~N)", estimatedLength));
            double volume = programLength * Math.log(programVocab) / Math.log(2);
            metrics.add(new Pair<>("Volume (V)", volume));
            double difficulty = (double) n_1 / 2 * N_2 / n_2;
            metrics.add(new Pair<>("Difficulty (D)", difficulty));
            double effort = volume * difficulty;
            metrics.add(new Pair<>("Effort (E)", effort));
            double timeToProgram = effort / 18;
            metrics.add(new Pair<>("Time to Program (T)", timeToProgram));
            double deliveredBugs = Math.pow(effort, (double) 2 / 3) / 3000;
            metrics.add(new Pair<>("Delivered Bugs (B)", deliveredBugs));

            return metrics;
        }

        public Result(Program program, Function function, int[] ops, int[] fOps) {
            this.program = program;
            this.function = function;
            if (ops != null) {
                programHalstead = generateMetrics(ops);
            }
            if (fOps != null) {
                functionHalstead = generateMetrics(fOps);
            }
        }

        @Override
        public List<Pair<String, String>> export() {
            List<Pair<String, String>> exportedData = new ArrayList<>();

            StringBuilder headerStringBuilder = new StringBuilder();
            headerStringBuilder.append("Program,");

            StringBuilder dataStringBuilder = new StringBuilder();
            dataStringBuilder.append(this.program.getName()).append(",");
            for (var elem : this.programHalstead) {
                headerStringBuilder.append(elem.first).append(",");
                dataStringBuilder.append(elem.second).append(",");
            }

            exportedData.add(new Pair<>(headerStringBuilder.toString(), dataStringBuilder.toString()));

            if (this.functionHalstead != null) {
                dataStringBuilder = new StringBuilder();
                dataStringBuilder.append(this.function.getName()).append(",");
                for (var elem : this.functionHalstead) {
                    dataStringBuilder.append(elem.second).append(",");
                }

                exportedData.add(new Pair<>(headerStringBuilder.toString(), dataStringBuilder.toString()));
            }
            return exportedData;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Halstead Metrics [%s]\n", program.getName()));
            builder.append("Program Halstead Metrics:\n");

            builder.append("-".repeat(22)).append("-+-").append("-".repeat(10)).append("\n");
            builder.append(String.format("%22s | %s\n", "Metric", "Value"));
            builder.append("-".repeat(22)).append("-+-").append("-".repeat(10)).append("\n");
            for (var e : programHalstead) {
                builder.append(String.format("%22s | %10.2f\n", e.first, e.second));
            }
            builder.append("-".repeat(22)).append("-+-").append("-".repeat(10)).append("\n");

            if (functionHalstead != null) {
                builder.append("Function Halstead Metrics:\n");
                builder.append("-".repeat(22)).append("-+-").append("-".repeat(10)).append("\n");
                builder.append(String.format("%22s | %s\n", "Metric", "Value"));
                builder.append("-".repeat(22)).append("-+-").append("-".repeat(10)).append("\n");
                for (var e : functionHalstead) {
                    builder.append(String.format("%22s | %10.2f\n", e.first, e.second));
                }
                builder.append("-".repeat(22)).append("-+-").append("-".repeat(10)).append("\n");
            }
            return builder.toString();
        }
    }

    private int[] halsteadByProgram() {
        int[] ops = new int[4];
        FunctionIterator functions = program.getFunctionManager().getFunctions(true);
        for (Function f : functions) {
            int[] fOps = halsteadByFunction(f);
            if (fOps != null) {
                for (int i = 0; i < 4; i++)
                    ops[i] += fOps[i];
            }
        }
        return ops;
    }

    private int[] halsteadByFunction(Function function) {

        if (function.isThunk() || function.isExternal())
            return null;

        ArrayList<String> operands = new ArrayList<>();
        ArrayList<String> operators = new ArrayList<>();

        Listing l = function.getProgram().getListing();
        InstructionIterator it = l.getInstructions(function.getBody(), true);

        while (it.hasNext()) {
            Instruction instr = it.next();
            String operator = instr.getMnemonicString();
            operators.add(operator);
            int numOp = instr.getNumOperands();
            for (int j = 0; j < numOp; j++) {
                Object[] ops = instr.getOpObjects(j);
                for (Object o : ops) {
                    operands.add(o.toString());
                }
            }
        }

        return new int[]{ new HashSet<>(operators).size(), new HashSet<>(operands).size(), operators.size(), operands.size() };
    }

}
