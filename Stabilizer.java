public class Stabilizer implements Runnable {
	private Peer peer;

	Stabilizer(Peer peer) {
		this.peer = peer;
	}

	public void run() {
		try {
			// peer.stabilize();
			this.peer.getExecutor().execute(new FingerFixer(peer));
			// peer.checkPredecessor();

			// System.out.print("Pre: ");

			// if (peer.predecessor != null)
			// 	System.out.print(peer.predecessor.id);
			// else
			// 	System.out.print("null");

			// System.out.print(" ID:" + peer.id + " Suc: ");

			// if (peer.successor != null)
			// 	System.out.println(peer.successor.id);
			// else
			// 	System.out.println("null");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}