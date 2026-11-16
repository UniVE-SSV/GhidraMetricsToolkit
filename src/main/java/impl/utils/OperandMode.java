package impl.utils;

/**
 * How the instruction operands take part in the listing-based similarity metrics
 * (Jaccard, Jaro-Winkler, LCS, Levenshtein).
 */
public enum OperandMode {
    /** The full instruction text as Ghidra shows it, including absolute addresses. */
    FULL,
    /** Addresses and scalars with a memory reference are replaced by ADDR, so code that only moved still matches. */
    MASKED,
    /** Only the mnemonic. */
    NONE;

    public static OperandMode fromString(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operand mode " + value + ", expected full, masked or none");
        }
    }
}
