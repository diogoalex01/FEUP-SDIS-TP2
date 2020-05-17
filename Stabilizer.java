public class Stabilizer implements Runnable {
	private Peer peer;
	private FingerFixer fingerFixer;

	Stabilizer(Peer peer) {
		this.peer = peer;
		this.fingerFixer = new FingerFixer(peer);
	}

	public void run() {
		try {
			// System.out.println("stab");
			peer.stabilize();
			if (peer.getSuccessor() != null) {
				peer.getSuccessor().notifySuccessor(peer.getAddress(), peer.getSuccessor().getInetSocketAddress());
			}
			peer.getExecutor().execute(fingerFixer);

			// System.out.print("Predecessor: ");
			// if (peer.getPredecessor() != null)
			// 	System.out.print(peer.getPredecessor().getId());
			// else
			// 	System.out.print("null");

			// System.out.print(" ID:" + peer.getId() + " Successor: ");
			// if (peer.getSuccessor() != null)
			// 	System.out.println(peer.getSuccessor().getId());
			// else
			// 	System.out.println("null");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}