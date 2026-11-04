//Computes the NCD Similarity between two programs
//@author Ca' Foscari - Software Security
//@category Metrics

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;
import impl.common.Pair;
import impl.metrics.Ncd;
import impl.common.Similarity;
import impl.common.SimilarityResult;
import impl.utils.CsvExporter;
import impl.utils.FunctionFilter;
import picocli.CommandLine;
import impl.utils.ProjectUtils;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;


public class NcdScript extends GhidraScript {

    static class ScriptArgs {
        @CommandLine.Parameters(index = "0", description = "The program to compare to")
        String programName;

        @CommandLine.Option(names = "--csv-export", description = "CSV file path to export result")
        String csvPath;

        @CommandLine.Option(names = "--binary-only", description = "Only compute overall binary similarity")
        boolean binaryOnly;

        @CommandLine.Option(names = "--exclusive", description = "Use exclusive function matching strategy")
        boolean exclusive;

        @CommandLine.Option(names = "--symmetric", description = "Use symmetric similarity")
        boolean symmetric;

        @CommandLine.Option(names = "--weighted", description = "Use function weights")
        boolean weighted;

        @CommandLine.Option(names = "--functions1", description = "File with the names of the functions of the first program to compare (one per line)")
        String functions1;

        @CommandLine.Option(names = "--functions2", description = "File with the names of the functions of the second program to compare (one per line)")
        String functions2;

        @CommandLine.Option(names = "--exclude-functions", description = "File with function names to leave out in both programs (one per line)")
        String excludeFunctions;
    }

    @Override
    protected void run() throws Exception {

        String os = System.getProperty("os.name").toLowerCase();

        if (!os.contains("linux")) {
            println("NCD is only available on linux");
            return;
        }

        Program program2;
        String csvPath = null;
        boolean binaryOnly = false;
        boolean exclusive = false;
        boolean symmetric = false;
        boolean weighted = false;
        Predicate<Function> filter1 = FunctionFilter.ALL;
        Predicate<Function> filter2 = FunctionFilter.ALL;

        if (isRunningHeadless()) {
            ScriptArgs args = new ScriptArgs();
            CommandLine cmd = new CommandLine(args);
            cmd.parseArgs(getScriptArgs());

            program2 = ProjectUtils.getProgramByName(state.getProject(), args.programName);
            csvPath = args.csvPath;
            binaryOnly = args.binaryOnly;
            exclusive = args.exclusive;
            symmetric = args.symmetric;
            weighted = args.weighted;
            filter1 = FunctionFilter.fromFiles(args.functions1, args.excludeFunctions);
            filter2 = FunctionFilter.fromFiles(args.functions2, args.excludeFunctions);

        } else {
            program2 = askProgram("Pick second program");
        }

        if (program2 == null) {
            printerr("Second program not found");
            return;
        }

        if (!binaryOnly) {
            Similarity ncdSimilarity = new Similarity(currentProgram, program2, new Ncd(), filter1, filter2, TaskMonitor.DUMMY);
            SimilarityResult result = ncdSimilarity.getOverallSimilarity(exclusive, weighted, symmetric);

            if (result == null) {
                printerr("The programs have different processors. Aborting");
                return;
            }

            result.sortBySimilarity();
            print(result.toString());

            if (csvPath != null) {
                try {
                    List<Pair<String, String>> out = result.export();
                    Pair<String, String> binaryResult = out.get(0);
                    CsvExporter csvExporter = new CsvExporter(csvPath, binaryResult.first);
                    csvExporter.exportData(binaryResult.second);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            Ncd ncd = new Ncd();
            double res = ncd.computeBinarySimilarity(currentProgram, program2);

            if (csvPath != null) {
                CsvExporter csvExporter = new CsvExporter(csvPath, "Program 1,Program 2,Similarity");
                csvExporter.exportData(currentProgram.getName() + "," + program2.getName() + "," + res);
            }
        }
    }

}
