//Computes the McCabe cyclomatic complexity of the whole program
//@author Ca' Foscari - Software Security
//@category Metrics

import generic.stl.Pair;
import ghidra.app.script.GhidraScript;
import impl.metrics.McCabe;
import impl.common.ResultInterface;
import impl.utils.CsvExporter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;


public class McCabeScript extends GhidraScript {

    static class ScriptArgs {
        @CommandLine.Option(names = "--csv-export", description = "CSV file path to export result")
        String csvPath;

        @CommandLine.Option(names = "--overall-only", description = "Only compute overall binary complexity")
        boolean overallOnly = false;
    }

    @Override
    protected void run() throws Exception {
        if (currentProgram == null) {
            printerr("no current program");
            return;
        }

        ScriptArgs args = new ScriptArgs();
        CommandLine cmd = new CommandLine(args);
        cmd.parseArgs(getScriptArgs());

        McCabe complexity = new McCabe(currentProgram, args.overallOnly);
        ResultInterface result = complexity.compute();
        printf(result.toString());

        if (args.csvPath != null) {
            try {
                List<Pair<String, String>> out = result.export();
                Pair<String, String> binaryResult = out.getFirst();
                CsvExporter csvExporter = new CsvExporter(args.csvPath, binaryResult.first);
                csvExporter.exportData(binaryResult.second);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
