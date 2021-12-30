import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ClientHandler implements Runnable {
    
    private String NEW = "\r\n";   

    private Socket client;
    private String username = null;
    private int timeout = 0;    // da leggere dal file di configurazione
    private Database db;


    public ClientHandler(Socket client, Database db){
        this.client = client;
        this.db = db;
    }


    public void run(){

		try (   
            DataOutputStream outToClient = new DataOutputStream(client.getOutputStream());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()))
        ){
            client.setSoTimeout(timeout);
            String requestMessageLine;
            String requestType = null;

            do{
            
                requestMessageLine = inFromClient.readLine();

                if (requestMessageLine == null) {
                    System.out.println("stringa nulla");    // il client ha chiuso
                    break;
                }
                if(requestMessageLine.length() <= 0) {
                    System.out.println("messaggio vuoto");
                    continue;
                }

                System.out.println(requestMessageLine + requestMessageLine.length());

                StringTokenizer tokenizer = new StringTokenizer(requestMessageLine);
                requestType = tokenizer.nextToken();

                if((username == null) && (!requestType.equals("login"))) {
                    noLoginHandler(outToClient);
                    continue;
                }

                System.out.println("requestType: " + requestType);

                switch(requestType){
                    case "login":{
                        loginHandler(tokenizer, outToClient);
                        break;
                    }
                    case "listusers":{
                        listusersHandler(outToClient);
                        break;
                    }



                    default:{
                        noValidHandler(outToClient);
                        break;
                    }

                }
            



            }while(!requestType.equals("logout"));

            client.close();

            System.out.println("connessione terminata");

        } 
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("connessione terminata");

            return;
        }


    }

    void listusersHandler(DataOutputStream outToClient)throws IOException {

        StringBuilder response = new StringBuilder();

        ArrayList<String> users;
        try {
            users = db.listUsers(username);
            for(String u : users){
                response.append(u).append(": ");
                //System.out.println(u + "\n");
                for(String t : db.getTags(u)){
                    response.append(t).append(" ");
                    //System.out.println(t + " ");
                
                }
                response.append('\n');
            }
        } 
        catch (UserNotFoundException | Exception e) {   // dovrebbe essere impossibile perchè l'utente è loggato
            throw new IOException();
        }

        if(response.toString().length() == 0) response.append("Nessun utente affine").append(NEW);

        response.insert(0, Integer.toString(response.length()) + NEW ); // da aggiornare tutto il protocollo in modo che invii come prima line la size del messaggio

        outToClient.writeBytes(response.toString());

        System.out.println(response.toString());

        
    }

    void noLoginHandler(DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();
        response.append("Devi prima effettuare il login").append(NEW);
        outToClient.writeBytes(response.toString());
        
    }

    void noValidHandler(DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();
        response.append("Comando non disponibile").append(NEW);
        outToClient.writeBytes(response.toString());
        
    }

    void loginHandler(StringTokenizer tokenizer, DataOutputStream outToClient)throws IOException{

        StringBuilder response = new StringBuilder();

        if(tokenizer.countTokens() < 2){    // argomenti errati
            response.append("Uso: login <username> <password>").append(NEW);
            outToClient.writeBytes(response.toString());
            return;
        }

        String username = tokenizer.nextToken();
        String password = tokenizer.nextToken();

        if(!db.existsUser(username)){   // utente non esiste
            response.append("Utente non registrato").append(NEW);
            outToClient.writeBytes(response.toString());
            return;
        }

        if(!db.getPassword(username).equals(password)){ // password errata
            response.append("Password errata").append(NEW);
            outToClient.writeBytes(response.toString());
            return;
        }

        this.username = username;   // questo thread si sta occupando di questo utente

        response.append("OK").append(NEW);
        outToClient.writeBytes(response.toString());
        return;

    }

}
