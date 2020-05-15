import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Messenger {
    public static void sendMessage(String message, InetSocketAddress socket) {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket.getAddress().getHostAddress(),
                    socket.getPort());

            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            out.writeBytes(message);
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendUpdatePosition(String predecessorIp, int predecessorPort, String successorIp,
            int successorPort, InetSocketAddress address) {
        String message = "UPDATEPOSITION " + predecessorIp + " " + predecessorPort + " " + successorIp + " "
                + successorPort + "\n";
            System.out.println("update position message " + message);
        try {
            sendMessage(message, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFindSuccessor(BigInteger peerKey, String newPeerIp, int newPeerPort,
            InetSocketAddress address) {

        String message = "FINDSUCCESSOR " + peerKey + " " + newPeerIp + " " + newPeerPort + "\n";

        try {
            sendMessage(message, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFindFinger(InetSocketAddress ipAddress, InetSocketAddress entryAddress, int index,
    BigInteger key) {
        String message = "MARCO " + key + " " + ipAddress.getAddress().getHostAddress() + " " + ipAddress.getPort() + " " + index;
        try {
            sendMessage(message, entryAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendUpdateFinger(InetSocketAddress entryAddress, InetSocketAddress destinationIpAddress, int index) {
        String message = "UPDATEFINGER " + entryAddress.getAddress().getHostAddress() + " " + entryAddress.getPort() + " " + index;
        try {
            sendMessage(message, destinationIpAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}