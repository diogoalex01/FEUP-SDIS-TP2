import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Messenger {
    public static SSLSocket sendMessage(String message, InetSocketAddress socket) {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = null;
        try {
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket.getAddress().getHostAddress(),
                    socket.getPort());

            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            out.writeBytes(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sslSocket;
    }

    public static void sendUpdatePosition(String predecessorIp, int predecessorPort, String successorIp,
            int successorPort, InetSocketAddress address) {
        String message = "UPDATEPOSITION " + predecessorIp + " " + predecessorPort + " " + successorIp + " "
                + successorPort + "\n";
        // System.out.println("update position message " + message);
        SSLSocket sslSocket = null;
        try {
            sslSocket = sendMessage(message, address);
            // System.out.println("---1");
            // sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFindSuccessor(BigInteger peerKey, String newPeerIp, int newPeerPort,
            InetSocketAddress address) {

        String message = "FINDSUCCESSOR " + peerKey + " " + newPeerIp + " " + newPeerPort + "\n";
        SSLSocket sslSocket = null;

        try {
            sslSocket = sendMessage(message, address);
            // System.out.println("---2");
            // sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFindFinger(InetSocketAddress ipAddress, InetSocketAddress entryAddress, int index,
            BigInteger key) {
        String message = "MARCO " + index + " " + ipAddress.getAddress().getHostAddress() + " " + ipAddress.getPort()
                + " " + key + "\n";
        SSLSocket sslSocket = null;

        try {
            sslSocket = sendMessage(message, entryAddress);
            // System.out.println("---3");
            // sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendUpdateFinger(InetSocketAddress entryAddress, InetSocketAddress destinationIpAddress,
            int index) {
        String message = "UPDATEFINGER " + entryAddress.getAddress().getHostAddress() + " " + entryAddress.getPort()
                + " " + index + "\n";
        SSLSocket sslSocket = null;

        try {
            sslSocket = sendMessage(message, destinationIpAddress);
            // System.out.println("---4");
            // sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendStored(BigInteger fileKey, String myIpAddress, int myPort, InetSocketAddress socket) {
        String message = "STORED " + fileKey + " " + myIpAddress + " " + myPort + "\n";

        SSLSocket sslSocket = null;

        try {
            sslSocket = sendMessage(message, socket);
            // System.out.println("---3");
            // sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}