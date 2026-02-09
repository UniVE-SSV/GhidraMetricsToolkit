package impl.utils;

import generic.stl.Pair;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.symbol.SourceType;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;
import org.apache.commons.text.similarity.SimilarityInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionUtils {

    public static SimilarityListingInput getSimilarityListingInput(Function function) {
        return new SimilarityListingInput(getOpcodeListing(function));
    }

    public static List<String> getOpcodeListing(Function function) {
        List<String> listing = new ArrayList<>();
        for (CodeUnit cb : function.getProgram().getListing().getCodeUnits(function.getBody(), true)) {
            listing.add(cb.toString());
        }
        return listing;
    }

    public static byte[] getFunctionBytes(Function function) {
        Memory memory = function.getProgram().getMemory();
        byte[] functionBytes = new byte[(int) function.getBody().getNumAddresses()];
        try {
            memory.getBytes(function.getEntryPoint(), functionBytes);
        } catch (MemoryAccessException e) {
            throw new RuntimeException(e);
        }
        return functionBytes;
    }

    public static Function getFunctionByName(Program program, String functionName) {
        FunctionIterator functions = program.getFunctionManager().getFunctions(true);
        for (Function f : functions) {
            if (f.getName().equals(functionName))
                return f;
        }
        return null;
    }

    public static void applyNames(Program targetProgram, List<Pair<Function, String>> replacements) {
        int tx = targetProgram.startTransaction("Rename Functions");
        try {
            for (Pair<Function, String> replacement : replacements) {
                replacement.first.setName(replacement.second, SourceType.USER_DEFINED);
            }
        } catch (InvalidInputException | DuplicateNameException e) {
            throw new RuntimeException(e);
        } finally {
            targetProgram.endTransaction(tx, true);
        }
    }

    public static Map<String, Double> getHistogram(Function function) {
        Map<String, Double> histogram = new HashMap<>();
        for (CodeUnit cb : function.getProgram().getListing().getCodeUnits(function.getBody(), true)) {
            String opcode = cb.toString().split(" ")[0];
            Double prev = histogram.putIfAbsent(opcode, 1.0);
            if (prev != null) {
                histogram.put(opcode, prev + 1);
            }
        }
        double count = histogram.values().stream().mapToDouble(a -> a).sum();
        histogram.replaceAll((key, value) -> value / count);
        return histogram;
    }

    public static class SimilarityListingInput implements SimilarityInput<String> {

        private final List<String> listing;

        public SimilarityListingInput(List<String> listing) {
            this.listing = listing;
        }

        @Override
        public String at(int i) {
            return listing.get(i);
        }

        @Override
        public int length() {
            return listing.size();
        }

    }

}
