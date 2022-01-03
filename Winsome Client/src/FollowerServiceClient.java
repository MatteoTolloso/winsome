import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FollowerServiceClient extends Remote {
    public void addFollower(String username) throws RemoteException;
    public void removeFollower (String username) throws RemoteException;
}
