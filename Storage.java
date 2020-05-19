import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSocket;

import java.util.Collections;
import java.util.List;

public class Storage implements Serializable {
    private List<BigInteger> storedFiles = Collections.synchronizedList(new ArrayList<BigInteger>());
    private List<BigInteger> askedFiles = Collections.synchronizedList(new ArrayList<BigInteger>());
    private ConcurrentHashMap<BigInteger, List<OutsidePeer>> fileLocation = new ConcurrentHashMap<BigInteger, List<OutsidePeer>>();

    private int availableSpace;

    public Storage() {
        availableSpace = -1;
    }

    /**
     * Gets available space
     */
    public int getAvailableSpace() {
        return this.availableSpace;
    }

    /**
     * Sets available space
     */
    public void setAvailableSpace(int space) {
        this.availableSpace = space;
    }

    /**
     * Gets stored files list
     */
    public List<BigInteger> getStoredFiles() {
        return this.storedFiles;
    }

    /**
     * Checks if file is backed up
     */
    public boolean hasFileStored(BigInteger fileId) {
        return this.storedFiles.contains(fileId);
    }

    public void removeFileStored(BigInteger fileId) {
        this.storedFiles.remove(fileId);
    }

    /**
     * Gets asked files List
     */
    public List<BigInteger> getmyAskedFiles() {
        return this.askedFiles;
    }

    /**
     * Checks if it asked the peers to backup the file
     */
    public boolean hasAskedForFile(BigInteger fileID) {
        return this.askedFiles.contains(fileID);
    }

    /**
     * Adds a new asked file
     */
    public void addAskedFile(BigInteger fileId) {
        this.askedFiles.add(fileId);
    }

    public void removeAskedFile(BigInteger fileId) {
        this.askedFiles.remove(fileId);
    }

    /**
     * Adds a new stored file
     */
    public void addStoredFile(BigInteger fileId) {
        this.storedFiles.add(fileId);
    }

    public void removeStoredFile(BigInteger fileId) {
        this.storedFiles.remove(fileId);
    }

    /**
     * Adds file id to hash map
     */
    public void initializeFileLocation(BigInteger fileId) {
        if (!this.fileLocation.containsKey(fileId)) {
            this.fileLocation.put(fileId, Collections.synchronizedList(new ArrayList<OutsidePeer>()));
        }
    }

    /**
     * Adds file id to hash map
     */
    public void addFileLocation(BigInteger fileId, OutsidePeer outsidePeer) {
        if (this.fileLocation.containsKey(fileId)) {
            this.fileLocation.get(fileId).add(outsidePeer);
        }
    }

    /**
     * Adds file id to hash map
     */
    public void removeFileLocation(BigInteger fileId) {
        if (this.fileLocation.containsKey(fileId)) {
            this.fileLocation.remove(fileId);
        }
    }

    public boolean hasFileLocation(BigInteger fileID) {
        return this.fileLocation.containsKey(fileID);
    }

    public void getFile(BigInteger fileId, String ipAddress, int port) throws IOException {
        List<OutsidePeer> peers = fileLocation.get(fileId);
        String message = new String();
        String response = new String();
        SSLSocket sslSocket = null;
        System.out.println("tamanho vetor " + peers.size());
        for (int i = 0; i < peers.size(); i++) {
            InetSocketAddress socket = peers.get(i).getInetSocketAddress();
            // FINDFILE file_key ip_address port
            message = "FINDFILE " + fileId + " " + ipAddress + " " + port + "\n";
            sslSocket = Messenger.sendMessage(message, socket);
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            response = in.readLine();
            in.close();
            sslSocket.close();
            if (response.equals("SENT " + fileId)) {
                System.out.println("RECEBI SENT");
                break;
            }
        }

        return;
    }

    // public String print() {
    // for (String fileID : fileNames.keySet()) {
    // state += "\n> File path: " + fileNames.get(fileID);
    // state += "\n File ID: " + fileID;
    // Set<String> ChunkIDset = storedRecord.keySet().stream().filter(string ->
    // string.endsWith("_" + fileID))
    // .collect(Collectors.toSet());

    // if (ChunkIDset.size() != 0)
    // state += "\n Desired Replication Degree: "
    // +
    // storedRecord.get(ChunkIDset.iterator().next()).getDesiredReplicationDegree();
    // state += "\n > Chunk Information: ";
    // for (String chunkKey : ChunkIDset) {
    // state += "\n Chunk ID: " + storedRecord.get(chunkKey).getID();
    // state += "\n Actual Replication Degree: "
    // + storedRecord.get(chunkKey).getActualReplicationDegree() + "\n";
    // }
    // }

    // state += "\n";
    // state += "-------- Stored File Chunks --------\n";
    // for (String fileID : fileNames) {
    // try {
    // Set<String> ChunkIDset = storedChunks.keySet().stream().filter(string ->
    // string.endsWith("_" + fileID))
    // .collect(Collectors.toSet());

    // for (String chunkKey : ChunkIDset) {
    // final Path path = Paths
    // .get(this.backupDir + "/" + fileID + "/" +
    // storedChunks.get(chunkKey).getID());
    // // Checks if file exists
    // if (Files.exists(path)) {
    // state += "\n> Chunk ID: " + storedChunks.get(chunkKey).getFileID() + "_"
    // + storedChunks.get(chunkKey).getID();
    // state += "\n Size: " + storedChunks.get(chunkKey).getSize() + " Bytes";
    // state += "\n Actual Replication Degree: "
    // + storedChunks.get(chunkKey).getActualReplicationDegree();
    // }
    // }
    // } catch (Exception e) {
    // System.err.println("Path exception: " + e.toString());
    // e.printStackTrace();
    // }
    // }

    // state += "\n\n";

    // return state;
    // }

    public void print() {
        System.out.println("FL----------------------- ");
        for (BigInteger key : fileLocation.keySet()) {
            System.out.println("File: " + key);
            for (OutsidePeer peerKey : fileLocation.get(key)) {
                System.out.println("\tPeer: " + peerKey.getId());
            }
        }
        System.out.println("SF----------------------- ");
        System.out.println(Arrays.toString(storedFiles.toArray()));
        System.out.println("AF----------------------- ");
        System.out.println(Arrays.toString(askedFiles.toArray()));
        System.out.println("------------------------- ");
    }
}
