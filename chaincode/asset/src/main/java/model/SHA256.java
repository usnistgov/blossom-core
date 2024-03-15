package model;

import org.bouncycastle.util.encoders.Hex;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SHA256 {

    public static final byte[] EMPTY_HASH_BYTES = hashBytesToBytes(new byte[]{});

    public static byte[] hashBytesToBytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new ChaincodeException(e);
        }
    }

    public static String hashStrToStr(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new ChaincodeException(e);
        }
    }
}
