import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.net.ssl.SSLSocket;

public interface RmiRemote extends Remote {

    String backup(String fileName, int replicationDegree);

    String restore(String fileName);

    String delete(String fileName);

    String reclaim(int space);

}