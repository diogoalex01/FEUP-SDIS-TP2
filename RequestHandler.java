import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.math.BigInteger;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;

class RequestHandler implements Runnable {
    private Peer peer;
    private SSLSocket sslSocket;

    public RequestHandler(Peer peer, SSLSocket sslSocket) {
        this.peer = peer;
        this.sslSocket = sslSocket;
    }

    private String findSuccessor(String[] request) throws UnknownHostException, IOException {
        String response = new String();
        if (this.peer.getSuccessor().middlePeer(new BigInteger(request[1]), this.peer.getId())) {
            response = this.peer.getSuccessor().getId() + " ";
            response += this.peer.getSuccessor().getInetSocketAddress().getAddress().getHostAddress() + " ";
            response += this.peer.getSuccessor().getInetSocketAddress().getPort();
            // TODO: update successor peer
        } else {
            OutsidePeer newSuccessor = this.peer.getSuccessor().findSuccessor(new BigInteger(request[1]));
            response = newSuccessor.getId() + " ";
            response += newSuccessor.getInetSocketAddress().getAddress().getHostAddress() + " ";
            response += newSuccessor.getInetSocketAddress().getPort();
        }

        return response;
    }

    @Override
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

            String[] request = in.readLine().split(" ");
            String response = "";

            switch (request[0]) {
                case "SUCCESSOR":
                    response = findSuccessor(request);
                    // case "BACKUP":
                    // response = peer.backup(request);
                    // break;
                    // case "RESTORE":
                    // response = peer.restore(request);
                    // break;
                    // case "DELETE":
                    // response = peer.delete(request);
                    // break;
                    // case "RECLAIM":
                    // response = peer.reclaim(request);
                    // break;
            }

            System.out.println(response);
            out.writeBytes(response);

            in.close();
            out.close();
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}