import java.rmi.Remote;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface RmiRemote extends Remote {

    String backup(String fileName, int replicationDegree) throws IOException, NoSuchAlgorithmException;

    String restore(String fileName) throws IOException, NoSuchAlgorithmException;

    String delete(String fileName) throws IOException, NoSuchAlgorithmException;

    String reclaim(int space) throws IOException, NoSuchAlgorithmException;

    String state() throws IOException, NoSuchAlgorithmException;
}