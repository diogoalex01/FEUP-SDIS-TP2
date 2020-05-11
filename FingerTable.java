import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;

public class FingerTable {
    private ArrayList<InetSocketAddress> table;
    private int numOfEntries;
    // private String localAddress = "127.0.0.1";

    public FingerTable(int numOfEntries) {
        table = new ArrayList<>();
        this.numOfEntries = numOfEntries;
    }

    public int getSize() {
        return table.size();
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public void add(String ipAddress, int port) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(ipAddress);
        InetSocketAddress address = new InetSocketAddress(inetAddress, port);
        table.add(address);
    }

    public synchronized void updateFingers(int i, InetSocketAddress value) {
        if (i > 0 && i <= numOfEntries) {
            updateIthFinger(i, value);
        }
        // else if (i == -1) {
        // deleteSuccessor();
        // } else if (i == -2) {
        // deleteCertainFinger(value);
        // } else if (i == -3) {
        // fillSuccessor();
        // }
    }

    private void updateIthFinger(int i, InetSocketAddress value) {
        // table.put(i, value);

        // // if the updated one is successor, notify the new successor
        // if (i == 1 && value != null && !value.equals(localAddress)) {
        // // notify(value);
        // }
    }

    // Utility Methods
    public static BigInteger getId(String ip, int port) {
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