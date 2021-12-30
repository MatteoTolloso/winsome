
import java.io.*;
import java.net.Socket;
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
    private static String hostname = "127.0.0.1";
    private static String TER = "\r\n";

    
    public static void main(String[] args){

        System.out.println("Client avviato");

        /*
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
        */

        try (Socket server = new Socket(hostname, 9999);
            DataOutputStream outToClient = new DataOutputStream(server.getOutputStream());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(server.getInputStream()))
            ){

            BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder request;

            do{
               request = new StringBuilder();

                request.append( commandLine.readLine() );

                request.append(TER);

                outToClient.writeBytes(request.toString());

                String response = inFromClient.readLine();
                
                while(inFromClient.readLine())
    
                System.out.println(response);

                
            }while (! (request.toString().contains("logout")));

        


            server.close();


        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
    }
}
