
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Database {
    // username --> Utente
    private ConcurrentHashMap<String, Utente> usersMap = new ConcurrentHashMap<String, Utente>(); 
    // idPost --> Post 
    private ConcurrentHashMap<String, Post> postMap = new ConcurrentHashMap<String, Post>();
    
    // da implementare: tagsMap <String, Utente>
    // da migliorare: per le funzioni showFeed e viewBlog non è necessario ritornare tutto il contenuto del post 

    private ReentrantLock regLock = new ReentrantLock();    // lock per la registrazione

    public Database(){

    }

    // registra un nuovo utente nel social
    public void register(String username, String password, ArrayList<String> tags) throws NullPointerException, UserAlrExiException, InvalidUsernameException{ // permette la registrazione di un nuovo utente
        
        if(username == null || password == null || tags == null) throw new NullPointerException();

        if(username.contains("#")){
            throw new InvalidUsernameException();
        }
        
        try{ 
            regLock.lock(); // può registrarsi ( e cancellarsi) un utente solo alla volta
    
            if(usersMap.containsKey(username)){ //utente esiste già
                throw new UserAlrExiException();
            }
            else{
                Utente newUtente = new Utente(username, password, tags);  // crea il nuovo utente
                usersMap.put(username, newUtente);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        finally{
            regLock.unlock();
        }

        return;
    }

    // ritorna una lista contenente gli utenti che hanno almeno un tag in comune con l'utente <<reqFrom>>
    public ArrayList<String> listUsers(String reqFrom) throws NullPointerException, UserNotFoundException{

        if(reqFrom == null) throw new NullPointerException();

        ArrayList<String> utentiAffini = new ArrayList<String>();
        ArrayList<String> targetTags;
        
        try{
            targetTags =  usersMap.get(reqFrom).getTags(); // tag dell'utente che ha fatto la richesta
        }
        catch(NullPointerException e){  // l'utente che ha fatto la richesta non è presente come chiave
            throw new UserNotFoundException();
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }

        Iterator<Utente> allUsersIter = usersMap.values().iterator();   // iteratore su tutti gli utenti del social
        
        try{
            while( allUsersIter.hasNext()){

                Utente currUtente = allUsersIter.next();

                if(currUtente.getUsername().equals(reqFrom)) continue;

                for(String s : targetTags){
                    if(currUtente.getTags().contains(s)){
                        utentiAffini.add(currUtente.getUsername());
                        break; // esci dal loop for
                    }
                }
            }
        }
        catch(NoSuchElementException e){
            // l'iteratore non ha più elementi
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return utentiAffini;
    }

    // ritorna una lista contente gli utenti di cui <<reqFrom>> è follower
    public ArrayList<String> listFollowing(String reqFrom) throws NullPointerException, UserNotFoundException{

        if (reqFrom == null) throw new NullPointerException();

        if (!usersMap.containsKey(reqFrom)) throw new UserNotFoundException();

        return usersMap.get(reqFrom).getFollowing(); // la clonazione viene fatta nel metodo della classe Utente

    }
   
    // registra che l'utente <<reqFrom>> inizia a seguire l'utente <<toFollow>>
    public void followUser(String reqFrom, String toFollow) throws NullPointerException, UserNotFoundException, InvalidUsernameException{

        if(reqFrom == null || toFollow == null) throw new NullPointerException();

        if(reqFrom.compareTo(toFollow) == 0) throw new InvalidUsernameException();  // impedisce ad un utente di seguire se stesso

        if(!usersMap.containsKey(reqFrom) || !usersMap.containsKey(toFollow)){
            throw new UserNotFoundException();
        }

        usersMap.get(toFollow).addFollower(reqFrom); // l'utente seguito ha un nuovo follower
        usersMap.get(reqFrom).addFollowing(toFollow);// l'utente che segue ha un nuovo following

        ArrayList<String> posts = usersMap.get(toFollow).getAllPost();  // post dell'utente che si inizia a seguire
        for(String p : posts){
            usersMap.get(reqFrom).addToFeed(p); // li inserisco tutti nel feed così si possono vedere anche i post precedenti
        }

    }

    // registra che l'utente <<reqFrom>> smette di seguire l'utente <<toUnFollow>>
    public void unfollowUser(String reqFrom, String toUnFollow) throws NullPointerException, UserNotFoundException{

        if(reqFrom == null || toUnFollow == null) throw new NullPointerException();

        if(!usersMap.containsKey(reqFrom) || !usersMap.containsKey(toUnFollow)){
            throw new UserNotFoundException();
        }

        usersMap.get(reqFrom).removeFollowing(toUnFollow); // <<reqFrom>> smette di seguire <<toUnFollow>>
        usersMap.get(toUnFollow).removeFollower(reqFrom);  // <<toUnFollow>> non ha più <<reqFrom>> tra i followers

    }

    // ritorna la lista di post che l'utente <<reqFrom>> ha sul proprio blog
    public ArrayList<Post> viewBlog(String reqFrom) throws NullPointerException, UserNotFoundException{

        if(reqFrom == null) throw new NullPointerException();

        if(!usersMap.containsKey(reqFrom)) throw new UserNotFoundException();

        ArrayList<String> postIDs = usersMap.get(reqFrom).getAllPost(); // la clone viene effettuata dalla classe Utente

        ArrayList<Post> posts = new ArrayList<Post>();

        for (String s : postIDs){
            
            try{
                posts.add( new Post ( postMap.get(s) )  );  // recupero il post attraverso la mapPost, effettuo una clone e lo aggiungo alla lista da ritornare
            } 
            catch(NullPointerException e){
                // vuol dire che <<reqFrom>> ha fatto il rewind di un post che successivamente è stato eliminato dal suo autore
            }  
        }

        return posts;

    }
    
    // crea un nuovo post con un id univoco e lo inserisce nel blog di <<reqFrom>> e nella postMap
    public void createPost(String reqFrom, String titolo, String contenuto) throws NullPointerException, UserNotFoundException{

        if(reqFrom == null || titolo == null || contenuto == null) throw new NullPointerException();

        if(!usersMap.containsKey(reqFrom)) throw new UserNotFoundException();   // sono sicuro che l'utente esiste quindi riuscirò ad inserire il post

        String postCounter = Integer.toString( usersMap.get(reqFrom).addAndGetPostCounter() ); // ottieni e aumenta il contatore di post dell'utente
        String id = reqFrom + "#" + postCounter;   // ottengo un id univoco perchè l'username è univoco e il post counter di un utente può solo aumentare

        Post newPost = new Post(id, reqFrom, titolo, contenuto);

        usersMap.get(reqFrom).addPost(id); // aggiungo il post ( solo l'id) al blog dell'utente che l'ha scritto
        postMap.put(id, newPost);   // aggiungo il post alla map dei post

        ArrayList<String> followers = usersMap.get(reqFrom).getFollowers(); //followers del creatore del post

        for(String f : followers){  // metti il post in tutti i feed
            usersMap.get(f).addToFeed(id);
        }
    }
    
    // ritorna il post avente come id <<postID>>
    public Post showPost(String postID) throws NullPointerException, PostNotFoundException{

        if (postID == null) throw new NullPointerException();

        if (!postMap.containsKey(postID)) throw new PostNotFoundException();

        return new Post ( postMap.get(postID) );
    }

    // elimina un post <<postID>>
    public void deletePost(String postID ) throws NullPointerException, PostNotFoundException{

        if (postID == null) throw new NullPointerException();

        if (!postMap.containsKey(postID)){
            throw new PostNotFoundException();
        }

        
        postMap.remove(postID);
        
        //String username = postID.substring(0, postID.indexOf("#"));
        // usersMap.get(username).removePost(postID);

        // non rimuovo il post dalla lista di postID del creatore, perchè voglio che rimanga una minima traccia dell'esisteza di quel post, 
        // inoltre in questo modo mantengo la coerenza con il fatto che nemmeno gli id dei post rewind e poi eliminati vengono eliminati
        // le eccezioni che questi post eliminati generano vengono gestite con semplicità nei metodi viewBlog e showPost 

        // andare ad eliminare completamente tutte le tracce del post vorrebbe dire eliminarlo da tutti i feed dei followers e se qualcuno di
        // essi ha fatto il rewind, anche da tutti i rispettivi followers

    }

    // ritorna il feed dell'utente che ha fatto la richiesta
    public ArrayList<Post> showFeed(String reqFrom) throws NullPointerException, UserNotFoundException{

        if (reqFrom == null) throw new NullPointerException();

        if(!usersMap.containsKey(reqFrom)) throw new UserNotFoundException();

        ArrayList<String> feedIDs = usersMap.get(reqFrom).getFeed();
        ArrayList<Post> feed = new ArrayList<Post>();

        for(String post : feedIDs){
            try{
                feed.add(postMap.get(post));
            }
            catch(NullPointerException e){
                // il post è stato eliminato
            }
        }

        feed.sort( (Post p1, Post p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()) );

        return feed;


        /*
        ArrayList<String> following = usersMap.get(reqFrom).getFollowing(); // following dell'utente
        ArrayList<Post> feed = new ArrayList<Post>();

        // lineare in numeroFollowing + numeroPostFollowing
        for( String userId : following){ // per ogni utente che segue
            
            ArrayList<String> toAdd = usersMap.get(userId).getAllPost(); // prendo l'id di tutti i post che ha sul suo blog

            for (String postId : toAdd){    // aggiungo ogni post al feed

                try{
                feed.add(postMap.get(postId));
                }
                catch(NullPointerException e){
                    // il post è stato eliminato
                }
            }
        }

        feed.sort( (Post p1, Post p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()) );

        return feed;

        */
    }

    // rewin di un post, cioè aggiungilo al proprio blog
    public void rewinPost(String reqFrom, String postID) throws NullPointerException, UserNotFoundException, PostNotFoundException, NotAllowedException{

        if(reqFrom == null || postID == null) throw new NullPointerException();

        Utente usr;
        try{
            usr = usersMap.get(reqFrom);
        } catch(Exception e){
            throw new UserNotFoundException();
        }
        Post pst;
        try{
            pst = postMap.get(postID);
        } catch(Exception e){
            throw new PostNotFoundException();
        }

        if(! usr.isInFeed(postID)) throw new NotAllowedException();   // il post deve essere nel proprio feed

        usr.addPost(postID);  // aggiungo il post al blog dell'utente

        ArrayList<String> followers = usr.getFollowers(); // followers dell'utente
        String authorUsername = pst.getAuthor();    // autore originale del post

        for (String f : followers){ // metto il post nel feed di tutti i followers di <<reqFrom>>
            if(f.equals(authorUsername)){  //tranne in quello dell'autore stesso 
                usersMap.get(f).addToFeed(postID);
            }
        }

    }

    // aggiungi un voto al post
    public void ratePost(String reqFrom, String postID, Boolean voto) throws NullPointerException, UserNotFoundException, PostNotFoundException, NotAllowedException{

        if(reqFrom == null || postID == null || voto == null) throw new NullPointerException();

        Utente usr;
        try{
            usr = usersMap.get(reqFrom);
        } catch(Exception e){
            throw new UserNotFoundException();
        }
        Post pst;
        try{
            pst = postMap.get(postID);
        } catch(Exception e){
            throw new PostNotFoundException();
        }

        if (!usr.isInFeed(postID)) throw new NotAllowedException();  // il post non è nel proprio feed

        if( pst.hasVote(reqFrom)) throw new NotAllowedException(); // l'utente ha già votato quel post

        if(pst.getAuthor().equals(reqFrom)) throw new NotAllowedException(); // l'utente sta votando un proprio post

        if(voto){   // voto positivo
            pst.addUpVote(reqFrom);
        }
        else{   // voto negativo
            pst.addDownVote(reqFrom);
        }
    }

    // aggiungi un commento al post
    public void addComment(String reqFrom, String postID, String comment) throws NullPointerException, UserNotFoundException, PostNotFoundException, NotAllowedException{

        if(reqFrom == null || postID == null || comment == null) throw new NullPointerException();

        Utente usr;
        try{
            usr = usersMap.get(reqFrom);
        } catch(Exception e){
            throw new UserNotFoundException();
        }
        Post pst;
        try{
            pst = postMap.get(postID);
        } catch(Exception e){
            throw new PostNotFoundException();
        }

        if(!usr.isInFeed(postID)) throw new NotAllowedException();  // il post non è nel feed dell'utente

        if(pst.getAuthor().equals(reqFrom)) throw new NotAllowedException(); // l'autore del post vuole inserire un commento

        pst.addComment(reqFrom, comment);
           

    }
    
    // ottieni il wallet dell'utente
    public double getWallet(String reqFrom)throws NullPointerException, UserNotFoundException{

        if(reqFrom == null) throw new NullPointerException();

        Utente usr;
        try{
            usr = usersMap.get(reqFrom);
        } catch(Exception e){
            throw new UserNotFoundException();
        }

        return usr.getWallet();

    }
    
    // ottieni le trasazioni dell'utente
    public ArrayList<Transaction> getTransactions(String reqFrom) throws NullPointerException, UserNotFoundException{

        if(reqFrom == null) throw new NullPointerException();

        Utente usr;
        try{
            usr = usersMap.get(reqFrom);
        } catch(Exception e){
            throw new UserNotFoundException();
        }

        return usr.getTransactions();   // clone fatta da Utente
    }
    
    // debug del database
    public void print(){    // stampa il database per debug
        Collection<Utente> users =  usersMap.values();
        for(Utente u : users){
            System.out.println("------------------------------");
            u.print();
            if(true){ // stampa anche i post
                ArrayList<String> postIDs = u.getAllPost();
                for(String s : postIDs){
                    System.out.println("    ---------");
                    try{
                        postMap.get(s).print();
                    }
                    catch(NullPointerException e){
                        System.out.println("post eliminato");
                        // il post è stato eliminato
                    }
                }
            }
        }
    }
}
