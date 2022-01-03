import java.rmi.RemoteException;
import java.util.ArrayList;

public class FollowersServiceClientImp implements FollowerServiceClient {

    private ArrayList<String> followers;

    public FollowersServiceClientImp(ArrayList<String> followers){
        this.followers = followers;
    }

    public void addFollower(String username) throws RemoteException{
        followers.add(username);
    }
    public void removeFollower (String username) throws RemoteException{
        followers.remove(username);
    }
}
