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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sslSocket;
    }

    public static void sendUpdatePosition(String predecessorIp, int predecessorPort, String successorIp,
            int successorPort, InetSocketAddress address) {
        String message = "UPDATEPOSITION " + predecessorIp + " " + predecessorPort + " " + successorIp + " "
                + successorPort + "\n";
        System.out.println("update position message " + message);
        SSLSocket sslSocket = null;
        try {
            sslSocket = sendMessage(message, address);
            sslSocket.close();
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
            sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFindFinger(InetSocketAddress ipAddress, InetSocketAddress entryAddress, int index,
            BigInteger key) {
        String message = "MARCO " + key + " " + ipAddress.getAddress().getHostAddress() + " " + ipAddress.getPort()
                + " " + index + "\n";
        SSLSocket sslSocket = null;

        try {
            sslSocket = sendMessage(message, entryAddress);
            sslSocket.close();
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
            sslSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}