package impl.utils;

import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RopGadgetWrapper {

    private static final long PROCESS_TIMEOUT_MINUTES = 5;

    public static HashMap<Long, String> getRops(Program program, Integer depth) throws Exception {
        return getRops(program, depth, TaskMonitor.DUMMY);
    }

    public static HashMap<Long, String> getRops(Program program, Integer depth, TaskMonitor monitor) throws Exception {

        File f = ProjectUtils.exportProgram(program);
        try {
            if (monitor.isCancelled()) {
                throw new CancelledException();
            }
            ProcessBuilder builder = new ProcessBuilder("ROPgadget", "--binary", f.getAbsolutePath(), "--depth", depth.toString());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (monitor.isCancelled()) {
                        process.destroyForcibly();
                        throw new CancelledException();
                    }
                    output.append(line).append(System.lineSeparator());
                }
            }

            if (!process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new Exception("ROPgadget timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new Exception("ROPgadget Error");
            }

            HashMap<Long, String> rops = new HashMap<>();
            Pattern pattern = Pattern.compile("0x([\\dabcdef]+) : (.+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(output);
            while (matcher.find()) {
                long address = Long.parseUnsignedLong(matcher.group(1), 16);
                String gadget = matcher.group(2).replace("nop ; ", "");
                rops.put(address, gadget);
            }

            return rops;
        } finally {
            f.delete();
        }
    }
}
