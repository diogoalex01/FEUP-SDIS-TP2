import java.math.BigInteger;
import java.security.MessageDigest;

public class Helper {
    static int numberOfNodes = 8;

    public static int getNumberOfNodes() {
        return numberOfNodes;
    }

    public static BigInteger getFileId(String fileName) {
        return getSHA1(fileName).mod(new BigInteger("2").pow(numberOfNodes));
    }

    public static BigInteger getPeerId(String ip, int port) {
        return getSHA1(ip + ":" + port).mod(new BigInteger("2").pow(numberOfNodes));
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

    public static boolean middlePeer(BigInteger id, BigInteger lBound, BigInteger rBound) {
        if (lBound.compareTo(rBound) > 0)
            return (id.compareTo(lBound) >= 0) || (id.compareTo(rBound) <= 0);

        return (id.compareTo(lBound) >= 0) && (id.compareTo(rBound) <= 0);
    }
}