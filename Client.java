import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    private static String accessPoint;
    private static String protocol;
    private static String filePath;

    public static void main(String[] args) {
        if (!init(args))
            return;

        try {
            String accessPoint = args[0];
            Registry registry = LocateRegistry.getRegistry();
            RmiRemote remote = (RmiRemote) registry.lookup(accessPoint);

            parseArgs(args, remote);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static boolean init(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.out.println("\n Usage:\tClient <peerAccessPoint> <subProtocol> \n");
            System.out.println(" Subprotocols :\t");
            System.out.println(" - BACKUP <fileID> \t");
            System.out.println(" - RESTORE <fileID>\t");
            System.out.println(" - DELETE <fileID>\t");
            System.out.println(" - RECLAIM <space>\t");
            return false;
        }

        accessPoint = args[0];
        protocol = args[1];
        filePath = args[2];

        return true;
    }

    private static void parseArgs(String[] args, RmiRemote peer) throws Exception {
        switch (protocol) {
            case "BACKUP":
                // int repDegree = Integer.parseInt(args[3]);
                peer.backup(filePath, 1);
                System.out.println("\nBackup finished successfully\n");
                break;

            case "RESTORE":
                peer.restore(filePath);
                System.out.println("\nRestore finished successfully\n");
                break;

            case "DELETE":
                peer.delete(filePath);
                System.out.println("\nFile deleted successfully\n");
                break;

            case "RECLAIM":
                int size = Integer.parseInt(args[2]);
                peer.reclaim(size);
                System.out.println("\nSpace reclaimed successfully\n");
                break;
        }
    }
}