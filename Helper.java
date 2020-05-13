import java.math.BigInteger;
import java.security.MessageDigest;

public class Helper {

    public static BigInteger getFileId(String fileName) {
        return getSHA1(fileName);
    }

    public static BigInteger getPeerId(String ip, int port) {
        return getSHA1(ip + ":" + port);
    }

    public static BigInteger getSHA1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encoded = digest.digest(input.getBytes());
            return new BigInteger(1, encoded);
        } catch (Exception e) {
            e.printStackTrace();
            return new BigInteger(1, "0".getBytes());
        }
    }
}