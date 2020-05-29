import java.math.BigInteger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ProtocolHandler {
    private Peer peer;

    public ProtocolHandler(Peer peer) {
        this.peer = peer;
    }

    public String forwardHandler(String[] request, byte[] body) throws UnknownHostException {
        // FORWARD <file_key> <rep_degree> <body>
        BigInteger fileKey = new BigInteger(request[1]);
        int replicationDegree = Integer.parseInt(request[2]);

        OutsidePeer outsidePeer = this.peer.getSuccessor();

        if (Helper.middlePeer(fileKey, peer.getPredecessor().getId(), peer.getId())) {
            this.peer.getStorage().initializeFileLocation(fileKey);
            String message = "BACKUP " + this.peer.getAddress().getAddress().getHostAddress() + " "
                    + this.peer.getAddress().getPort() + " "
                    + this.peer.getSuccessor().getInetSocketAddress().getAddress().getHostAddress() + " "
                    + this.peer.getSuccessor().getInetSocketAddress().getPort() + " " + fileKey + " "
                    + replicationDegree + " " + body.length + "\n";
            try {
                this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // FORWARD <file_key> <rep_degree> <body_length>
            String message = "FORWARD " + fileKey + " " + replicationDegree + " " + body.length + "\n";
            try {
                this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "OK \n";
    }

    public String backupHandler(String[] request, byte[] body) {
        // BACKUP <ip_address> <port> <Successor ip_address> <successor port> <file_key>
        // <rep_degree> <body>
        String ipAddress = request[1];
        int port = Integer.parseInt(request[2]);
        String succesorIpAddress = request[3];
        int successorPort = Integer.parseInt(request[4]);
        int myPort = this.peer.getAddress().getPort();
        String myIpAddress = this.peer.getAddress().getAddress().getHostAddress();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
        InetSocketAddress successorInetSocketAddress = new InetSocketAddress(succesorIpAddress, successorPort);

        try {
            String fileKey = request[5];
            int replicationDegree = Integer.parseInt(request[6]);
            // TODO: ver se o file foi enviado por mim
            System.out.println("!!!MANDARAM ME O BACKUP \n\n");
            if (replicationDegree < 1 && replicationDegree != -1) {

                return "OK \n";
            }

            if (ipAddress.equals(myIpAddress) && myPort == port) {
                System.out.println("There aren't enough peers to backup the file");

                return "OK \n";
            }

            OutsidePeer outsidePeer = this.peer.getSuccessor();
            String message = "BACKUP " + ipAddress + " " + port + " " + succesorIpAddress + " " + successorPort + " "
                    + fileKey + " " + replicationDegree + " " + body.length + "\n";

            if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {

                if (replicationDegree > 1 || replicationDegree == -1) {

                    if (replicationDegree != -1) {
                        replicationDegree--;
                    }

                    // TODO enviar stored?
                    System.out.println("ja tenho o file vou reencaminhar");
                    String message1 = "BACKUP " + ipAddress + " " + port + " " + succesorIpAddress + " " + successorPort
                            + " " + fileKey + " " + replicationDegree + " " + body.length + "\n";

                    if (!outsidePeer.testSuccessor()) {
                        Peer.sendMessage(message1, body, outsidePeer.getInetSocketAddress());
                    } else {
                        OutsidePeer otherSuccessor = this.peer.getNextSuccessor();
                        Peer.sendMessage(message1, body, otherSuccessor.getInetSocketAddress());
                    }

                }
                /*
                 * System.out.println("mandei o 1 stored"); Messenger.sendStored(new
                 * BigInteger(fileKey), myIpAddress, myPort, inetSocketAddress);
                 * 
                 * 
                 * System.out.println("mandei o 2 stored : " + succesorIpAddress + "porta: " +
                 * successorPort); Messenger.sendStored(new BigInteger(fileKey), myIpAddress,
                 * myPort, successorInetSocketAddress);
                 */
                return "OK \n";
            }
            int space = this.peer.getStorage().spaceOccupied(this.peer.getBackupDirPath())
                    + Integer.parseInt(request[7]);
            int availableSpace = this.peer.getStorage().getAvailableSpace();

            if (this.peer.getStorage().hasAskedForFile(new BigInteger(fileKey))
                    || (space > availableSpace && availableSpace != -1)) {
                if (!outsidePeer.testSuccessor()) {
                    Peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
                } else {
                    OutsidePeer otherSuccessor = this.peer.getNextSuccessor();
                    Peer.sendMessage(message, body, otherSuccessor.getInetSocketAddress());
                }

                return "OK \n";
            }

            String fileDirName = this.peer.getBackupDirPath();
            // Store file
            final Path DirPath = Paths.get(fileDirName);

            if (Files.notExists(DirPath)) {
                Files.createDirectories(DirPath);
            }

            final Path fileDirPath = Paths.get(fileDirName + "/" + fileKey);

            Files.newOutputStream(fileDirPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE).write(body);
            System.out.println("TAMANHO FICHEIRO:" + body.length);

            for (int i = 0; i < request.length; i++) {
                System.out.println(request[i]);
            }

            System.out.println("Stored!");
            replicationDegree--;

            OutsidePeer peer = new OutsidePeer(inetSocketAddress);
            this.peer.getStorage().addStoredFile(new BigInteger(fileKey));
            System.out.println("mandei o 1 stored");
            Messenger.sendStored(new BigInteger(fileKey), myIpAddress, myPort, inetSocketAddress);

            System.out.println("mandei o 2 stored para o ip: " + succesorIpAddress + "porta: " + successorPort);
            Messenger.sendStored(new BigInteger(fileKey), myIpAddress, myPort, successorInetSocketAddress);

            if (replicationDegree >= 1) {
                System.out.println("SENDING BACKUP PARA O PROXIMO");
                message = "BACKUP " + ipAddress + " " + port + " " + succesorIpAddress + " " + successorPort + " "
                        + fileKey + " " + replicationDegree + " " + body.length + "\n";
                this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
            }
        } catch (Exception e) {

        }

        return "OK \n";
    }

    public String restoreHandler(String[] request) throws IOException {
        // RESTORE <file_key> <ip_address> <port>
        String fileKey = request[1];
        String ipAddress = request[2];
        int port = Integer.parseInt(request[3]);
        System.out.println("RESTORE");
        if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
            // deleteFile(fileKey)
            String fileName = this.peer.getBackupDirPath() + "/" + fileKey;
            File file = new File(fileName);
            byte[] body = null;

            try {
                body = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("MANDEI GIVE FILE");
            String message = "GIVEFILE " + fileKey + " " + body.length + " " + "\n";
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
            try {
                this.peer.sendMessage(message, body, inetSocketAddress);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (this.peer.getStorage().hasFileLocation(new BigInteger(fileKey))) {
            System.out.println(" EU TENHO A LISTA DOS PEERS");
            this.peer.getStorage().getFile(new BigInteger(fileKey), ipAddress, port);
        } else

        {
            String message = "RESTORE " + fileKey + " " + ipAddress + " " + port + "\n";

            try {
                this.peer.sendMessage(message, this.peer.getSuccessor().getInetSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "OK \n";
    }

    public String deleteHandler(String[] request) throws UnknownHostException {
        // DELETE <file_key> <ip_address> <port>
        String fileKey = request[1];
        String ipAddress = request[2];
        int port = Integer.parseInt(request[3]);
        this.peer.getStorage().removeAskedFile(new BigInteger(fileKey));

        if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
            this.peer.getStorage().removeStoredFile(new BigInteger(fileKey));
            Helper.deleteFile(fileKey, this.peer.getStorageDirPath(), this.peer.getBackupDirPath());
        }

        if (!(ipAddress.equals(this.peer.getAddress().getHostName())) && (this.peer.getPort() != port)) {
            if (this.peer.getStorage().hasFileLocation(new BigInteger(fileKey))) {
                try {
                    sendDelete(new BigInteger(fileKey), ipAddress, port);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                this.peer.getStorage().removeFileLocation(new BigInteger(fileKey));
            } else {

                this.peer.getStorage().removeFileLocation(new BigInteger(fileKey));

                String message = "DELETE " + fileKey + " " + ipAddress + " " + port + "\n";
                try {
                    this.peer.sendMessage(message, this.peer.getSuccessor().getInetSocketAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return "OK \n";
    }

    public String reclaimHandler(String[] request, byte[] body) {
        // "REMOVED " + fileId + " " + address.getAddress().getHostAddress() + " " +
        // address.getPort()
        System.out.println("EU RECEBI O REMOVED");
        BigInteger fileKey = new BigInteger(request[1]);
        String ipAddress = request[2];
        int port = Integer.parseInt(request[3]);
        OutsidePeer outsidePeer = this.peer.getSuccessor();
        this.peer.getStorage().removePeerLocation(fileKey, ipAddress, port);

        // Peer that holds table with file locations
        if (Helper.middlePeer(fileKey, peer.getPredecessor().getId(), peer.getId())) {

            String message1 = "REMOVELOCATION " + request[1] + " " + request[2] + " " + request[3] + "\n";

            String message = "BACKUP " + this.peer.getAddress().getAddress().getHostAddress() + " "
                    + this.peer.getAddress().getPort() + " "
                    + this.peer.getSuccessor().getInetSocketAddress().getAddress().getHostAddress() + " "
                    + this.peer.getSuccessor().getInetSocketAddress().getPort() + " " + fileKey + " " + "-1 "
                    + body.length + "\n";
            try {
                if (!outsidePeer.testSuccessor()) {
                    System.out.println("A mandar para o successor");
                    this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
                    this.peer.sendMessage(message1, body, outsidePeer.getInetSocketAddress());
                } else {
                    System.out.println("A mandar para o nextsuccessor");
                    OutsidePeer otherSuccessor = this.peer.getNextSuccessor();
                    this.peer.sendMessage(message, body, otherSuccessor.getInetSocketAddress());
                    this.peer.sendMessage(message1, body, otherSuccessor.getInetSocketAddress());
                }
            } catch (IOException e) {
            }
            return "OK \n";
        }

        String message = "REMOVED " + request[1] + " " + request[2] + " " + request[3] + " " + request[4] + "\n";
        System.out.println("Fiz forward do removed");
        try {
            this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK \n";
    }

    public String getFileHandler(String[] request, byte[] body) throws UnknownHostException {
        // GIVEFILE <file_key> <body>
        String fileKey = request[1];
        String folderDirectory = this.peer.getRestoreDirPath();
        String fileDirectory = this.peer.getRestoreDirPath() + "/" + fileKey;

        System.out.println("SAVING file...");
        try {
            final Path filePathDir = Paths.get(folderDirectory);
            final Path filePath = Paths.get(fileDirectory);
            if (Files.notExists(filePathDir))
                Files.createDirectories(filePathDir);

            // FileOutputStream outputStream = new FileOutputStream(fileDirectory, true);
            Files.newOutputStream(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE).write(body);
            // outputStream.write(body);
            // outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK \n";
    }

    public String storedHandler(String[] request) {
        // STORED <file_key> <ip_address> <port>
        System.out.println("Recebi um stored");
        BigInteger fileKey = new BigInteger(request[1]);
        OutsidePeer outsidePeer = new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3])));
        peer.getStorage().addFileLocation(fileKey, outsidePeer);
        System.out.println("stored i guess");
        return "OK \n";
    }

    public boolean sendDelete(BigInteger fileId, String ipAddress, int port) throws IOException {
        List<OutsidePeer> peers = this.peer.getStorage().getFileLocations().get(fileId);
        String message = new String();

        System.out.println("tamanho vetor " + peers.size());

        for (int i = 0; i < peers.size(); i++) {
            InetSocketAddress socket = peers.get(i).getInetSocketAddress();
            // FINDFILE file_key ip_address port
            message = "DELETE " + fileId + " " + ipAddress + " " + port + "\n";
            Messenger.sendMessage(message, socket);

            // sslSocket.close();
        }

        return true;
    }
}
