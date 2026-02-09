//Computes the Halstead metrics of a function and the entire program
//@author Ca' Foscari - Software Security
//@category Metrics

import generic.stl.Pair;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import impl.metrics.Halstead;
import impl.utils.CsvExporter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;


public class HalsteadScript extends GhidraScript {

    static class ScriptArgs {
        @CommandLine.Option(names = "--csv-export", description = "CSV file path to export result")
        String csvPath;
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

        Function currentFunction = currentProgram.getFunctionManager().getFunctionAt(currentAddress);
        Halstead halstead = new Halstead(currentProgram, currentFunction);
        Halstead.Result result = (Halstead.Result) halstead.compute();
        printf(result.toString());

        if (args.csvPath != null) {
            try {
                List<Pair<String, String>> out = result.export();
                Pair<String, String> programHalstead = out.getFirst();
                CsvExporter csvExporter = new CsvExporter(args.csvPath, programHalstead.first);
                csvExporter.exportData(programHalstead.second);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
