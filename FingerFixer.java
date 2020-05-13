public class FingerFixer implements Runnable {
    private Peer peer;

    public FingerFixer(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        OutsidePeer outsidePeer = null;
        try {
            System.out.println("SIZE:" + this.peer.getFingerTable().getSize());
            for (int i = 0; i < this.peer.getFingerTable().getSize(); i++) {
                System.out.println(i);

                outsidePeer = this.peer.getSuccessor()
                        .findSuccessor(this.peer.getFingerTable().calculateFinger(this.peer.getId(), i));
                System.out.println("111");
                if (outsidePeer != null)
                    this.peer.getFingerTable().updateFingers(outsidePeer.getInetSocketAddress(), i);

                System.out.println("222");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}