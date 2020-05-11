import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.net.ssl.SSLSocket;

public interface RmiRemote extends Remote {

    String backup(String[] request) throws RemoteException;

    String restore(String[] request) throws RemoteException;

    String delete(String[] request) throws RemoteException;
    
    String reclaim(String[] request) throws RemoteException;

}