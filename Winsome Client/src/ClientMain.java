
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Arrays;


public class ClientMain {

    private static int registryPort = 8888;

    
    public static void main(String[] args){

        System.out.println("Client avviato");

        try {
            Registry r = LocateRegistry.getRegistry(registryPort);
            
            for (String s : r.list()){
                System.out.println(s);
            }
            
            RegistrationService serverObject = (RegistrationService) r.lookup("register");
            
            serverObject.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));

        } catch (RemoteException | NotBoundException | NullPointerException | UserAlrExiException | InvalidUsernameException e) {
            e.printStackTrace(); 
        }
        
    }
}
