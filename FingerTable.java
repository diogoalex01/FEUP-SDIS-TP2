import java.math.BigInteger;
import java.util.ArrayList;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class FingerTable {
    private ArrayList<OutsidePeer> table;
    private int numOfEntries;
    // private String localAddress = "127.0.0.1";

    public FingerTable(int numOfEntries, OutsidePeer outsidePeer) {
        table = new ArrayList<>();
        this.numOfEntries = numOfEntries;
        table.forEach(value -> value = outsidePeer);
    }

    public int getSize() {
        return numOfEntries;
    }

    public OutsidePeer getPeer(int index) {
        return table.get(index);
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public void add(OutsidePeer outsidePeer, int index) throws UnknownHostException {
        if (table.size() > index) {
            table.set(index, outsidePeer);
        } else {
            table.add(index, outsidePeer);
        }
    }

    public synchronized void updateFingers(InetSocketAddress inetSocketAddress, int index) {
        OutsidePeer outsidePeer = new OutsidePeer(inetSocketAddress);
        try {
            add(outsidePeer, index);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public BigInteger calculateFinger(BigInteger peerID, int i) {
        return ((peerID.add(new BigInteger("2").pow(i))).mod(new BigInteger("2").pow(this.getSize())));
    }

    public OutsidePeer getNearestPeer(BigInteger fileId) {
        OutsidePeer receiverPeer = table.get(0);

        for (int i = 1; i < table.size(); i++) {
            BigInteger peerId = table.get(i).getId();
            
            if (peerId.compareTo(fileId) == -1 || peerId.compareTo(fileId) == 0) {
                if (peerId.compareTo(fileId) == 0) {
                    receiverPeer = table.get(i);
                    break;
                }

                if (peerId.compareTo(receiverPeer.getId()) == 1) {
                    receiverPeer = table.get(i);
                }
            }
        }

        return receiverPeer;
    }

    public void setAllEntries(OutsidePeer outsidePeer) {
        table.forEach(value -> value = outsidePeer);
    }

    public void print() {
        for (int i = 0; i < table.size(); i++) {
            System.out.println("| " + i + " | " + table.get(i).getId() + " |");
        }
    }
}