package impl.utils;

import ghidra.program.model.listing.Program;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RopGadgetWrapper {
    public static HashMap<Long, String> getRops(Program program, Integer depth) throws Exception {

        File f = ProjectUtils.exportProgram(program);
        ProcessBuilder builder = new ProcessBuilder("ROPgadget", "--binary", f.getAbsolutePath(), "--depth", depth.toString());
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        HashMap<Long, String> rops = new HashMap<>();
        Pattern pattern = Pattern.compile("0x([\\dabcdef]+) : (.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(output);
        while (matcher.find()) {
            long address = Long.parseUnsignedLong(matcher.group(1), 16);
            String gadget = matcher.group(2).replace("nop ; ", "");
            rops.put(address, gadget);
        }

        if (f.exists()) {
            f.delete();
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("ROPgadget Error");
        }

        return rops;
    }
}
