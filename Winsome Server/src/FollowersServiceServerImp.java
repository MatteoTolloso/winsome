import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class FollowersServiceServerImp implements FollowerServiceServer {
    
    private Database db;
    private ConcurrentHashMap<String, FollowerServiceClient> callbackMap;
    

    public FollowersServiceServerImp(Database db, ConcurrentHashMap<String, FollowerServiceClient> callbackMap){
        this.db = db;
        this.callbackMap = callbackMap;
    }
    
    public void addFollowerServiceClient(String username, String password, FollowerServiceClient fsc) throws RemoteException, UserNotFoundException, NotAllowedException{

        try{
            if(! db.getPassword(username).equals(password)) throw new NotAllowedException();   // password errata
        }
        catch(NullPointerException e){  // nome utente errato
            throw new UserNotFoundException();
        }

        callbackMap.put(username, fsc);

    }
    public void removeFollowerServiceClient(String username, String password) throws RemoteException, UserNotFoundException, NotAllowedException{
        
        try{
            if(! db.getPassword(username).equals(password)) throw new NotAllowedException();   // password errata
        }
        catch(NullPointerException e){  // nome utente errato
            throw new UserNotFoundException();
        }

        callbackMap.remove(username);

    }
    public ArrayList<String> getFollower(String username, String password) throws RemoteException, UserNotFoundException, NotAllowedException{
        
        try{
            if(! db.getPassword(username).equals(password)) throw new NotAllowedException();   // password errata
        }
        catch(NullPointerException e){  // nome utente errato
            throw new UserNotFoundException();
        }

        return db.listFollowers(username);

    }

}
