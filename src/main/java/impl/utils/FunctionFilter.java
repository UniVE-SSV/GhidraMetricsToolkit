package impl.utils;

import ghidra.program.model.listing.Function;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Selects the functions of a program that take part in a similarity comparison,
 * by name. For example, the symbols of a statically linked library
 * (nm --defined-only libfoo.a) can be excluded, so that the comparison only
 * covers the program's own code.
 */
public class FunctionFilter {

    public static final Predicate<Function> ALL = function -> true;

    /**
     * @param includeFile file with the names of the functions to keep (one per line), or null to keep all
     * @param excludeFile file with the names of the functions to drop (one per line), or null to drop none
     */
    public static Predicate<Function> fromFiles(String includeFile, String excludeFile) throws IOException {
        Set<String> include = (includeFile != null) ? readNames(includeFile) : null;
        Set<String> exclude = (excludeFile != null) ? readNames(excludeFile) : Set.of();
        return function -> (include == null || include.contains(function.getName())) && !exclude.contains(function.getName());
    }

    private static Set<String> readNames(String file) throws IOException {
        Set<String> names = new HashSet<>();
        for (String line : Files.readAllLines(Path.of(file))) {
            String name = line.strip();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}
