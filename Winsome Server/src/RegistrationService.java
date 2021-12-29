
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RegistrationService extends Remote {
    
    public void register(String username, String password, ArrayList<String> tags) throws RemoteException, NullPointerException, UserAlrExiException, InvalidUsernameException;

}
