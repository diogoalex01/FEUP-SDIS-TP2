import java.math.BigInteger;
import java.net.InetSocketAddress;

public class FingerFixer implements Runnable {
    private Peer peer;

    public FingerFixer(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        try {

            for (int i = 0; i < peer.getFingerTable().getSize(); i++) {
                BigInteger key = peer.getFingerTable().calculateFinger(peer.getId(), i);
                InetSocketAddress entryAddress = peer.getSuccessor().getInetSocketAddress();
                Messenger.sendFindFinger(peer.getAddress(), entryAddress, i, key);
            }

            // this.peer.getFingerTable().print();
        } catch (Exception e) {

        }
    }
}