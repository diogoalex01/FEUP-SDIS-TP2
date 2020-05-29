import java.math.BigInteger;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLSocket;

public class Storage {
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
     * Clear file location
     */
    public void clearFileLocation() {
        fileLocation.clear();
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
        if (!askedFiles.contains(fileId)) {
            askedFiles.add(fileId);
        }
    }

    public void removeAskedFile(BigInteger fileId) { 
        if (askedFiles.contains(fileId)) {
            askedFiles.remove(fileId);
        }
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

    public ConcurrentHashMap<BigInteger, List<OutsidePeer>> getFileLocations() {
        return fileLocation;
    }

    /**
     * Adds file id to hash map
     */
    public void addFileLocation(BigInteger fileId, OutsidePeer outsidePeer) {
        if (this.fileLocation.containsKey(fileId)) {
            if (!this.fileLocation.get(fileId).contains(outsidePeer)) {
                this.fileLocation.get(fileId).add(outsidePeer);
            }
        } else {
            List<OutsidePeer> list = Collections.synchronizedList(new ArrayList<OutsidePeer>());
            list.add(outsidePeer);
            this.fileLocation.put(fileId, list);
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

    public void removePeerLocation(BigInteger fileId, String ipAddress, int port) {
        OutsidePeer peer = new OutsidePeer(new InetSocketAddress(ipAddress, port));

        if (fileLocation.containsKey(fileId)) {
            List<OutsidePeer> peers = fileLocation.get(fileId);
            if (peers.contains(peer)) {
                peers.remove(peer);
                fileLocation.remove(fileId);
                fileLocation.put(fileId, peers);
            }
        }
    }

    public boolean getFile(BigInteger fileId, String ipAddress, int port) throws IOException {
        List<OutsidePeer> peers = fileLocation.get(fileId);
        String message = new String();
        String response = new String();
        SSLSocket sslSocket = null;

        for (int i = 0; i < peers.size(); i++) {
            InetSocketAddress socket = peers.get(i).getInetSocketAddress();
            // FINDFILE file_key ip_address port
            message = "FINDFILE " + fileId + " " + ipAddress + " " + port + "\n";
            sslSocket = Messenger.sendMessage(message, socket);
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            if (sslSocket.isInputShutdown()) {
                continue;
            }
            response = in.readLine();
            in.close();
            if (response.equals("SENT " + fileId)) {
                return true;
            }
        }

        return false;
    }

    public boolean sendDelete(BigInteger fileId) throws IOException {
        List<OutsidePeer> peers = fileLocation.get(fileId);
        String message = new String();
        if(peers != null){
            for (int i = 0; i < peers.size(); i++) {
                InetSocketAddress socket = peers.get(i).getInetSocketAddress();
                // FINDFILE file_key ip_address port
                message = "DELETE " + fileId + "\n";
                Messenger.sendMessage(message, socket);
            }
        }

        return true;
    }

    public int spaceOccupied(String path) {
        int counter = 0;

        for (BigInteger hash : storedFiles) {
            String filename = path + "/" + hash;

            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                continue;
            }

            counter += file.length();
        }

        System.out.println("Space occupied: " + counter);
        return counter;
    }

    public boolean checkIfOverload(String path) {
        return this.availableSpace >= this.spaceOccupied(path) || storedFiles.size() == 0;
    }

    /**
     * Chooses a random chunk from the stored chunks
     */
    public BigInteger randomFile() {
        Random rand = new Random();
        int randomNumber = rand.nextInt(storedFiles.size());

        BigInteger hash = storedFiles.get(randomNumber);

        return hash;
    }


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
