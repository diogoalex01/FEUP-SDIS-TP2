import java.math.BigInteger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

        return "OK\n";
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
            if (replicationDegree < 1 && replicationDegree != -1) {

                return "OK\n";
            }

            if (ipAddress.equals(myIpAddress) && myPort == port) {
                System.out.println("There aren't enough peers to backup the file");

                return "OK\n";
            }

            OutsidePeer outsidePeer = this.peer.getSuccessor();
            String message = "BACKUP " + ipAddress + " " + port + " " + succesorIpAddress + " " + successorPort + " "
                    + fileKey + " " + replicationDegree + " " + body.length + "\n";

            if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {

                if (replicationDegree > 1 || replicationDegree == -1) {

                    if (replicationDegree != -1) {
                        replicationDegree--;
                    }

                    String message1 = "BACKUP " + ipAddress + " " + port + " " + succesorIpAddress + " " + successorPort
                            + " " + fileKey + " " + replicationDegree + " " + body.length + "\n";

                    if (!outsidePeer.testSuccessor()) {
                        Peer.sendMessage(message1, body, outsidePeer.getInetSocketAddress());
                    } else {
                        OutsidePeer otherSuccessor = this.peer.getNextSuccessor();
                        Peer.sendMessage(message1, body, otherSuccessor.getInetSocketAddress());
                    }

                }
                return "OK\n";
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

                return "OK\n";
            }

            String fileDirName = this.peer.getBackupDirPath();
            // Store file
            final Path DirPath = Paths.get(fileDirName);

            if (Files.notExists(DirPath)) {
                Files.createDirectories(DirPath);
            }

            final Path fileDirPath = Paths.get(fileDirName + "/" + fileKey);

            Files.newOutputStream(fileDirPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE).write(body);
            System.out.println("File size: " + body.length + "bytes");

            System.out.println("Stored!");
            replicationDegree--;

            OutsidePeer peer = new OutsidePeer(inetSocketAddress);
            this.peer.getStorage().addStoredFile(new BigInteger(fileKey));
            Messenger.sendStored(new BigInteger(fileKey), myIpAddress, myPort, inetSocketAddress);
            Messenger.sendStored(new BigInteger(fileKey), myIpAddress, myPort, successorInetSocketAddress);

            if (replicationDegree >= 1) {
                message = "BACKUP " + ipAddress + " " + port + " " + succesorIpAddress + " " + successorPort + " "
                        + fileKey + " " + replicationDegree + " " + body.length + "\n";
                this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
            }
        } catch (Exception e) {

        }

        return "OK\n";
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
            String message = "GIVEFILE " + fileKey + " " + body.length + " " + "\n";
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
            try {
                this.peer.sendMessage(message, body, inetSocketAddress);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (this.peer.getStorage().hasFileLocation(new BigInteger(fileKey))) {
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

        return "OK\n";
    }

    public String deleteHandler(String[] request) throws UnknownHostException {
        // DELETE <file_key> <ip_address> <port>
        String fileKey;
        String ipAddress;
        int port;
        if (request.length == 4) {
            fileKey = request[1];
            ipAddress = request[2];
            port = Integer.parseInt(request[3]);

        } else {
            fileKey = request[1];
            this.peer.getStorage().removeFileLocation(new BigInteger(fileKey));
            if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
                this.peer.getStorage().removeStoredFile(new BigInteger(fileKey));
                Helper.deleteFile(fileKey, this.peer.getStorageDirPath(), this.peer.getBackupDirPath());
            }
            return "OK\n";
        }

        this.peer.getStorage().removeAskedFile(new BigInteger(fileKey));

        if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
            this.peer.getStorage().removeStoredFile(new BigInteger(fileKey));
            Helper.deleteFile(fileKey, this.peer.getStorageDirPath(), this.peer.getBackupDirPath());
        }

        if (!(ipAddress.equals(this.peer.getAddress().getHostName())) && (this.peer.getPort() != port)) {
            String message;
            if (Helper.middlePeer(new BigInteger(fileKey), peer.getPredecessor().getId(), peer.getId())) {
                message = "DELETE " + fileKey + "\n";
                try {
                    this.peer.getStorage().sendDelete(new BigInteger(fileKey));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.peer.getStorage().removeFileLocation(new BigInteger(fileKey));
            } else {
                this.peer.getStorage().removeFileLocation(new BigInteger(fileKey));
                message = "DELETE " + fileKey + " " + ipAddress + " " + port + "\n";
            }
            try {
                this.peer.sendMessage(message, this.peer.getSuccessor().getInetSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "OK\n";
    }

    public String reclaimHandler(String[] request, byte[] body) {
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
                    this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
                    this.peer.sendMessage(message1, body, outsidePeer.getInetSocketAddress());
                } else {
                    OutsidePeer otherSuccessor = this.peer.getNextSuccessor();
                    this.peer.sendMessage(message, body, otherSuccessor.getInetSocketAddress());
                    this.peer.sendMessage(message1, body, otherSuccessor.getInetSocketAddress());
                }
            } catch (IOException e) {
            }
            return "OK\n";
        }

        String message = "REMOVED " + request[1] + " " + request[2] + " " + request[3] + " " + request[4] + "\n";
        try {
            this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK\n";
    }

    public String getFileHandler(String[] request, byte[] body) throws UnknownHostException {
        // GIVEFILE <file_key> <body>
        String fileKey = request[1];
        String folderDirectory = this.peer.getRestoreDirPath();
        String fileDirectory = this.peer.getRestoreDirPath() + "/" + fileKey;

        System.out.println("Saving file...");
        try {
            final Path filePathDir = Paths.get(folderDirectory);
            final Path filePath = Paths.get(fileDirectory);
            if (Files.notExists(filePathDir))
                Files.createDirectories(filePathDir);

            Files.newOutputStream(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE).write(body);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK\n";
    }

    public String storedHandler(String[] request) {
        // STORED <file_key> <ip_address> <port>
        BigInteger fileKey = new BigInteger(request[1]);
        OutsidePeer outsidePeer = new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3])));
        peer.getStorage().addFileLocation(fileKey, outsidePeer);
        return "OK\n";
    }

}
