import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface FollowerServiceServer extends Remote {

    public void addFollowerServiceClient(String username, String password, FollowerServiceClient fsc) throws RemoteException, UserNotFoundException, NotAllowedException;
    public void removeFollowerServiceClient(String username, String password) throws RemoteException, UserNotFoundException, NotAllowedException;
    public ArrayList<String> getFollower(String username, String password) throws RemoteException, UserNotFoundException, NotAllowedException;
}
