import java.io.*;
import java.math.BigInteger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Peer implements RmiRemote {
    private BigInteger id;
    private InetSocketAddress address;
    private int port;
    private OutsidePeer successor;
    private FingerTable fingerTable;
    private RequestListener listener;
    private ScheduledThreadPoolExecutor executor;
    private static String storageDirPath;
    private static String backupDirPath;
    private static String restoreDirPath;
    private final int numberOfNodes = 8;
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
        this.port = port;
        this.listener = new RequestListener(this);
        executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(150);
        executor.execute(this.listener);
        this.stabilizer = new Stabilizer(this);
        executor.scheduleAtFixedRate(stabilizer, 10, 10, TimeUnit.SECONDS);

        // Initialize finger table
        fingerTable = new FingerTable(numberOfNodes);

        InetAddress inetAddress = InetAddress.getByName(ipAddress);
        this.address = new InetSocketAddress(inetAddress, port);
        this.id = Helper.getPeerId(ipAddress, port).mod(new BigInteger("2").pow(this.fingerTable.getSize())); // chord.hashSocketAddress(address);

        storageDirPath = "node" + "_" + this.id;
        backupDirPath = storageDirPath + "/Backup";
        restoreDirPath = storageDirPath + "/Restore";

        if (otherPort != -1) {
            InetAddress otherAddress = InetAddress.getByName(otherIpAddress);
            this.successor = new OutsidePeer(new InetSocketAddress(otherAddress, otherPort));
            this.successor.findSuccessor(this.id);
        }
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

    @Override
    public String backup(String fileName, int replicationDegree) {
        System.out.println("Backup");

        // String message = "BACKUP 32 1 127.0.0.1 7000 dfasfsdf\n";
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
        sslSocket.close();
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 4) {
            System.out.println(
                    "\n Usage:\tPeer <network_address> <port_number>\n\tPeer <network_address> <port_number> <network_address> <port_number_peer>");
            return;
        }

        String address = args[0];
        int port = Integer.parseInt(args[1]);
        Peer server;

        try {
            if (args.length == 4) {
                String otherIpAddress = args[2];
                int otherPort = Integer.parseInt(args[3]);
                server = new Peer(address, port, otherIpAddress, otherPort);
                // public Peer(String accessPoint, int port, String otherIpAddress, String
                // otherPort)
            } else {
                server = new Peer(address, port, "0", -1);
            }

            // RmiRemote stub = (RmiRemote) UnicastRemoteObject.exportObject(server, 0);
            // Registry registry = LocateRegistry.getRegistry();
            // registry.rebind(accessPoint, stub);
            // setJSSEProperties();
            // System.out.println("Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Stores Class Records to a File
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
}
