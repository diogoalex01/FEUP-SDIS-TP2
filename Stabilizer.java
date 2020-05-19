public class Stabilizer implements Runnable {
	private Peer peer;
	private FingerFixer fingerFixer;

	Stabilizer(Peer peer) {
		this.peer = peer;
		this.fingerFixer = new FingerFixer(peer);
	}

	public void run() {
		try {
			peer.getStorage().print();
			if (peer.getSuccessor() != null) {
				if (peer.getSuccessor().testSuccessor()) {
					if (peer.updateToNextPeer()) {
						System.out.println("NO NEXT SUCCESSOR");
						return;
					}
				}
				OutsidePeer newNextPeer = this.peer.getSuccessor().getNextSuccessor();
				if (newNextPeer.getId().compareTo(this.peer.getId()) == 0) {
					this.peer.setNextSuccessor(null);
				} else {
					this.peer.setNextSuccessor(newNextPeer);
				}
				peer.getSuccessor().notifySuccessor(peer.getAddress(), peer.getSuccessor().getInetSocketAddress());
				peer.stabilize();
				peer.getExecutor().execute(fingerFixer);
			}
		} catch (Exception e) {

		}
	}
}