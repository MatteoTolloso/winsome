
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.management.loading.MLet;


public class ClientMain{


    private static int registryPort = 8888;
    private static String hostname = "127.0.0.1";
    private static int hostport = 9999;
    private static String TER = "\r\n";
    private static ArrayList<String> followers = new ArrayList<String>();
    private static String username;
    private static String password;


    
    public static void main(String[] args){

        System.out.println("Client avviato");


        try (Socket server = new Socket(hostname, hostport);
            DataOutputStream outToClient = new DataOutputStream(server.getOutputStream());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(server.getInputStream()))
            ){

            BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder request = null;
            String reqType = null;

            do{
                
                System.out.print("< ");
                String line = commandLine.readLine();
                StringTokenizer token;
                try{
                    token = new StringTokenizer(line);
                }
                catch(NullPointerException e){
                    continue;
                }

                reqType = token.nextToken();

                if(reqType.equals("listfollowers")){ // gestita localmente
                    for(String s : followers){
                        System.out.println("> " + s);
                    }
                    continue;
                }
                if(reqType.equals("register")){ // gestita tramite RMI
                    registerHandler(token);
                    continue;
                }

                request = new StringBuilder();
                request.append( line ).append(TER);
                
                outToClient.writeBytes(request.toString()); // da implementare login passivo

                StringBuilder response = new StringBuilder();
                response.append(inFromClient.readLine());  // prima  riga della risposta che contiene il numero di byte
                int remaining;  
                try{
                    remaining = Integer.parseInt(response.toString()); // byte rimaneneti
                }catch(Exception e){
                    throw new IOException();
                }
                                
                do{     
                    
                    response.append(inFromClient.readLine());
                    remaining = remaining - (response.length() + 2); // sottraggo anche il carattere newline
                    System.out.println("> "  +response);

                } while(remaining > 0); //leggo la risposta finche ho ancora byte da leggere
    
                if(reqType.equals("login") && response.toString().contains("OK")){   // se c'è stata una richiesta di login andata a buon fine
                    StringTokenizer newToken = new StringTokenizer(line);// parso username e password
                    newToken.nextToken();
                    username = newToken.nextToken();
                    password = newToken.nextToken();
                    registerForCallback();  // mi registro per la callback
                }
                if(reqType.equals("multicastaddress") && response.toString().contains("OK")){
                    StringTokenizer newToken = new StringTokenizer(response.toString());
                    String multicastAddr = newToken.nextToken();
                    int multicastPort = Integer.parseInt(newToken.nextToken());
                    (new MulticastReciver(multicastAddr, multicastPort)).start();
                }
                
            }while (! (reqType.equals("logout")));

            server.close();


        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
    }

    private static void registerForCallback(){
        FollowerServiceServer serverObject;

        try {
            Registry r = LocateRegistry.getRegistry(registryPort);
            serverObject = (FollowerServiceServer) r.lookup("followers");   // stub dell'oggetto che mi permette la registrazione
            
        } catch (RemoteException | NotBoundException | NullPointerException  e) {
            System.out.println("> Errore di comunicazione");
            e.printStackTrace();
            return; 
        }

        FollowersServiceClientImp followerServiceClientObj;
        FollowerServiceClient followerServiceClientStub;

        try {
            followerServiceClientObj = new FollowersServiceClientImp(followers);    // oggetto che permette al server di aggiornare la lista del client
            followerServiceClientStub = (FollowerServiceClient)UnicastRemoteObject.exportObject(followerServiceClientObj, 0);
        } catch (RemoteException e1) {
            System.out.println("> Errore di comunicazione");
            e1.printStackTrace();
            return;
        }

        try{
            serverObject.addFollowerServiceClient(username, password, followerServiceClientStub);   // mi registro alla callback dando al server l'oggetto che gli permette di aggiornarmi
        }catch(Exception | UserNotFoundException | NotAllowedException e){
            System.out.println("> Nome utente o password errati");
            return;
        }
        
        // sincronizza la lista locale di followers
        try {
            for (String s : serverObject.getFollower(username, password)){
                followers.add(s);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (UserNotFoundException | NotAllowedException e) {
            System.out.println("> Nome utente o password errati");
        } 
        
    }

    private static void registerHandler(StringTokenizer token){

        
        if(token.countTokens() < 3){
            System.out.println("> Uso: register <username> <password> <tags>");
            return;
        }

        String username = token.nextToken();
        String password = token.nextToken();
        ArrayList<String> tags = new ArrayList<String>();
        while(token.hasMoreTokens() && (tags.size() < 5)){
            tags.add(token.nextToken());
        }

        if(username.contains("#")){
            System.out.println("> L'username non può contenere il carattere '#'");
            return;
        }

        RegistrationService serverObject;

        try {
            Registry r = LocateRegistry.getRegistry(registryPort);
            
            serverObject = (RegistrationService) r.lookup("register");
            
        } catch (RemoteException | NotBoundException | NullPointerException  e) {
            System.out.println("> Errore di comunicazione");
            e.printStackTrace();
            return; 
        }
        
        try{
            serverObject.register(username, password, tags);
        }
        catch(Exception | UserAlrExiException | InvalidUsernameException e){
            System.out.println("> Questo utente già esiste");
            return;
        }

        System.out.println("> OK");
        
    }

}
