package impl.utils;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CsvExporter {

    private final File outputFile;
    private final String header;

    public CsvExporter(String outputPath, String header) {
        try {
            Path path = Paths.get(outputPath);
            this.outputFile = path.toFile();
            this.header = header;
        } catch (InvalidPathException e) {
            throw new RuntimeException("Invalid CSV path provided");
        }
    }

    public void exportData(String data) throws IOException {
        boolean isNewFile = !outputFile.exists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile, true))) {
            if (isNewFile) {
                pw.println(header);
            }
            pw.println(data);
        }
    }
}
