package impl.utils;

import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;


public class LrzipWrapper {

    private static final long PROCESS_TIMEOUT_MINUTES = 5;

    private final String lrzipPath;

    public LrzipWrapper(String binaryPath) {
        lrzipPath = binaryPath;

        File lrzipFile = new File(lrzipPath);
        if (lrzipFile.exists() && !lrzipFile.canExecute()) {
            boolean success = lrzipFile.setExecutable(true);
            if (!success) {
                System.err.println("Failed to set executable permissions for " + lrzipFile.getAbsolutePath());
            }
        }
    }

    private File concatenate(File f1, File f2) {
        File fConcat = null;
        try {
            fConcat = File.createTempFile("gmt_concat_", ".bin");
        } catch (IOException e) {
            return null;
        }
        try (FileOutputStream outputStream = new FileOutputStream(fConcat);
             FileInputStream inputStream1 = new FileInputStream(f1);
             FileInputStream inputStream2 = new FileInputStream(f2)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream1.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            while ((bytesRead = inputStream2.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return fConcat;

        } catch (IOException e) {
            fConcat.delete();
            return null;
        }
    }

    private File compress(File f, TaskMonitor monitor) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(lrzipPath, "--best", "-f", f.getAbsolutePath());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        // Drain the output so the process cannot block on a full pipe, and check
        // for cancellation so the user can abort a long-running compression.
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (monitor.isCancelled()) {
                    process.destroyForcibly();
                    throw new CancelledException();
                }
            }
        }
        if (monitor.isCancelled()) {
            process.destroyForcibly();
            throw new CancelledException();
        }
        if (!process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new Exception("Compression timed out");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new Exception("Compression failed");
        }
        return new File(f.getAbsolutePath() + ".lrz");
    }

    public long measure(File f) throws Exception {
        return measure(f, TaskMonitor.DUMMY);
    }

    public long measure(File f, TaskMonitor monitor) throws Exception {
        File compressed = compress(f, monitor);
        try {
            return compressed.length();
        } finally {
            compressed.delete();
        }
    }

    public long measure(File f1, File f2) throws Exception {
        return measure(f1, f2, TaskMonitor.DUMMY);
    }

    public long measure(File f1, File f2, TaskMonitor monitor) throws Exception {
        File concat = concatenate(f1, f2);
        if (concat == null) {
            throw new IOException("Failed to concatenate the input files");
        }
        try {
            return measure(concat, monitor);
        } finally {
            concat.delete();
        }
    }

    public long measure(byte[] bytes) throws Exception {
        return measure(bytes, TaskMonitor.DUMMY);
    }

    public long measure(byte[] bytes, TaskMonitor monitor) throws Exception {
        File tmp = File.createTempFile("gmt_compress_", ".bin");
        try (FileOutputStream stream = new FileOutputStream(tmp)) {
            stream.write(bytes);
        }
        try {
            return measure(tmp, monitor);
        } finally {
            tmp.delete();
        }
    }

    public long measure(byte[] bytes1, byte[] bytes2) throws Exception {
        return measure(bytes1, bytes2, TaskMonitor.DUMMY);
    }

    public long measure(byte[] bytes1, byte[] bytes2, TaskMonitor monitor) throws Exception {
        byte[] concat = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, concat, 0, bytes1.length);
        System.arraycopy(bytes2, 0, concat, bytes1.length, bytes2.length);
        return measure(concat, monitor);
    }

}
