import java.math.BigInteger;
import java.util.Arrays;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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

    public void findSuccessor(BigInteger peerKey, InetSocketAddress peerInetSocketAddress)
            throws UnknownHostException, IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetSocketAddress.getAddress().getHostAddress(),
                inetSocketAddress.getPort());

        // FINDSUCCESSOR <peer_key> <ip_address> <port>
        String message = "FINDSUCCESSOR " + peerKey + " " + peerInetSocketAddress.getAddress().getHostAddress() + " "
                + peerInetSocketAddress.getPort() + "\n";

        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        // BufferedReader in = new BufferedReader(new
        // InputStreamReader(sslSocket.getInputStream()));
        // System.out.println("out-findsuc sent: " + message);
        out.writeBytes(message);
        // String response = in.readLine();
        // in.close();
        out.close();
        // System.out.println("---5");
        // sslSocket.close();
        // String[] splitMessage = response.split(" ");
        // InetAddress inetAddress = InetAddress.getByName(splitMessage[1]);
        // InetSocketAddress socketAddress = new InetSocketAddress(inetAddress,
        // Integer.parseInt(splitMessage[2]));

        // return new OutsidePeer(socketAddress);
    }

    public void notifySuccessor(InetSocketAddress peerSocketAddress, InetSocketAddress successorSocketAddress)
            throws UnknownHostException, IOException {
        // UPDATEPREDECESSOR <ip_address> <port>
        String message = "UPDATEPREDECESSOR " + peerSocketAddress.getAddress().getHostAddress() + " "
                + peerSocketAddress.getPort() + "\n";
        SSLSocket sslSocket = Messenger.sendMessage(message, successorSocketAddress);
        // sslSocket.close();
    }

    public boolean testSuccessor() {
        System.out.println(("ESTOU A TESTAR"));
        try {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory
                    .createSocket(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
            sslSocket.setSoTimeout(1000);
            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            String message = "TEST\n"; 
            out.writeBytes(message);
            return false;
        } catch (SocketTimeoutException e) {
            System.out.println(("nao ESTA a funcionar"));
            return true;
        } catch (ConnectException e) {
            System.out.println(("nao ESTA a funcionar"));
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(("nao ESTA a funcionar"));
        return true;
    }

    public OutsidePeer getPredecessor(InetSocketAddress peerSocketAddress) throws IOException {
        // FINDPREDECESSOR <ip_address> <port>
        String message = "FINDPREDECESSOR " + peerSocketAddress.getAddress().getHostAddress() + " "
                + peerSocketAddress.getPort() + "\n";
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetSocketAddress.getAddress().getHostAddress(),
                inetSocketAddress.getPort());
        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        out.writeBytes(message);
        String response = in.readLine();
        in.close();
        out.close();
        // sslSocket.close();
        // PREDECESSOR <ip_address> <port>
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

        out.writeBytes(Arrays.toString(string));
        String response = in.readLine();
        // in.close();
        // out.close();
        // System.out.println("---8");
        // sslSocket.close();
    }

    public OutsidePeer getNextSuccessor() throws UnknownHostException, IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetSocketAddress.getAddress().getHostAddress(),
                inetSocketAddress.getPort());

        DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

        String message = "NEXTSUCCESSOR \n";
        out.writeBytes(message);
        String[] response = in.readLine().split(" ");
        return new OutsidePeer(new InetSocketAddress(response[1], Integer.parseInt(response[2])));
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        OutsidePeer otherPeer = (OutsidePeer) o;
        // field comparison
        return (id.compareTo(otherPeer.getId()) == 0);
    }
}