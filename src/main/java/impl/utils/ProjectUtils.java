package impl.utils;

import ghidra.app.util.exporter.ExporterException;
import ghidra.app.util.exporter.OriginalFileExporter;
import ghidra.framework.model.DomainFile;
import ghidra.framework.model.DomainFolder;
import ghidra.framework.model.DomainObject;
import ghidra.framework.model.Project;
import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;
import ghidra.util.exception.VersionException;
import ghidra.util.task.TaskMonitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectUtils {

    public static File exportProgram(Program program) throws CancelledException, IOException, VersionException, ExporterException {
        File f = new File("export_" + program.getName());
        OriginalFileExporter exporter = new OriginalFileExporter();
        exporter.export(f, program.getDomainFile().getDomainObject(DomainFile.DEFAULT_VERSION, false, false, TaskMonitor.DUMMY), null, null);
        return f;
    }

    public static Program getProgramByName(Project project, String programName) {
        if (!programName.startsWith("/")) {
            programName = "/" + programName;
        }
        List<DomainFile> programs = getPrograms(project);
        for (var p : programs) {
            if (p.getPathname().equals(programName)) {
                return getProgramFromDomainFile(p);
            }
        }
        return null;
    }

    public static List<DomainFile> getPrograms(Project project) {
        List<DomainFile> programs = new ArrayList<>();
        getProgramList(project.getProjectData().getRootFolder(), programs);
        return programs;
    }

    private static void getProgramList(DomainFolder folder, List<DomainFile> programFiles) {
        for (DomainFile file : folder.getFiles()) {
            if (Program.class.isAssignableFrom(file.getDomainObjectClass())) {
                programFiles.add(file);
            }
        }
        for (DomainFolder subFolder : folder.getFolders()) {
            getProgramList(subFolder, programFiles);
        }
    }

    public static Program getProgramFromDomainFile(DomainFile domainFile) {

        Program program = null;

        try {
            DomainObject domainObject = domainFile.getDomainObject(DomainFile.DEFAULT_VERSION, false, false, TaskMonitor.DUMMY);
            if (domainObject instanceof Program) {
                program = (Program) domainObject;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CancelledException | VersionException e) {
            throw new RuntimeException(e);
        }

        return program;
    }
}
