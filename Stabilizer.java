import java.math.BigInteger;

public class Stabilizer implements Runnable {
	private Peer peer;

	Stabilizer(Peer peer) {
		this.peer = peer;
	}

	public void run() {
		try {
			System.out.println("stab");
			peer.stabilize();
			// peer.getSuccessor().notifySuccessor(peer.getAddress());
			//peer.getExecutor().execute(new FingerFixer(peer));

			System.out.print("Predecessor: ");
			if (peer.getPredecessor() != null)
				System.out.print(peer.getPredecessor().getId());
			else
				System.out.print("null");

			System.out.print(" ID:" + peer.getId() + " Successor: ");
			if (peer.getSuccessor() != null)
				System.out.println(peer.getSuccessor().getId());
			else
				System.out.println("null");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}