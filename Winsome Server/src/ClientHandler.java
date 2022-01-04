import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    
    private String NEW = "\r\n";   

    private Socket client;
    private String username = null;
    private int timeout = 0;    // da leggere dal file di configurazione
    private Database db;
    private ConcurrentHashMap<String, FollowerServiceClient> callbackMap;
    private String multicastAddr;
    private int multicastPort;


    public ClientHandler(Socket client, Database db, ConcurrentHashMap<String, FollowerServiceClient> callbackMap, String multicastAddr, int multicastPort){
        this.client = client;
        this.db = db;
        this.callbackMap = callbackMap;
        this.multicastAddr = multicastAddr;
        this.multicastPort =  multicastPort;
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
                    noValidHandler(outToClient);
                    continue;
                }

                System.out.println("Richesta ricevuta: " + requestMessageLine);

                StringTokenizer tokenizer = new StringTokenizer(requestMessageLine);
                requestType = tokenizer.nextToken();

                if((username == null) && (!requestType.equals("login") ) && (!requestType.equals("logout"))) {
                    noLoginHandler(outToClient);
                    continue;
                }

                switch(requestType){
                    case "login":{
                        loginHandler(tokenizer, outToClient);
                        break;
                    }
                    case "listusers":{
                        listusersHandler(outToClient);
                        break;
                    }
                    case "listfollowing":{
                        listfollowingHandler(outToClient);
                        break;
                    }
                    case "follow":{
                        followHandler(tokenizer, outToClient);
                        break;
                    }
                    case "unfollow":{
                        unfollowHandler(tokenizer, outToClient);
                        break;
                    }
                    case "blog":{
                        viewblogHandler(outToClient);
                        break;
                    }
                    case "post":{
                        postHaldler(requestMessageLine, outToClient);
                        break;
                    }
                    case "showfeed":{
                        showfeedHandler(outToClient);
                        break;
                    } 
                    case "showpost":{
                        showpostHandler(tokenizer, outToClient);
                        break;
                    }
                    case "delete":{
                        deleteHandler(tokenizer, outToClient);
                        break;
                    }
                    case "rewin":{
                        rewindpostHandler(tokenizer, outToClient);
                        break;
                    }
                    case "ratepost":{
                        ratepostHandler(tokenizer, outToClient);;
                        break;
                    }
                    case "comment":{
                        commentHandler(requestMessageLine, tokenizer, outToClient);
                        break;
                    }
                    case "wallet":{
                        walletHandler(outToClient);
                        break;
                    }
                    case "walletbtc":{
                        walletbtcHandler(outToClient);
                        break;
                    }
                    case "multicastabbress":{
                        multicastaddressHandler(outToClient);
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

    private void multicastaddressHandler(DataOutputStream outToClient)throws IOException{
        StringBuilder response = new StringBuilder();

        response.append(multicastAddr).append(" ").append(Integer.toString(multicastPort)).append(NEW);

        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
    }

    private void walletbtcHandler(DataOutputStream outToClient){
        //TODO
    }

    void walletHandler(DataOutputStream outToClient) throws IOException{
        StringBuilder response = new StringBuilder();

        try {
            response.append("Portafoglio: ").append(Double.toString( db.getWallet(username) ) ).append(NEW);
            for(Transaction t : db.getTransactions(username)){
                response.append("   Incremento: ").append(Double.toString( t.getIncremento()) ).append(", Timestamp: ").append(t.getTimestamp().get(Calendar.DATE)).append(NEW);
            }
        } catch (NullPointerException | UserNotFoundException e) { // impossibile
            throw new IOException();
        }

        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());

    }

    void commentHandler(String messageLine, StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{
        
        StringBuilder response = new StringBuilder();
        StringTokenizer tokenizer2 = new StringTokenizer(messageLine, "\"");

        if((tokenizer.countTokens() < 2 ) || (tokenizer2.countTokens() < 2)){
            response.append("Uso: comment <postId> \"<testo>\"").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        String postID = tokenizer.nextToken();
        tokenizer2.nextToken(); // prima parte del messaggio
        String contenuto = tokenizer2.nextToken();   // messaggio tra virgolette

        try {
            db.addComment(username, postID, contenuto);
        } catch (NullPointerException e) {
            throw new IOException();
        } catch (UserNotFoundException e) {
            throw new IOException();
        } catch (PostNotFoundException e) {
            response.append("Il post non esiste o e' stato eliminato").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        } catch (NotAllowedException e) {
            response.append("Non puoi commentare questo post").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;

    }
    void ratepostHandler(StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{
        StringBuilder response = new StringBuilder();

        if (tokenizer.countTokens() < 2){
            response.append("Uso: rate <idPost> <voto>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }
        String postID = tokenizer.nextToken();
        String voteStr = tokenizer.nextToken();
        int vote;
        try{
            vote= Integer.parseInt(voteStr);
        }
        catch (Exception e){
            response.append("Il voto può essere +1 o -1").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        try {
            db.ratePost(username, postID, (vote > 0) ? true : false);
        } catch (NullPointerException | UserNotFoundException e) {  // impossibile
            e.printStackTrace();
            throw new IOException();
        } catch (PostNotFoundException e) {
            response.append("Il post non esiste o e' stato rimosso").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        } catch (NotAllowedException e) {
            response.append("Non puoi votare questo post").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;

    }

    void rewindpostHandler(StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();

        if (tokenizer.countTokens() < 1){
            response.append("Uso: rewind <idPost>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }
        String postID = tokenizer.nextToken();

        try {
            db.rewinPost(username, postID);
        } catch (NullPointerException e) {
            
            e.printStackTrace();
        } catch (UserNotFoundException e) { // impossibile  
            e.printStackTrace();
            throw new IOException();
        } catch (PostNotFoundException e) { 
            response.append("Il post non esiste o è stato rimosso").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        } catch (NotAllowedException e) {
            response.append("Questo post non è nel tuo feed").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;


    }

    void deleteHandler(StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{
        
        StringBuilder response = new StringBuilder();

        if (tokenizer.countTokens() < 1){
            response.append("Uso: delete <idPost>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }
        String postID = tokenizer.nextToken();

        try {

            Post p = db.showPost(postID);
            if(! p.getAuthor().equals(username)){
                response.append("Non puoi eliminare questo post perche' non sei l'autore").append(NEW);
                response.insert(0, Integer.toString(response.length()) + NEW );
                outToClient.writeBytes(response.toString());
                return;
            }

            db.deletePost(postID);

        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new IOException();
        } catch (PostNotFoundException e) {
            response.append("Il post richiesto non esiste").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }
        
        if(response.toString().length() == 0) response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());


    }

    void showpostHandler(StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();

        if (tokenizer.countTokens() < 1){
            response.append("Uso: show post <idpost>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        String postID = tokenizer.nextToken();

        Post p;

        try {
            p = db.showPost(postID);
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new IOException();
        } catch (PostNotFoundException e) {
            response.append("Il post richiesto non esiste").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        response.append("--------").append(NEW);
        response.append("ID: ").append(p.getID()).append(NEW);
        response.append("Autore: ").append(p.getAuthor()).append(NEW);
        response.append("Titolo: ").append(p.getTitle()).append(NEW);
        response.append("Contenuto: ").append(p.getBody()).append(NEW);
        response.append("Voti positivi: ").append( Integer.toString( p.getUpVote().size() )).append(NEW);
        response.append("Voti negativi: ").append( Integer.toString( p.getDownVote().size() )).append(NEW);
        response.append("Commenti: ").append(NEW);
        for(Comment c : p.getComments()){
            response.append("   ").append(c.getUsername()).append(": ").append(c.getBody()).append(NEW);
        }
        response.append("--------").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());

    }

    void showfeedHandler(DataOutputStream outToClient)throws IOException{

        StringBuilder response = new StringBuilder();

        ArrayList<Post> posts;

        try {
            posts = db.showFeed(username);
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
            throw new IOException();
        }

        for(Post p : posts){
            response.append("--------").append(NEW);
            response.append("ID: ").append(p.getID()).append(NEW);
            response.append("Autore: ").append(p.getAuthor()).append(NEW);
            response.append("Titolo: ").append(p.getTitle()).append(NEW);
            response.append("--------").append(NEW);
        }
        
        if(response.toString().length() == 0) response.append("Nessun post nel feed").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;

    }

    void postHaldler(String messageLine, DataOutputStream outToClient) throws IOException{  // da testare

        StringBuilder response = new StringBuilder();

        StringTokenizer tokenizer = new StringTokenizer(messageLine,"\"" );

        tokenizer.nextToken();// dovrebbe essere la richiesta "post" 

        if (tokenizer.countTokens() < 3){
            response.append("Uso: post \"<titolo>\" \"<contenuto>\"").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        String title = tokenizer.nextToken();
        tokenizer.nextToken();// dovrebbe essere la stringa con lo spazio
        String content = tokenizer.nextToken();

        if(title.length() > 20){
            response.append("Il titolo non può superare 20 caratteri").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }
        if(content.length() > 500){
            response.append("Il contenuto non può superare 500 caratteri").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        try {
            db.createPost(username, title, content);
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
            throw new IOException();
        }

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;

    }

    void viewblogHandler(DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();

        ArrayList<Post> posts;
        
        try {
            posts = db.viewBlog(username);
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
            throw new IOException();
        }

        for(Post p: posts){
            response.append("--------").append(NEW);
            response.append("ID: ").append(p.getID()).append(NEW);
            response.append("Autore: ").append(p.getAuthor()).append(NEW);
            response.append("Titolo: ").append(p.getTitle()).append(NEW);
            response.append("--------").append(NEW);
        }

        if(response.toString().length() == 0) response.append("Nessun post nel blog").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;

    }

    void unfollowHandler(StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{
        StringBuilder response = new StringBuilder();

        if(tokenizer.countTokens() < 1){    // argomenti errati
            response.append("Uso: unfollow <username>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        String toUnFollow = tokenizer.nextToken();

        if(!db.existsUser(toUnFollow)){
            response.append("Utente inesistente").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        if(username.equals(toUnFollow)){
            response.append("Non puoi smettere di seguire te stesso").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        try {
            db.unfollowUser(username, toUnFollow);
        } catch (NullPointerException | UserNotFoundException  e) {
            e.printStackTrace();
            throw new IOException();
        }

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());

        // notifica l'utente che viene smesso di seguire

        try{
            callbackMap.get(toUnFollow).removeFollower(username);
        }
        catch(RemoteException e){
            e.printStackTrace();
        }catch(NullPointerException e){

        }
        

        return;
    }

    void followHandler(StringTokenizer tokenizer, DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();

        if(tokenizer.countTokens() < 1){    // argomenti errati
            response.append("Uso: follow <username>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        String toFollow = tokenizer.nextToken();

        if(!db.existsUser(toFollow)){
            response.append("Utente inesistente").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        if(username.equals(toFollow)){
            response.append("Non puoi seguire te stesso").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        try {
            db.followUser(username, toFollow);
        } catch (NullPointerException | UserNotFoundException | InvalidUsernameException e) {
            e.printStackTrace();
            throw new IOException();
        }

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());


        // bisogna notificare toFollow che ha un nuovo follower

    
        try{
            callbackMap.get(toFollow).addFollower(username);
        }
        catch(RemoteException e){
            e.printStackTrace();
        }
        catch(NullPointerException e){
            
        }
    

        return;

    }

    void listfollowingHandler(DataOutputStream outToClient) throws IOException{
        
        StringBuilder response = new StringBuilder();
        ArrayList<String> users;
        
        try {
            users = db.listFollowing(username);
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
            throw new IOException();
        }

        for(String u : users){
            response.append(u).append(NEW);
        }

        if(response.toString().length() == 0) response.append("Nessun utente seguito").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW ); 
        outToClient.writeBytes(response.toString());

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
                response.append(NEW);
            }
        } 
        catch (UserNotFoundException | Exception e) {   // dovrebbe essere impossibile perchè l'utente è loggato
            e.printStackTrace();
            throw new IOException();
        }

        if(response.toString().length() == 0) response.append("Nessun utente affine").append(NEW);

        response.insert(0, Integer.toString(response.length()) + NEW ); 

        outToClient.writeBytes(response.toString());

        System.out.println(response.toString());

        
    }

    void noLoginHandler(DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();
        response.append("Devi prima effettuare il login").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        
    }

    void noValidHandler(DataOutputStream outToClient) throws IOException{

        StringBuilder response = new StringBuilder();
        response.append("Comando non disponibile").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        
    }

    void loginHandler(StringTokenizer tokenizer, DataOutputStream outToClient)throws IOException{

        StringBuilder response = new StringBuilder();

        if(tokenizer.countTokens() < 2){    // argomenti errati
            response.append("Uso: login <username> <password>").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        String username = tokenizer.nextToken();
        String password = tokenizer.nextToken();

        if(!db.existsUser(username)){   // utente non esiste
            response.append("Utente non registrato, effettua la registrazione").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        if(!db.getPassword(username).equals(password)){ // password errata
            response.append("Password errata").append(NEW);
            response.insert(0, Integer.toString(response.length()) + NEW );
            outToClient.writeBytes(response.toString());
            return;
        }

        this.username = username;   // questo thread si sta occupando di questo utente

        response.append("OK").append(NEW);
        response.insert(0, Integer.toString(response.length()) + NEW );
        outToClient.writeBytes(response.toString());
        return;

    }

}
