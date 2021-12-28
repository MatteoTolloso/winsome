

import java.rmi.RemoteException;
import java.util.ArrayList;

public class RegistrationServiceImp implements RegistrationService{
    
    private Database db;

    public RegistrationServiceImp(Database db) throws RemoteException{
        super();
        this.db = db;
    }

    public void register(String username, String password, ArrayList<String> tags) throws RemoteException, NullPointerException, UserAlrExiException, InvalidUsernameException{

        db.register(username, password, tags);

    }
}
