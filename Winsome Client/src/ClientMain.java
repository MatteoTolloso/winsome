
import java.io.*;
import java.net.Socket;
import java.nio.file.InvalidPathException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ClientMain{

    private static Parametri parametri;

    private static String TER = "\r\n";
    private static ArrayList<String> followers = new ArrayList<String>();
    private static String username = null;
    private static String password = null;
    private static Socket server;
    private static DataOutputStream outToServer;
    private static BufferedReader inFromServer;

    
    public static void main(String[] args){

        System.out.println("Client avviato");

        parametri = new Parametri();

        try {
            parametri.parseParametri("config.txt");
        } catch (InvalidPathException | NumberFormatException | IOException e1) {
            System.err.println("Impossibile leggere i parametri di avvio");
            System.exit(0);
        }

        try {

            BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder request = null;
            String reqType = null;
            Boolean wasLogged = false;
            Boolean connected = false;

            do{
                
                System.out.print("< ");
                String line = commandLine.readLine();
                line = compactReq(line);
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

                if(!connected) {
                    connect();
                    connected = true;
                }

                request = new StringBuilder();
                request.append( line ).append(TER);

                try{
                    outToServer.writeBytes(request.toString());
                } catch(IOException e){ 
                   if(wasLogged && connected){   // se ero loggato provo a riconnetermi e riloggarmi
                        connect();
                        loginAgain();
                        registerForCallback();
                        // e poi proseguo con la richesta normale
                        outToServer.writeBytes(request.toString());
                   }
                   else throw new IOException();// se non ero loggato propago l'eccezione e termino
                }
                
                String responseReader = inFromServer.readLine();  // prima  riga della risposta che contiene il numero di byte
                int remaining;  
                try{
                    remaining = Integer.parseInt(responseReader.toString()); // byte rimaneneti
                }catch(Exception e){
                    throw new IOException();
                }

                StringBuilder responseFormatted = new StringBuilder();
                do{       
                    responseReader = inFromServer.readLine();
                    responseFormatted.append("> ").append(responseReader).append("\n");
                    remaining = remaining - (responseReader.length() + 2); // sottraggo anche il carattere newline
                } while(remaining > 0); //leggo la risposta finche ho ancora byte da leggere
                
                System.out.println(responseFormatted.toString());

                if(reqType.equals("login") && responseReader.contains("OK")){   // se c'è stata una richiesta di login andata a buon fine
                    StringTokenizer newToken = new StringTokenizer(line);// parso username e password dalla richiesta
                    newToken.nextToken();
                    username = newToken.nextToken();
                    password = newToken.nextToken();
                    wasLogged = true;
                    registerForCallback();  // mi registro per la callback
                }
                if(reqType.equals("multicast")){    // richesta di multicast
                    StringTokenizer newToken = new StringTokenizer(responseReader.toString());
                    String multicastAddr = newToken.nextToken();    // parso indirizzo e porta dalla risposta
                    int multicastPort = Integer.parseInt(newToken.nextToken());
                    (new MulticastReciver(multicastAddr, multicastPort)).start();// avvio un thread che ascolta il multicast
                }
                
            }while (! (reqType.equals("logout")));

            server.close();
            outToServer.close();
            inFromServer.close();
            System.exit(0);

        } catch (IOException e) {
            
            System.out.println("Disconnesso");
            System.exit(0);
        }
        
    }

    private static String compactReq(String req){

        try{
            if(req.contains("show post")){
                StringTokenizer token = new StringTokenizer(req);
                token.nextToken();
                token.nextToken();
                return "showpost" + " " + token.nextToken();
            }
            if(req.contains("list users")){
                return "listusers";
            }
            if(req.contains("list followers")){
                return "listfollowers";
            }
            if(req.contains("list following")){
                return "listfollowing";
            }
            if(req.contains("show feed")){
                return "showfeed";
            }
            if(req.contains("wallet btc")){
                return "walletbtc";
            }
        }catch(Exception e){
            
        }

        return req;
        
    }

    private static void connect() throws IOException{
        server = new Socket(parametri.getServerAddr(), parametri.getServerPort());
        outToServer = new DataOutputStream(server.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
    }

    private static void loginAgain() throws IOException{

        StringBuilder req = new StringBuilder();
        req.append("login").append(" ").append(username).append(" ").append(password).append("\n");
        outToServer.writeBytes(req.toString());

        String responseReader = inFromServer.readLine(); 
        int remaining;  
        try{
            remaining = Integer.parseInt(responseReader.toString()); // byte rimaneneti
        }catch(Exception e){
            throw new IOException();
        }
        StringBuilder responseFormatted = new StringBuilder();
        do{     
            responseReader = inFromServer.readLine();
            responseFormatted.append("> ").append(responseReader).append("\n");
            remaining = remaining - (responseReader.length() + 2);
        } while(remaining > 0);
        if(!responseReader.contains("OK")) throw new IOException();

    }

    private static void registerForCallback(){
        FollowerServiceServer serverObject;

        try {
            Registry r = LocateRegistry.getRegistry(parametri.getRegistryPort());
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
            Registry r = LocateRegistry.getRegistry(parametri.getRegistryPort());
            
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
