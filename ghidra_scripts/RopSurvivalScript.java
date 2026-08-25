//Computes the ROP Survival metrics between two programs
//@author Ca' Foscari - Software Security
//@category Metrics

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Program;
import impl.common.Pair;
import impl.metrics.RopSurvival;
import impl.utils.CsvExporter;
import picocli.CommandLine;
import impl.utils.ProjectUtils;

import java.io.IOException;
import java.util.List;

public class RopSurvivalScript extends GhidraScript {

    static class ScriptArgs {
        @CommandLine.Parameters(index = "0", description = "The program to compare to")
        String programName;

        @CommandLine.Option(names = "--csv-export", description = "CSV file path to export result")
        String csvPath;

        @CommandLine.Option(names = "--depth", description = "Gadget search depth in bytes (default: 10)")
        Integer depth = 10;
    }

    private int depth = 10;

    @Override
    protected void run() throws Exception {

        if (currentProgram == null) {
            printerr("Program not found");
            return;
        }

        String csvPath = null;

        Program program2;

        if (isRunningHeadless()) {
            ScriptArgs args = new ScriptArgs();
            CommandLine cmd = new CommandLine(args);
            cmd.parseArgs(getScriptArgs());

            program2 = ProjectUtils.getProgramByName(state.getProject(), args.programName);
            csvPath = args.csvPath;
            depth = args.depth;
        } else {
            program2 = askProgram("Pick second program");
        }

        if (program2 == null) {
            printerr("Second program not found");
            return;
        }

        RopSurvival ropSurvival = new RopSurvival(currentProgram, program2, depth);
        RopSurvival.Result result = (RopSurvival.Result) ropSurvival.compute();

        if (result == null) {
            printerr("The programs have different processors. Aborting");
            return;
        }
        printf(result.toString());

        if (csvPath != null) {
            try {
                List<Pair<String, String>> out = result.export();
                Pair<String, String> ropResult = out.get(0);
                CsvExporter csvExporter = new CsvExporter(csvPath, ropResult.first);
                csvExporter.exportData(ropResult.second);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
