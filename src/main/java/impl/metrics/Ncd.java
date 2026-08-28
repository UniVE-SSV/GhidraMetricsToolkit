package impl.metrics;

import ghidra.framework.Application;
import ghidra.framework.OSFileNotFoundException;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;
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
        return computeBinarySimilarity(program1, program2, TaskMonitor.DUMMY);
    }

    public double computeBinarySimilarity(Program program1, Program program2, TaskMonitor monitor) throws Exception {

        File f1 = null;
        File f2 = null;
        try {
            monitor.setMessage("Exporting " + program1.getName() + "...");
            f1 = ProjectUtils.exportProgram(program1);
            monitor.setMessage("Exporting " + program2.getName() + "...");
            f2 = ProjectUtils.exportProgram(program2);

            LrzipWrapper lrzipWrapper = new LrzipWrapper(lrzipPath);
            monitor.setMessage("Compressing " + program1.getName() + "...");
            long size1 = lrzipWrapper.measure(f1, monitor);
            monitor.setMessage("Compressing " + program2.getName() + "...");
            long size2 = lrzipWrapper.measure(f2, monitor);
            monitor.setMessage("Compressing concatenation...");
            long sizeConcat = lrzipWrapper.measure(f1, f2, monitor);

            double value = 1 - (double) (sizeConcat - Math.min(size1, size2)) / Math.max(size1, size2);
            return clamp01(value);
        } finally {
            if (f1 != null) {
                f1.delete();
            }
            if (f2 != null) {
                f2.delete();
            }
        }
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

            return clamp01(1 - (double) (sizeConcat - Math.min(size1, size2)) / Math.max(size1, size2));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return NAME;
    }

    // Java 17 compatible clamp (Math.clamp requires Java 21)
    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
