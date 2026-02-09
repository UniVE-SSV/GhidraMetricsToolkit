import generic.stl.Pair;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Program;
import impl.metrics.Jaccard;
import impl.common.Similarity;
import impl.common.SimilarityResult;
import impl.utils.CsvExporter;
import picocli.CommandLine;
import impl.utils.ProjectUtils;

import java.io.IOException;
import java.util.List;

public class JaccardScript extends GhidraScript {

    static class ScriptArgs {
        @CommandLine.Parameters(index = "0", description = "The program to compare to")
        String programName;

        @CommandLine.Option(names = "--csv-export", description = "CSV file path to export result")
        String csvPath;

        @CommandLine.Option(names = "--exclusive", description = "Use exclusive function matching strategy")
        boolean exclusive;

        @CommandLine.Option(names = "--symmetric", description = "Use symmetric similarity")
        boolean symmetric;

        @CommandLine.Option(names = "--weighted", description = "Use function weights")
        boolean weighted;
    }

    @Override
    protected void run() throws Exception {
        if (currentProgram == null) {
            printerr("~ current program");
            return;
        }

        Program program2;
        String csvPath = null;
        boolean exclusive = false;
        boolean symmetric = false;
        boolean weighted = false;

        if (isRunningHeadless()) {
            ScriptArgs args = new ScriptArgs();
            CommandLine cmd = new CommandLine(args);
            cmd.parseArgs(getScriptArgs());
            program2 = ProjectUtils.getProgramByName(state.getProject(), args.programName);
            csvPath = args.csvPath;
            exclusive = args.exclusive;
            symmetric = args.symmetric;
            weighted = args.weighted;
        } else {
            program2 = askProgram("Pick second program");
        }

        Similarity jaccardSimilarity = new Similarity(currentProgram, program2, new Jaccard());
        SimilarityResult result = jaccardSimilarity.getOverallSimilarity(exclusive, weighted, symmetric);
        result.sortBySimilarity();
        print(result.toString());

        if (csvPath != null) {
            try {
                List<Pair<String, String>> out = result.export();
                Pair<String, String> binaryResult = out.getFirst();
                CsvExporter csvExporter = new CsvExporter(csvPath, binaryResult.first);
                csvExporter.exportData(binaryResult.second);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
