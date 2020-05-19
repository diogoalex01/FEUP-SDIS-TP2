import java.math.BigInteger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                    + this.peer.getAddress().getPort() + " " + fileKey + " " + replicationDegree + " " + body.length
                    + "\n";
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

        return "OK";
    }

    public String backupHandler(String[] request, byte[] body) {
        // BACKUP <ip_address> <port> <file_key> <rep_degree> <body>
        String ipAddress = request[1];
        int port = Integer.parseInt(request[2]);
        int myPort = this.peer.getAddress().getPort();
        String myIpAddress = this.peer.getAddress().getAddress().getHostAddress();
        try {
            String fileKey = request[3];
            int replicationDegree = Integer.parseInt(request[4]);
            // TODO: ver se o file foi enviado por mim
            if (replicationDegree < 1) {
                return "OK";
            }

            if (ipAddress.equals(myIpAddress) && myPort == port) {
                System.out.println("There aren't enough peers to backup the file");
                return "";
            }

            OutsidePeer outsidePeer = this.peer.getSuccessor();
            String message = "BACKUP " + ipAddress + " " + port + " " + fileKey + " " + replicationDegree + " "
                    + body.length + "\n";

            if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
                replicationDegree--;
                String message1 = "BACKUP " + ipAddress + " " + port + " " + fileKey + " " + replicationDegree + " "
                        + body.length + "\n";

                this.peer.sendMessage(message1, body, outsidePeer.getInetSocketAddress());
                return "OK";
            }

            if (this.peer.getStorage().hasAskedForFile(new BigInteger(fileKey))) {
                this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
                return "OK";
            }

            String fileDirName = this.peer.getBackupDirPath();
            // Store file
            final Path fileDirPath = Paths.get(fileDirName);

            if (Files.notExists(fileDirPath)) {
                Files.createDirectories(fileDirPath);
            }

            OutputStream outputStream = new FileOutputStream(fileDirName + "/" + fileKey);
            outputStream.write(body);
            outputStream.close();

            for (int i = 0; i < request.length; i++) {
                System.out.println(request[i]);
            }

            System.out.println("Stored!");
            replicationDegree--;
            peer.getStorage().addStoredFile(new BigInteger(fileKey));
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
            Messenger.sendStored(new BigInteger(fileKey), myIpAddress, myPort, inetSocketAddress);

            if (replicationDegree >= 1) {
                System.out.println("SENDING BACKUP PARA O PROXIMO");
                message = "BACKUP " + ipAddress + " " + port + " " + fileKey + " " + replicationDegree + " "
                        + body.length + "\n";
                this.peer.sendMessage(message, body, outsidePeer.getInetSocketAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "OK";
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

        } else {
            String message = "RESTORE " + fileKey + " " + ipAddress + " " + port + "\n";

            try {
                this.peer.sendMessage(message, this.peer.getSuccessor().getInetSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "OK";
    }

    public String deleteHandler(String[] request) throws UnknownHostException {
        // DELETE <file_key> <ip_address> <port>
        String fileKey = request[1];
        String ipAddress = request[2];
        int port = Integer.parseInt(request[3]);

        if (this.peer.getStorage().hasFileStored(new BigInteger(fileKey))) {
            this.peer.getStorage().removeStoredFile(new BigInteger(fileKey));
            this.peer.getStorage().removeFileLocation(new BigInteger(fileKey));
            deleteFile(fileKey);
        }

        if (!(ipAddress.equals(this.peer.getAddress().getHostName())) && (this.peer.getPort() != port)) {
            String message = "DELETE " + fileKey + " " + ipAddress + " " + port + "\n";
            try {
                this.peer.sendMessage(message, this.peer.getSuccessor().getInetSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "OK";
    }

    public void deleteFile(String fileId) {
        if (fileId.equals("") || fileId.equals(null)) {
            return;
        }

        File storageDir = new File(peer.getStorageDirPath());
        File backupDir = new File(peer.getBackupDirPath());
        File fileIDDir = new File(backupDir.getPath(), fileId);

        if (backupDir.exists()) {
            if (fileIDDir.delete()) {
                System.out.println("File deleted successfully");
            } else {
                System.out.println("Failed to delete the file");
            }
        }

        // Deletes the backup directory if it's empty after the fileID deletion
        File[] backupDirectory = backupDir.listFiles();
        if (backupDirectory.length == 0) {
            backupDir.delete();
        }

        // File[] storageDirectory = storageDir.listFiles();
        // if (storageDirectory.length == 0) {
        // storageDir.delete();
        // }
    }

    public String getFileHandler(String[] request, byte[] body) throws UnknownHostException {
        // GIVEFILE <file_key> <body>
        String fileKey = request[1];
        String folderDirectory = this.peer.getRestoreDirPath();
        String fileDirectory = this.peer.getRestoreDirPath() + "/" + fileKey;

        System.out.println("SAVING file...");
        try {
            final Path filePath = Paths.get(folderDirectory);

            if (Files.notExists(filePath))
                Files.createDirectories(filePath);

            FileOutputStream outputStream = new FileOutputStream(fileDirectory, true);

            outputStream.write(body);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    public String storedHandler(String[] request) {
        // STORED <file_key> <ip_address> <port>
        BigInteger fileKey = new BigInteger(request[1]);
        OutsidePeer outsidePeer = new OutsidePeer(new InetSocketAddress(request[2], Integer.parseInt(request[3])));
        peer.getStorage().addFileLocation(fileKey, outsidePeer);
        System.out.println("stored i guess");
        return "OK";
    }
}
