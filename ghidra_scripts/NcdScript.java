//Computes the NCD Similarity between two programs
//@author Ca' Foscari - Software Security
//@category Metrics

import generic.stl.Pair;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Program;
import impl.metrics.Ncd;
import impl.common.Similarity;
import impl.common.SimilarityResult;
import impl.utils.CsvExporter;
import picocli.CommandLine;
import impl.utils.ProjectUtils;

import java.io.IOException;
import java.util.List;


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
    }

    @Override
    protected void run() throws Exception {

        String os = System.getProperty("os.name").toLowerCase();

        if (!os.contains("linux")) {
            println("NCD is only available on linux");
            return;
        }

        Program p2;
        String csvPath = null;
        boolean binaryOnly = false;
        boolean exclusive = false;
        boolean symmetric = false;
        boolean weighted = false;

        if (isRunningHeadless()) {
            ScriptArgs args = new ScriptArgs();
            CommandLine cmd = new CommandLine(args);
            cmd.parseArgs(getScriptArgs());

            p2 = ProjectUtils.getProgramByName(state.getProject(), args.programName);
            csvPath = args.csvPath;
            binaryOnly = args.binaryOnly;
            exclusive = args.exclusive;
            symmetric = args.symmetric;
            weighted = args.weighted;

        } else {
            p2 = askProgram("Pick second program");
        }

        if (p2 == null) {
            printerr("second program not found");
            return;
        }

        if (!binaryOnly) {
            Similarity ncdSimilarity = new Similarity(currentProgram, p2, new Ncd());
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
                    Pair<String, String> binaryResult = out.getFirst();
                    CsvExporter csvExporter = new CsvExporter(csvPath, binaryResult.first);
                    csvExporter.exportData(binaryResult.second);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            Ncd ncd = new Ncd();
            double res = ncd.computeBinarySimilarity(currentProgram, p2);

            if (csvPath != null) {
                CsvExporter csvExporter = new CsvExporter(csvPath, "Program 1,Program 2,Similarity");
                csvExporter.exportData(currentProgram.getName() + "," + p2.getName() + "," + res);
            }
        }
    }

}
