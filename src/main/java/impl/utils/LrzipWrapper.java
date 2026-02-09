package impl.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class LrzipWrapper {

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
        File fConcat = new File(f1.getParent(), String.format("concat_%s_%s.bin", f1.getName(), f2.getName()));
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
            return null;
        }
    }

    private File compress(File f) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(lrzipPath, "--best", "-f", f.getAbsolutePath());
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Compression failed");
        }
        return new File(f.getAbsolutePath() + ".lrz");
    }

    public long measure(File f) throws Exception {
        File compressed = compress(f);
        long compressedLen = compressed.length();
        boolean delete = compressed.delete();
        return compressedLen;
    }

    public long measure(File f1, File f2) throws Exception {
        File concat = concatenate(f1, f2);
        long compressedLen = measure(concat);
        boolean delete = concat.delete();
        return compressedLen;
    }

    public long measure(byte[] bytes) throws Exception {
        File tmp = new File("compress.tmp");
        try (FileOutputStream stream = new FileOutputStream(tmp)) {
            stream.write(bytes);
        }
        long res = measure(tmp);
        tmp.delete();
        return res;
    }

    public long measure(byte[] bytes1, byte[] bytes2) throws Exception {
        byte[] concat = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, concat, 0, bytes1.length);
        System.arraycopy(bytes2, 0, concat, bytes1.length, bytes2.length);
        return measure(concat);
    }

}
