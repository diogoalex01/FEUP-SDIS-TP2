import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.math.BigInteger;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class OutsidePeer {
    private BigInteger id;
    private InetSocketAddress inetSocketAddress;

    public OutsidePeer(BigInteger id, InetSocketAddress inetSocketAddress) {
        this.id = id;
        this.inetSocketAddress = inetSocketAddress;
    }

    public BigInteger getId(){
        return id;
    }

    public InetSocketAddress getInetSocketAddress(){
        return inetSocketAddress;
    }

    public OutsidePeer findSuccessor(BigInteger id) throws UnknownHostException, IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetSocketAddress.getAddress().getHostAddress(),
                inetSocketAddress.getPort());
        String message = "SUCCESSOR " + id;

        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

        out.writeBytes(message);
        String response = in.readLine();
        in.close();
        out.close();
        sslSocket.close();

        String[] splitMessage = response.split(" ");
        InetAddress inetAddress = InetAddress.getByName(splitMessage[1]);
        InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, Integer.parseInt(splitMessage[2]));

        return new OutsidePeer(new BigInteger(splitMessage[0]), socketAddress);
    }

    public boolean middlePeer(BigInteger leftBoundary, BigInteger actual) {
        return (this.id.compareTo(leftBoundary) == -1) && (actual.compareTo(leftBoundary) == 1);
    }
}