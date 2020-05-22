import java.io.File;
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
            if(id.compareTo(lBound) >= 0 || id.compareTo(rBound) <= 0){
                return true;
            }

        if(id.compareTo(lBound) >= 0 && id.compareTo(rBound) <= 0){
            return true;
        }

        return false;
    }

    public static void deleteFile(String fileId, String storage, String backup) {
        if (fileId.equals("") || fileId.equals(null)) {
            return;
        }

        File storageDir = new File(storage);
        File backupDir = new File(backup);
        File fileIDDir = new File(backupDir.getPath(), fileId);

        if (backupDir.exists()) {
            if (fileIDDir.delete()) {
                System.out.println("File deleted successfully");
            } else {
                System.out.println("Failed to delete the file");
            }
        }

        // Deletes the backup directory if it's empty after the fileID deletion
        File[] backupDirectory = backupDir.listFiles();
        if (backupDirectory.length == 0) {
            backupDir.delete();
        }

        // File[] storageDirectory = storageDir.listFiles();
        // if (storageDirectory.length == 0) {
        // storageDir.delete();
        // }
    }
}