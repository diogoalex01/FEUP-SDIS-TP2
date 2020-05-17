import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigInteger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements RmiRemote {
    private BigInteger id;
    private InetSocketAddress address;
    private int port;
    private OutsidePeer predecessor;
    private OutsidePeer successor;
    private FingerTable fingerTable;
    private RequestListener listener;
    private ScheduledThreadPoolExecutor executor;
    private static String storageDirPath;
    private static String backupDirPath;
    private static String restoreDirPath;
    private static Storage storage = new Storage();

    // private FixFingers checkFingers;
    private Stabilizer stabilizer;

    public Peer(String address, int port, String otherIpAddress, int otherPort)
            throws UnknownHostException, IOException {

        setJSSEProperties();
        // Find IP Address
        String ipAddress = new String();

        // try (final DatagramSocket socket = new DatagramSocket()) {
        // socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        // ipAddress = socket.getLocalAddress().getHostAddress();
        // }

        ipAddress = address;
        this.id = Helper.getPeerId(ipAddress, port); // chord.hashSocketAddress(address);
        this.port = port;
        this.listener = new RequestListener(this);
        executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(150);
        executor.execute(this.listener);
        this.stabilizer = new Stabilizer(this);
        executor.scheduleAtFixedRate(stabilizer, 5, 5, TimeUnit.SECONDS);

        // Initialize finger table
        fingerTable = new FingerTable(Helper.getNumberOfNodes(), new OutsidePeer(new InetSocketAddress(address, port)));

        InetAddress inetAddress = InetAddress.getByName(ipAddress);
        this.address = new InetSocketAddress(inetAddress, port);

        storageDirPath = "node" + "_" + this.id;
        backupDirPath = storageDirPath + "/Backup";
        restoreDirPath = storageDirPath + "/Restore";

        if (otherPort != -1) {
            InetAddress otherAddress = InetAddress.getByName(otherIpAddress);
            this.successor = new OutsidePeer(new InetSocketAddress(otherAddress, otherPort));
            // this.predecessor = this.successor;
            this.successor.findSuccessor(this.id, this.address);
            fingerTable.setAllEntries(this.successor);
        }

        System.out.println("peerID: " + this.id);
    }

    public ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public BigInteger getId() {
        return id;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public OutsidePeer getSuccessor() {
        return successor;
    }

    public void setSuccessor(OutsidePeer successor) {
        this.successor = successor;
    }

    public OutsidePeer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(OutsidePeer predecessor) {
        this.predecessor = predecessor;
    }

    public FingerTable getFingerTable() {
        return fingerTable;
    }

    public String getStorageDirPath() {
        return storageDirPath;
    }

    public String getBackupDirPath() {
        return backupDirPath;
    }

    public String getRestoreDirPath() {
        return restoreDirPath;
    }

    public Storage getStorage() {
        return storage;
    }

    /**
     * Set JSSE Properties
     */
    private static void setJSSEProperties() {
        System.setProperty("javax.net.ssl.keyStore", "keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "qwerty123");
        System.setProperty("javax.net.ssl.trustStore", "trustStore");
        System.setProperty("javax.net.ssl.trustStorePassword", "qwerty123");
    }

    public void stabilize() throws IOException {
        System.out.println("Stabilizing...");
        OutsidePeer predecessorPeer = successor.getPredecessor(address);

        if (predecessorPeer != null && !id.equals(predecessorPeer.getId())
                && (Helper.middlePeer(predecessorPeer.getId(), id, successor.getId())
                        || id.equals(successor.getId()))) {
            successor = predecessorPeer;
            fingerTable.add(predecessorPeer, 0);
        }
    }

    // TODO check if its your id
    @Override
    public String backup(String fileName, int replicationDegree) {
        System.out.println("Backup");
        if (fingerTable.getSize() == 0) {
            System.out.println("There are no peers available");
            return "ERROR";
        }

        BigInteger fileId = Helper.getFileId(fileName);
        OutsidePeer receiverPeer = fingerTable.getNearestPeer(fileId);
        this.storage.addAskedFile(fileId);

        File file = new File(fileName);
        byte[] body = null;

        try {
            body = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String message = " ";

        // FORWARD <file_key> <rep_degree> <InetAddress> <port> <body>
        if (body != null) {
            if (receiverPeer.getId() == fileId) {
                message = "BACKUP " + fileId + " " + replicationDegree + " " + body + "\n";
            } else {
                message = "FORWARD " + fileId + " " + replicationDegree + " " + body + "\n";
            }
        }

        try {
            sendMessage(message, receiverPeer.getInetSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("end Backup");
        return "OK";
    }

    @Override
    public String restore(String fileName) {
        System.out.println("restore");

        // String message = "BACKUP 32 1 127.0.0.1 7000 dfasfsdf\n";
        if (fingerTable.getSize() == 0 || this.successor == null) {
            System.out.println("There are no peers available");
            return "ERROR";
        }

        BigInteger fileId = Helper.getFileId(fileName);
        OutsidePeer receiverPeer = fingerTable.getNearestPeer(fileId);

        String message = "RESTORE " + fileId + " " + address.getAddress().getHostAddress() + " " + this.port + "\n";

        try {
            sendMessage(message, receiverPeer.getInetSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("end restore");
        return "";
    }

    @Override
    public String delete(String fileName) {
        System.out.println("delete");

        // String message = "BACKUP 32 1 127.0.0.1 7000 dfasfsdf\n";
        if (fingerTable.getSize() == 0 || this.successor == null) {
            System.out.println("There are no peers available");
            return "ERROR";
        }

        BigInteger fileId = Helper.getFileId(fileName);

        String message = "DELETE " + fileId + " " + address.getAddress().getHostAddress() + " " + this.port + "\n";

        try {
            sendMessage(message, this.successor.getInetSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("end delete");
        return "";
    }

    @Override
    public String reclaim(int space) {
        System.out.println("reclaim");
        System.out.println("end reclaim");
        return "";
    }

    public void sendMessage(String message, InetSocketAddress messageReceiver)
            throws UnknownHostException, IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(messageReceiver.getAddress().getHostAddress(),
                messageReceiver.getPort());

        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        System.out.println(message);
        out.writeBytes(message);
        String response = in.readLine();
        in.close();
        out.close();
        System.out.println("---9");
        sslSocket.close();
    }

    /**
     * Stores Class Records to a file
     */
    public static void storeFile() {
        try {
            String filePath = storageDirPath + "/" + "records.ser";
            FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            // Write objects to file
            objectOutputStream.writeObject(storage);

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        }
    }

    /**
     * Reads Class Records from a file
     */
    public void readFile() {
        try {
            String filePath = storageDirPath + "/" + "records.ser";
            final Path recordsPath = Paths.get(filePath);
            final Path peerFolderPath = Paths.get(storageDirPath);

            if (!Files.exists(peerFolderPath)) {
                Files.createDirectories(peerFolderPath);
                return;
            }

            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            // Read objects
            Storage storage = (Storage) objectInputStream.readObject();

            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 5) {
            System.out.println(
                    "Usage:\tPeer <rmi_accesspoint> <network_address> <port_number>\n\tPeer <network_address> <port_number> <network_address> <port_number_peer>");
            return;
        }
        String accessPoint = args[0];
        String address = args[1];
        int port = Integer.parseInt(args[2]);
        Peer server;

        try {
            if (args.length == 5) {
                String otherIpAddress = args[3];
                int otherPort = Integer.parseInt(args[4]);
                server = new Peer(address, port, otherIpAddress, otherPort);
                // public Peer(String accessPoint, int port, String otherIpAddress, String
                // otherPort)
            } else {
                server = new Peer(address, port, "0", -1);
            }

            RmiRemote rmiRemote = (RmiRemote) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, rmiRemote);
            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Peer::storeFile));
    }
}
