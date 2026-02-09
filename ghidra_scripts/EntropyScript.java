//Computes the entropy of the program
//@author Ca' Foscari - Software Security
//@category Metrics

import generic.stl.Pair;
import ghidra.app.script.GhidraScript;
import impl.metrics.Entropy;
import impl.common.ResultInterface;
import impl.utils.CsvExporter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;


public class EntropyScript extends GhidraScript {

    static class ScriptArgs {
        @CommandLine.Option(names = "--csv-export", description = "CSV file path to export result")
        String csvPath;

        @CommandLine.Option(names = "--base", description = "The logarithm base for the entropy computation (default: 2)")
        Integer base;
    }

    @Override
    protected void run() {
        if (currentProgram == null) {
            printerr("no current program");
            return;
        }

        ScriptArgs args = new ScriptArgs();
        CommandLine cmd = new CommandLine(args);
        cmd.parseArgs(getScriptArgs());

        Entropy entropy;
        if (args.base != null) {
            entropy = new Entropy(currentProgram, args.base);
        } else {
            entropy = new Entropy(currentProgram);
        }

        ResultInterface result = entropy.compute();
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
