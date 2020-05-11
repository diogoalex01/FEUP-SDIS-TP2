import java.io.*;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Peer implements RmiRemote {
    private BigInteger id;
    private InetSocketAddress address;
    private int port;
    private OutsidePeer successor;
    private FingerTable fingerTable;
    private Chord chord;
    private final int numberOfNodes = 8;
    private ScheduledThreadPoolExecutor executor;

    // private FixFingers checkFingers;
    // private Stabilize stabilize;

    public Peer(String accessPoint, int port, String otherIpAddress, int otherPort)
            throws UnknownHostException, IOException {

        String ipAddress = new String();
        // Find IP Address
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ipAddress = socket.getLocalAddress().getHostAddress();
        }
        setExecutor(new ScheduledThreadPoolExecutor(150));
        chord = new Chord();
        InetAddress inetAddress = InetAddress.getByName(ipAddress);
        this.address = new InetSocketAddress(inetAddress, port);
        this.id = FingerTable.getId(ipAddress, port);// chord.hashSocketAddress(address);

        this.port = port;

        // initialize finger table
        fingerTable = new FingerTable(numberOfNodes);
        if (otherPort != -1) {
            fingerTable.add(otherIpAddress, otherPort);
        }

        initializeSSL();
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


    /**
     * Set JSSE Properties
     */

    // System.setProperty("javax.net.ssl.keyStore","clientKeyStore.key");
    // System.setProperty("javax.net.ssl.keyStorePassword","qwerty");
    // System.setProperty("javax.net.ssl.trustStore","clientTrustStore.key");
    // System.setProperty("javax.net.ssl.trustStorePassword","qwerty");

    private static void setJSSEProperties() {
        System.setProperty("javax.net.ssl.keyStore", "jsse/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "qwerty123");
        System.setProperty("javax.net.ssl.trustStore", "jsse/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "qwerty123");
    }

    public void initializeSSL() throws IOException {
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
        // InputStream inputStream = sslSocket.getInputStream();
        // OutputStream outputStream = sslSocket.getOutputStream();

        // outputStream.write(1);
        // while (inputStream.available() > 0) {
        // System.out.print(inputStream.read());
        // }

        System.out.println("Secured connection initialized successfully");
    }

    @Override
    public String backup(String[] request) {
        System.out.println("Backup");

        System.out.println("end Backup");
        return "";
    }

    @Override
    public String restore(String[] request) {
        System.out.println("restore");
        System.out.println("end restore");
        return "";
    }

    @Override
    public String delete(String[] request) {
        System.out.println("delete");
        System.out.println("end delete");
        return "";
    }

    @Override
    public String reclaim(String[] request) {
        System.out.println("reclaim");
        System.out.println("end reclaim");
        return "";
    }

    public static void main(String[] args) {
        // +Peer access point
        // +Peer port
        // ?Peer ip
        Peer server;
        String accessPoint = args[0];
        int port = Integer.parseInt(args[1]);

        if (args.length == 2 || args.length == 4) {
            System.out.println(
                    "\n Usage:\tPeer <port_number>\n\tPeer <port_number> <network_address> <port_number_peer>");
            return;
        }

        try {
            if (args.length == 4) {
                int otherPort = Integer.parseInt(args[2]);
                String otherIpAddress = args[3];
                server = new Peer(accessPoint, port, otherIpAddress, otherPort);
                // public Peer(String accessPoint, int port, String otherIpAddress, String
                // otherPort)
            } else {
                server = new Peer(accessPoint, port, "0", -1);
            }

            RmiRemote stub = (RmiRemote) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, stub);
            setJSSEProperties();
            System.out.println("Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}