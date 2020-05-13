import java.math.BigInteger;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class OutsidePeer {
    private BigInteger id;
    private InetSocketAddress inetSocketAddress;

    public OutsidePeer(InetSocketAddress inetSocketAddress) {
        this.id = Helper.getPeerId(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
        this.inetSocketAddress = inetSocketAddress;
    }

    public BigInteger getId() {
        return id;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public OutsidePeer findSuccessor(BigInteger id) throws UnknownHostException, IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetSocketAddress.getAddress().getHostAddress(),
                inetSocketAddress.getPort());

        // SUCCESSOR <file_key> <ip_address> <port>
        String message = "SUCCESSOR " + id + " " + inetSocketAddress.getAddress().getHostAddress() + " "
                + inetSocketAddress.getPort() + "\n";

        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        System.out.println(message);
        out.writeBytes(message);
        String response = in.readLine();
        in.close();
        out.close();
        sslSocket.close();
        System.out.println(response);
        String[] splitMessage = response.split(" ");
        InetAddress inetAddress = InetAddress.getByName(splitMessage[1]);
        InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, Integer.parseInt(splitMessage[2]));
        
        return new OutsidePeer(socketAddress);
    }

    public void forwardBackupMessage(String[] string) throws UnknownHostException, IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetSocketAddress.getAddress().getHostAddress(),
                inetSocketAddress.getPort());

        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

        String str = Arrays.toString(string);
        out.writeBytes(str);
        String response = in.readLine();
        in.close();
        out.close();
        sslSocket.close();
    }

    public boolean middlePeer(BigInteger leftBoundary, BigInteger actual) {
        return (this.id.compareTo(leftBoundary) == -1) && (actual.compareTo(leftBoundary) == 1);
    }
}