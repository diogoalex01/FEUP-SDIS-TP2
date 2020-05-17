import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;

class RequestHandler implements Runnable {
    private Peer peer;
    private ProtocolHandler protocolHandler;
    private SSLSocket sslSocket;

    public RequestHandler(Peer peer, SSLSocket sslSocket) {
        this.peer = peer;
        this.sslSocket = sslSocket;
        protocolHandler = new ProtocolHandler(peer);
    }

    private void findSuccessor(String[] request) throws UnknownHostException, IOException {
        OutsidePeer newPeer = new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3])));
        // FINDSUCCESSOR <peer_key> <ip_address> <port>
        // Second peer to join
        if (this.peer.getSuccessor() == null) {
            System.out.println("if do null");
            this.peer.setSuccessor(newPeer);
            this.peer.setPredecessor(newPeer);
            this.peer.getFingerTable().add(this.peer.getSuccessor(), 0);

            Messenger.sendUpdatePosition(this.peer.getAddress().getAddress().getHostAddress(), this.peer.getPort(),
                    this.peer.getAddress().getAddress().getHostAddress(), this.peer.getPort(),
                    newPeer.getInetSocketAddress());
        }
        // New peer is between him and his successor
        else if (Helper.middlePeer(new BigInteger(request[1]), this.peer.getId(), this.peer.getSuccessor().getId())) {
            System.out.println("if do middle");
            Messenger.sendUpdatePosition(this.peer.getAddress().getAddress().getHostAddress(), this.peer.getPort(),
                    this.peer.getSuccessor().getInetSocketAddress().getAddress().getHostAddress(),
                    this.peer.getSuccessor().getInetSocketAddress().getPort(), newPeer.getInetSocketAddress());
            this.peer.setSuccessor(new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3]))));
        }
        // The position of the new peer isn't known
        else {
            System.out.println("if do forward");
            Messenger.sendFindSuccessor(new BigInteger(request[1]), request[2], Integer.parseInt(request[3]),
                    this.peer.getSuccessor().getInetSocketAddress());
        }
    }

    private void updatePredecessor(String[] request) throws UnknownHostException, IOException {
        // UPDATEPREDECESSOR <ip_predecessor> <port_predecessor>
        this.peer.setPredecessor(new OutsidePeer(new InetSocketAddress(request[1], Integer.parseInt(request[2]))));
    }

    public String sendPredecessor(String[] request) throws UnknownHostException, IOException {
        String message = "PREDECESSOR "
                + this.peer.getPredecessor().getInetSocketAddress().getAddress().getHostAddress() + " "
                + this.peer.getPredecessor().getInetSocketAddress().getPort() + "\n";
        System.out.println("x" + message);
        // InetSocketAddress socket = new InetSocketAddress(request[1],
        // Integer.parseInt(request[2]));
        // SSLSocket sslSocketPre = Messenger.sendMessage(message, socket);
        // sslSocketPre.close();
        return message;
    }

    private void updatePosition(String[] request) throws UnknownHostException, IOException {
        // UPDATEPREDECESSOR <ip_predecessor> <port_predecessor>
        String predecessorIp = request[1];
        int predecessorPort = Integer.parseInt(request[2]);
        String successorIp = request[3];
        int successorPort = Integer.parseInt(request[4]);
        this.peer.setPredecessor(new OutsidePeer(new InetSocketAddress(predecessorIp, predecessorPort)));
        this.peer.setSuccessor(new OutsidePeer(new InetSocketAddress(successorIp, successorPort)));
        this.peer.getSuccessor().notifySuccessor(this.peer.getAddress(),
                this.peer.getSuccessor().getInetSocketAddress());
        System.out.println("Successor id: " + this.peer.getSuccessor().getId());
        System.out.println("predecessor id: " + this.peer.getPredecessor().getId());
    }

    private void getFinger(String[] request) {
        // "MARCO " + index + " " + ipAddress.getHostName() + " " + ipAddress.getPort()
        // + " " + key;
        if (Helper.middlePeer(new BigInteger(request[4]), this.peer.getPredecessor().getId(), this.peer.getId())
                || this.peer.getId().compareTo(new BigInteger(request[4])) == 0) {
            Messenger.sendUpdateFinger(this.peer.getAddress(),
                    new InetSocketAddress(request[2], Integer.parseInt(request[3])), Integer.parseInt(request[1]));
        } else {
            Messenger.sendFindFinger(new InetSocketAddress(request[2], Integer.parseInt(request[3])),
                    this.peer.getSuccessor().getInetSocketAddress(), Integer.parseInt(request[1]),
                    new BigInteger(request[4]));
        }
    }

    private void updateFinger(String[] request) {
        this.peer.getFingerTable().updateFingers(new InetSocketAddress(request[1], Integer.parseInt(request[2])),
                Integer.parseInt(request[3]));
    }

    @Override
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            BufferedReader in = null;

            String responseMess = null;
            while (responseMess == null) {
                in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                if (sslSocket.isClosed() || in == null || sslSocket == null)
                    continue;
                responseMess = in.readLine();
                System.out.println("Reading...." + responseMess + " " + sslSocket.isClosed());
                in.close();
            }

            String[] request = responseMess.split(" ");

            String response = "";
            // System.out.println("received: " + request[0] + " " + request[1] + " " +
            // request[3]);

            switch (request[0]) {
                case "FINDSUCCESSOR":
                    findSuccessor(request);
                    break;
                case "UPDATEPOSITION":
                    updatePosition(request);
                    break;
                case "UPDATEPREDECESSOR":
                    System.out.println("RECEBI DO PRED\n");
                    updatePredecessor(request);
                    break;
                case "FINDPREDECESSOR":
                    System.out.println("RECEBI DO PRED2\n");
                    response = sendPredecessor(request);
                    out.writeBytes(response);
                    break;
                case "MARCO":
                    getFinger(request);
                    break;
                case "UPDATEFINGER":
                    updateFinger(request);
                    break;
                case "FORWARD":
                    // response = forwardHandler(request);
                    break;
                case "BACKUP":
                    response = protocolHandler.backupHandler(request);
                    break;
                case "RESTORE":
                    response = protocolHandler.restoreHandler(request);
                    break;
                case "DELETE":
                    response = protocolHandler.deleteHandler(request);
                    break;
                case "RECLAIM":
                    // response = protocolHandler.reclaimHandler(request);
                    break;
                case "GIVECHUNK":
                    response = protocolHandler.GiveChunkHandler(request);
                    break;
                default:
                    System.out.println(request[0]);
            }
            // in.close();
            out.close();
            System.out.println("---10");
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}