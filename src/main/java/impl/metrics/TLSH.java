package impl.metrics;

import app.keve.ktlsh.TLSHUtil;
import app.keve.ktlsh.spi.KProvider;
import ghidra.program.model.listing.Function;
import impl.common.SimilarityInterface;
import impl.utils.FunctionUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

public class TLSH implements SimilarityInterface{

    private static final String NAME = "TLSH";
    private static final Provider PROVIDER = new KProvider();

    static {
        Security.addProvider(PROVIDER);
    }

    @Override
    public double compute(Function function1, Function function2) {
        byte[] bytes1 = FunctionUtils.getFunctionBytes(function1);
        byte[] bytes2 = FunctionUtils.getFunctionBytes(function2);

        MessageDigest tlshDigest = null;
        try {
            tlshDigest = MessageDigest.getInstance("TLSH", PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] hash1 = tlshDigest.digest(bytes1);
        byte[] hash2 = tlshDigest.digest(bytes2);

        return Math.clamp(1 - (double) TLSHUtil.score(hash1, hash2, false) / 300, 0., 1.);
    }

    @Override
    public String toString() {
        return NAME;
    }
}
