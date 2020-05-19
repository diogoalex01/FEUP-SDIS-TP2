import java.math.BigInteger;
import java.net.InetSocketAddress;

public class FingerFixer implements Runnable {
    private Peer peer;

    public FingerFixer(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        // System.out.println("Fixing fingers...");
        // OutsidePeer outsidePeer = null;
        try {
            // System.out.println("SIZE:" + peer.getFingerTable().getSize());

            for (int i = 0; i < peer.getFingerTable().getSize(); i++) {
                // System.out.println(i);
                // outsidePeer = peer.getSuccessor().findFinger(
                // peer.getFingerTable().calculateFinger(peer.getId(), i), peer.getAddress());
                // TODO: por num thread com timeout e tratar timeout
                BigInteger key = peer.getFingerTable().calculateFinger(peer.getId(), i);
                InetSocketAddress entryAddress = peer.getSuccessor().getInetSocketAddress();
                Messenger.sendFindFinger(peer.getAddress(), entryAddress, i, key);
                // System.out.println(" | " + peer.getFingerTable().getPeer(i).getId());
            }

            // this.peer.getFingerTable().print();
        } catch (Exception e) {

        }
    }
}