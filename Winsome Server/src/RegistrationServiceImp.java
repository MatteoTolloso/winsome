

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class RegistrationServiceImp implements RegistrationService{
    
    private Database db;

    public RegistrationServiceImp(Database db) throws RemoteException{
        super();
        this.db = db;
    }

    public void register(String username, String password, ArrayList<String> tags) throws RemoteException, NullPointerException, UserAlrExiException, InvalidUsernameException{

        //trasforma tutte le stringhe il lower case
        ArrayList<String> tagsLower = (ArrayList<String>) tags.stream().map(String::toLowerCase).collect(Collectors.toList());
        
        db.register(username, password, tagsLower);

    }
}
