public class Stabilizer implements Runnable {
	private Peer peer;
	private FingerFixer fingerFixer;

	Stabilizer(Peer peer) {
		this.peer = peer;
		this.fingerFixer = new FingerFixer(peer);
	}

	public void run() {
		try {
			System.out.println("Peer with id: " + this.peer.getId());
			System.out.println("Successor id: " + this.peer.getSuccessor().getId());
			System.out.println("Predecessor id: " + this.peer.getPredecessor().getId());
			this.peer.getFingerTable().print();
			peer.getStorage().print();
			if (peer.getSuccessor() != null) {
				if (peer.getSuccessor().testSuccessor()) {
					if (peer.updateToNextPeer()) {
						System.out.println("NO NEXT SUCCESSOR");
						return;
					} else {

					}
				}
				OutsidePeer newNextPeer = this.peer.getSuccessor().getNextSuccessor();
				if (newNextPeer.getId().compareTo(this.peer.getId()) == 0) {
					this.peer.setNextSuccessor(null);
				} else {
					this.peer.setNextSuccessor(newNextPeer);
				}
				peer.updateTable();
				peer.getSuccessor().notifySuccessor(peer.getAddress(), peer.getSuccessor().getInetSocketAddress());
				peer.stabilize();
				peer.getExecutor().execute(fingerFixer);
			}
		} catch (Exception e) {

		}
	}
}