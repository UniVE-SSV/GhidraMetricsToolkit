package impl.metrics;

import ghidra.framework.Application;
import ghidra.framework.OSFileNotFoundException;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;
import impl.utils.LrzipWrapper;
import impl.utils.ProjectUtils;

import java.io.File;


public class Ncd implements SimilarityInterface {

    private static final String NAME = "Normalized Compression Distance";
    private final String lrzipPath;

    public Ncd() {
        try {
            lrzipPath = Application.getOSFile("GhidraMetricsToolkit", "lrzip").getPath();
        } catch (OSFileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public double computeBinarySimilarity(Program program1, Program program2) throws Exception {

        File f1 = ProjectUtils.exportProgram(program1);
        File f2 = ProjectUtils.exportProgram(program2);

        LrzipWrapper lrzipWrapper = new LrzipWrapper(lrzipPath);
        long size1 = lrzipWrapper.measure(f1);
        long size2 = lrzipWrapper.measure(f2);
        long sizeConcat = lrzipWrapper.measure(f1, f2);

        f1.delete();
        f2.delete();

        double value = 1 - (double) (sizeConcat - Math.min(size1, size2)) / Math.max(size1, size2);
        return Math.clamp(value, 0, 1);
    }

    @Override
    public double compute(Function function1, Function function2) {
        byte[] f1Bytes = FunctionUtils.getFunctionBytes(function1);
        byte[] f2Bytes = FunctionUtils.getFunctionBytes(function2);

        LrzipWrapper lrzipWrapper = new LrzipWrapper(lrzipPath);
        try {
            long size1 = lrzipWrapper.measure(f1Bytes);
            long size2 = lrzipWrapper.measure(f2Bytes);
            long sizeConcat = lrzipWrapper.measure(f1Bytes, f2Bytes);

            return Math.clamp(1 - (double) (sizeConcat - Math.min(size1, size2)) / Math.max(size1, size2), 0, 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return NAME;
    }
}
