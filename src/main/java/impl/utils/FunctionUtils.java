package impl.utils;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.scalar.Scalar;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.SourceType;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;
import impl.common.Pair;
import org.apache.commons.text.similarity.SimilarityInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionUtils {

    public static SimilarityListingInput getSimilarityListingInput(Function function) {
        return getSimilarityListingInput(function, OperandMode.FULL);
    }

    public static SimilarityListingInput getSimilarityListingInput(Function function, OperandMode operandMode) {
        return new SimilarityListingInput(getOpcodeListing(function, operandMode));
    }

    public static List<String> getOpcodeListing(Function function) {
        return getOpcodeListing(function, OperandMode.FULL);
    }

    public static List<String> getOpcodeListing(Function function, OperandMode operandMode) {
        List<String> listing = new ArrayList<>();
        for (CodeUnit cb : function.getProgram().getListing().getCodeUnits(function.getBody(), true)) {
            listing.add(codeUnitString(cb, operandMode));
        }
        return listing;
    }

    private static String codeUnitString(CodeUnit codeUnit, OperandMode operandMode) {
        switch (operandMode) {
            case NONE:
                return codeUnit.toString().split(" ")[0];
            case MASKED:
                return (codeUnit instanceof Instruction) ? maskedInstructionString((Instruction) codeUnit) : codeUnit.toString();
            default:
                return codeUnit.toString();
        }
    }

    // Same layout as InstructionDB.toString(), but operand elements that are addresses
    // (or scalars of an operand with a memory reference) are replaced by ADDR. Stack
    // offsets, other constants and registers stay.
    private static String maskedInstructionString(Instruction instruction) {
        StringBuilder builder = new StringBuilder(instruction.getMnemonicString());
        int n = instruction.getNumOperands();
        String separator = instruction.getSeparator(0);
        if (separator != null || n != 0) {
            builder.append(' ');
        }
        if (separator != null) {
            builder.append(separator);
        }
        for (int i = 0; i < n; i++) {
            builder.append(maskedOperandString(instruction, i));
            separator = instruction.getSeparator(i + 1);
            if (separator != null) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    private static String maskedOperandString(Instruction instruction, int opIndex) {
        List<Object> elements = instruction.getDefaultOperandRepresentationList(opIndex);
        if (elements == null) {
            return instruction.getDefaultOperandRepresentation(opIndex);
        }
        boolean memoryReference = false;
        for (Reference reference : instruction.getOperandReferences(opIndex)) {
            memoryReference |= reference.isMemoryReference();
        }
        StringBuilder builder = new StringBuilder();
        for (Object element : elements) {
            if (element instanceof Address || (memoryReference && element instanceof Scalar)) {
                builder.append("ADDR");
            } else {
                builder.append(element);
            }
        }
        return builder.toString();
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
                if (replacement.first == null) {
                    continue;
                }
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
