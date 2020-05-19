import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

class RequestListener implements Runnable {
    private final Peer peer;

    public RequestListener(final Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        final SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory
                .getDefault();
        SSLServerSocket sslServerSocket = null;

        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(peer.getPort());
        } catch (final IOException e) {
            e.printStackTrace();
        }

        while (true) {
            SSLSocket sslSocket;
            // System.out.println("listining");

            try {
                sslSocket = null;
                sslSocket = (SSLSocket) sslServerSocket.accept();
                peer.getExecutor().execute(new RequestHandler(peer, sslSocket));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}