package impl.common;

import generic.stl.Pair;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public class SimilarityResult implements ResultInterface {

    private final Program program1;
    private final Program program2;
    private final List<FunctionSimilarity> functionSimilarities;
    public Double overallSimilarity;

    public SimilarityResult(Program program1, Program program2) {
        this.program1 = program1;
        this.program2 = program2;

        functionSimilarities = new ArrayList<>();
    }

    public void addFunctionSimilarity(Function f1, Function f2, Double similarity) {
        functionSimilarities.add(new FunctionSimilarity(f1, f2, similarity));
    }

    public void addFunctionSimilarity(Function f1, Function f2, Double similarity, Double weight) {
        functionSimilarities.add(new FunctionSimilarity(f1, f2, similarity, weight));
    }

    public void setOverallSimilarity(double overallSimilarity) {
        this.overallSimilarity = overallSimilarity;
    }

    public List<Object[]> getFunctionSimilarities() {
        List<Object[]> s = new ArrayList<>();
        for (FunctionSimilarity m : functionSimilarities) {
//            String similarity = (m.similarity != null) ? String.format("%.2f", m.similarity) : "----";
            String function1 = (m.f1 != null) ? m.f1.getName() : "―";
            String function2 = (m.f2 != null) ? m.f2.getName() : "―";
            s.add(new Object[]{m.similarity, m.weight, function1, function2});
        }
        return s;
    }

    public void sortBySimilarity() {
        functionSimilarities.sort(Comparator.comparing(m -> m.similarity, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    @Override
    public List<Pair<String, String>> export() {

        List<Pair<String, String>> exportedData = new ArrayList<>();

        Pair<String, String> overallSimilarity = new Pair<>("Program 1,Program 2,Similarity", this.program1.getName() + "," + this.program2.getName() + "," + this.overallSimilarity);
        exportedData.add(overallSimilarity);

        StringBuilder functionSimilarityBuilder = new StringBuilder();
        for (var m : functionSimilarities) {
            String similarity = (m.similarity != null) ? String.format("%.2f", m.similarity) : "----";
            String function1 = (m.f1 != null) ? m.f1.getName() : "--";
            String function2 = (m.f2 != null) ? m.f2.getName() : "--";
            functionSimilarityBuilder.append(function1).append(",").append(function2).append(",").append(similarity);
        }
        Pair<String, String> functionSimilarity = new Pair<>("Function 1,Function 2,Similarity", functionSimilarityBuilder.toString());
        exportedData.add(functionSimilarity);

        return exportedData;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        if (this.overallSimilarity != null) {
            output.append(String.format("Overall Similarity [%s, %s]: %f\n", program1.getName(), program2.getName(), overallSimilarity));
        }

        if (!this.functionSimilarities.isEmpty()) {
            boolean hasWeights = functionSimilarities.stream().map(a -> a.weight).anyMatch(Objects::nonNull);
            String headerFormat = (hasWeights) ? "Sim  | Weight | %-26s | %-26s\n" : "Sim  | %-26s | %-26s\n";
            String dataFormat = (hasWeights) ? "%s | %6s | %-26s | %-26s \n" : "%s | %-26s | %-26s \n";
            output.append("Function matching:\n");
            output.append(String.format(headerFormat, program1.getName(), program2.getName()));
            output.append("--------------------------------------------------------------\n");
            for (FunctionSimilarity m : functionSimilarities) {
                String similarity = (m.similarity != null) ? String.format("%.2f", m.similarity) : "----";
                String weight = (m.weight != null) ? String.format("%.2f", m.weight) : "----";
                String function1 = (m.f1 != null) ? m.f1.getName() : "--";
                String function2 = (m.f2 != null) ? m.f2.getName() : "--";
                if (hasWeights)
                    output.append(String.format(dataFormat, similarity, weight, function1, function2));
                else
                    output.append(String.format(dataFormat, similarity, function1, function2));
            }
            output.append("--------------------------------------------------------------\n");
        }
        return output.toString();
    }

    public static class FunctionSimilarity {
        public Function f1;
        public Function f2;
        public Double similarity;
        public Double weight;

        public FunctionSimilarity(Function f1, Function f2, Double similarity, Double weight) {
            this.f1 = f1;
            this.f2 = f2;
            this.similarity = similarity;
            this.weight = weight;
        }

        public FunctionSimilarity(Function f1, Function f2, Double similarity) {
            this(f1, f2, similarity, null);
        }
    }

}