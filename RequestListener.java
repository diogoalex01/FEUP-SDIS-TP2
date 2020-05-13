
import java.io.IOException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

class RequestListener implements Runnable {
    private Peer peer;

    public RequestListener(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket = null;

        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(peer.getPort() + 80);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            SSLSocket sslSocket;

            try {
                sslSocket = (SSLSocket) sslServerSocket.accept();
                peer.getExecutor().execute(new RequestHandler(peer, sslSocket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}