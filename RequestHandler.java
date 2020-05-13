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

    private String findSuccessor(String[] request) throws UnknownHostException, IOException {
        String response = new String();
        System.out.println("parse find successor");

        // SUCCESSOR <file_key> <ip_address> <port>
        if (this.peer.getSuccessor() == null) {
            this.peer.setSuccessor(new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3]))));
            response = "OK " + this.peer.getAddress().getHostString() + " " + this.peer.getAddress().getPort() + "\n";
        } else if (this.peer.getSuccessor().middlePeer(new BigInteger(request[1]), this.peer.getId())) {
            System.out.println("--3");
            response = this.peer.getSuccessor().getId() + " ";
            response += this.peer.getSuccessor().getInetSocketAddress().getHostString() + " ";
            response += this.peer.getSuccessor().getInetSocketAddress().getPort() + "\n";
            System.out.println("--3.1");
        } else {
            System.out.println("--4");
            OutsidePeer newSuccessor = this.peer.getSuccessor().findSuccessor(new BigInteger(request[1]));
            response = newSuccessor.getId() + " ";
            response += newSuccessor.getInetSocketAddress().getHostString() + " ";
            response += newSuccessor.getInetSocketAddress().getPort() + "\n";
            System.out.println("--5");
        }
        System.out.println(response);
        return response;
    }

    @Override
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            String[] request = in.readLine().split(" ");
            String response = "";
            System.out.println(request[0]);

            switch (request[0]) {
                case "SUCCESSOR":
                    response = findSuccessor(request);
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
                     //response = protocolHandler.reclaimHandler(request);
                    break;
                case "GIVECHUNK":
                    response = protocolHandler.GiveChunkHandler(request);
                   break;
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