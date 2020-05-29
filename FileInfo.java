import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileInfo {
    private BigInteger id;
    private int replicationDegree;
    private String fileName;

    public FileInfo(File file, int replicationDegree) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String hashInput = file.getName() + file.lastModified() + Files.getOwner(file.toPath());

        this.id = Helper.getFileId(fileName);
        // this.id = toHexString(md.digest(hashInput.getBytes(StandardCharsets.UTF_8)));
        this.setReplicationDegree(replicationDegree);
        this.fileName = file.getName();
    }

    public static String toHexString(byte[] hash) {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    public BigInteger getId() {
        return this.id;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public void setReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
    }
}