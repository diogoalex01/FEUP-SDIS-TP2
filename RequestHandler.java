import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
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
            // System.out.println("if do null");
            this.peer.setSuccessor(newPeer);
            this.peer.setPredecessor(newPeer);
            this.peer.getFingerTable().add(this.peer.getSuccessor(), 0);

            Messenger.sendUpdatePosition(this.peer.getAddress().getAddress().getHostAddress(), this.peer.getPort(),
                    this.peer.getAddress().getAddress().getHostAddress(), this.peer.getPort(),
                    newPeer.getInetSocketAddress());
        }
        // New peer is between him and his successor
        else if (Helper.middlePeer(new BigInteger(request[1]), this.peer.getId(), this.peer.getSuccessor().getId())) {
            // System.out.println("if do middle");
            Messenger.sendUpdatePosition(this.peer.getAddress().getAddress().getHostAddress(), this.peer.getPort(),
                    this.peer.getSuccessor().getInetSocketAddress().getAddress().getHostAddress(),
                    this.peer.getSuccessor().getInetSocketAddress().getPort(), newPeer.getInetSocketAddress());
            this.peer.setSuccessor(new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3]))));
        }
        // The position of the new peer isn't known
        else {
            // System.out.println("if do forward");
            Messenger.sendFindSuccessor(new BigInteger(request[1]), request[2], Integer.parseInt(request[3]),
                    this.peer.getSuccessor().getInetSocketAddress());
        }
    }

    private void updatePredecessor(String[] request) throws UnknownHostException, IOException {
        // UPDATEPREDECESSOR <ip_predecessor> <port_predecessor>
        this.peer.setPredecessor(new OutsidePeer(new InetSocketAddress(request[1], Integer.parseInt(request[2]))));
        this.peer.updatePredecessorTable();

    }

    public String sendPredecessor(String[] request) throws UnknownHostException, IOException {
        String message = "PREDECESSOR "
                + this.peer.getPredecessor().getInetSocketAddress().getAddress().getHostAddress() + " "
                + this.peer.getPredecessor().getInetSocketAddress().getPort() + "\n";
        // System.out.println("x" + message);
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
        // System.out.println("Successor id: " + this.peer.getSuccessor().getId());
        // System.out.println("predecessor id: " + this.peer.getPredecessor().getId());
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

    private void nextSuccessor(String[] request, DataOutputStream out) throws IOException {
        String message = "OK " + this.peer.getSuccessor().getInetSocketAddress().getAddress().getHostAddress() + " "
                + this.peer.getSuccessor().getInetSocketAddress().getPort() + "\n";
        out.writeBytes(message);
    }

    private void updateTable(String[] request) {
        // UPDATETABLE key ipaddress port
        BigInteger fileKey = new BigInteger(request[1]);
        OutsidePeer newEntry = new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3])));
        this.peer.getStorage().addFileLocation(fileKey, newEntry);
        if (!Helper.middlePeer(fileKey, this.peer.getPredecessor().getId(), this.peer.getId())) {

            String message = "REMOVETABLE " + fileKey + "\n";
            Messenger.sendMessage(message, this.peer.getSuccessor().getInetSocketAddress());
        }
    }

    private void removeTable(String[] request) {
        // REMOVETABLE key
        this.peer.getStorage().removeFileLocation(new BigInteger(request[1]));
    }

    private String findFile(String[] request) {
        String fileKey = request[1];
        String ipAddress = request[2];
        int port = Integer.parseInt(request[3]);

        if (!this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
            return "NO\n";
        }

        String fileName = this.peer.getBackupDirPath() + "/" + fileKey;
        File file = new File(fileName);
        byte[] body = null;

        try {
            body = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String message = "GIVEFILE " + fileKey + " " + body.length + "\n";
        System.out.println("MANDEI GIVE FILE");
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
        try {
            this.peer.sendMessage(message, body, inetSocketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "SENT " + fileKey + "\n";
    }

    @Override
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            BufferedReader in = null;

            String responseMess = null;
            // while (responseMess == null) {
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            // if (sslSocket.isClosed() || in == null || sslSocket == null)
            // continue;
            responseMess = in.readLine();
            // System.out.println("Reading...." + responseMess + " " +
            // sslSocket.isClosed());
            // }

            String[] request = responseMess.split(" ");

            String response = "\n";
            byte[] file;
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
                    updatePredecessor(request);
                    break;
                case "FINDPREDECESSOR":
                    response = sendPredecessor(request);
                    out.writeBytes(response);
                    out.flush();
                    break;
                case "FINDFILE":
                    response = findFile(request);
                    out.writeBytes(response);
                    out.flush();
                    break;
                case "MARCO":
                    getFinger(request);
                    break;
                case "UPDATEFINGER":
                    updateFinger(request);
                    break;
                case "NEXTSUCCESSOR":
                    nextSuccessor(request, out);
                    break;
                case "FORWARD":
                    file = new byte[Integer.parseInt(request[3])];
                    InputStream inputStream = sslSocket.getInputStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int bytesRead;
                    while ((bytesRead = inputStream.read(file)) != -1) {
                        buffer.write(file, 0, bytesRead);
                    }
                    buffer.flush();
                    file = buffer.toByteArray();
                    response = protocolHandler.forwardHandler(request, file);
                    buffer.close();
                    inputStream.close();
                    break;
                case "BACKUP":
                    file = new byte[Integer.parseInt(request[7])];
                    InputStream inputStreamBackup = sslSocket.getInputStream();
                    ByteArrayOutputStream bufferBackup = new ByteArrayOutputStream();
                    int bytesReadBackup;
                    while ((bytesReadBackup = inputStreamBackup.read(file)) != -1) {
                        bufferBackup.write(file, 0, bytesReadBackup);
                    }
                    bufferBackup.flush();
                    file = bufferBackup.toByteArray();
                    response = protocolHandler.backupHandler(request, file);
                    bufferBackup.close();
                    inputStreamBackup.close();
                    break;
                case "RESTORE":
                    response = protocolHandler.restoreHandler(request);
                    break;
                case "REMOVELOCATION":
                    System.out.println("\nRECEBI REMOVELOCATION\n");
                    response = "OK\n";
                    BigInteger fileKey = new BigInteger(request[1]);
                    String ipAddress = request[2];
                    int port = Integer.parseInt(request[3]);
                    OutsidePeer outsidePeer = this.peer.getSuccessor();
                    this.peer.getStorage().removePeerLocation(fileKey, ipAddress, port);
                    break;
                case "DELETE":
                    response = protocolHandler.deleteHandler(request);
                    out.writeBytes(response);
                    out.flush();
                    break;
                case "REMOVED":
                    System.out.println("remov:" + responseMess);
                    file = new byte[Integer.parseInt(request[4])];
                    System.out.println("\n******\nFile size é:" + file.length);
                    InputStream inputStreamRemoved = sslSocket.getInputStream();
                    ByteArrayOutputStream bufferRemoved = new ByteArrayOutputStream();
                    int bytesReadRemoved = 0;
                    while ((bytesReadRemoved = inputStreamRemoved.read(file)) != -1) {
                        bufferRemoved.write(file, 0, bytesReadRemoved);
                    }
                    bufferRemoved.flush();
                    file = bufferRemoved.toByteArray();
                    System.out.println("byee read: " + file.length);
                    response = protocolHandler.reclaimHandler(request, file);
                    bufferRemoved.close();
                    inputStreamRemoved.close();
                    break;
                case "UPDATETABLE":
                    updateTable(request);
                    out.writeBytes("OK\n");
                    out.flush();
                    break;
                case "REMOVETABLE":
                    removeTable(request);
                    out.writeBytes("OK\n");
                    out.flush();
                    break;
                case "GIVEFILE":
                    System.out.println("RECEBI GIVEFILE");
                    file = new byte[Integer.parseInt(request[2])];
                    InputStream inputStreamFile = sslSocket.getInputStream();
                    ByteArrayOutputStream bufferFile = new ByteArrayOutputStream();
                    int bytesReadFile;
                    while ((bytesReadFile = inputStreamFile.read(file)) != -1) {
                        bufferFile.write(file, 0, bytesReadFile);
                    }
                    bufferFile.flush();
                    file = bufferFile.toByteArray();
                    response = protocolHandler.getFileHandler(request, file);
                    bufferFile.close();
                    inputStreamFile.close();
                    break;
                case "STORED":
                    response = protocolHandler.storedHandler(request);
                    break;
                default:
                    System.out.println(request[0]);
            }
            in.close();
            // out.close();
            // sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}