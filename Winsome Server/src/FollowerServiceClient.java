import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FollowerServiceClient extends Remote {
    void addFollower(String username) throws RemoteException;
    void removeFollower (String username) throws RemoteException;
}
