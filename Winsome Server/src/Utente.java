
import java.util.ArrayList;

public class Utente {
    private String username;
    private String password;
    private ArrayList<String> tags; // tag dell'utente  
    private ArrayList<String> followers = new ArrayList<String>();    //followers dell'utente
    private ArrayList<String> following = new ArrayList<String>();    //following dell'utente
    private ArrayList<String> posts = new ArrayList<String>();  // post pubblicati
    private ArrayList<String> feed = new ArrayList<String>();
    private int postCounter = 0; // contatore dei post pubblicati, usato per avere un id univoco dei post (conta anche i post rimossi)
    private ArrayList<Transaction> transactions = new ArrayList<Transaction>(); // lista di transazioni
    private double wallet = 0;

    public Utente(String username, String password, ArrayList<String> tags){
        this.username = username;
        this.password = password;
        this.tags = new ArrayList<String>(tags);
    }

    public Utente(){
        
    }

    public void print(){
        System.out.println("Username: "+ username);
        System.out.println("Password: "+ password);
        System.out.println("Tags: "+ tags.toString());
        System.out.println("Followers: "+ followers.toString());
        System.out.println("Following: "+ following.toString());
        System.out.println("Feed: " + feed.toString());
        System.out.println("Wallet: " + wallet);
        System.out.println("Posts: " + posts.toString());
        
    }

    public synchronized String getUsername(){
        return this.username;
    }

    public synchronized void addFollower(String newFollower){
        if (newFollower == null) return;
        if(followers.contains(newFollower)) return; // impedisci duplicati
        followers.add(newFollower);
    }

    public synchronized void addFollowing(String newFollowing){
        if(newFollowing == null) return;
        if(following.contains(newFollowing)) return; // impedisci duplicati
        following.add(newFollowing);
    }

    public synchronized void removeFollower(String oldFollower){
        if (oldFollower == null) return;
        followers.remove(oldFollower);
    }

    public synchronized void removeFollowing(String oldFollowing){
        if (oldFollowing == null) return;
        following.remove(oldFollowing);
    }

    public synchronized ArrayList<String> getFollowing(){
        return new ArrayList<String>(this.following);
    }

    public synchronized ArrayList<String> getFollowers(){
        return new ArrayList<String>(this.followers);
    }

    public synchronized ArrayList<String> getTags(){    // inutile rendere concorrente se creo la tagsMap
        return new ArrayList<String>(this.tags);
    }

    public synchronized ArrayList<String> getAllPost(){   // da rendere concorrente??
        return new ArrayList<String>(this.posts);
    }

    public synchronized int addAndGetPostCounter(){
        postCounter++;
        return postCounter;
    }

    public synchronized void addPost(String postID){
        if (postID == null) return;
        posts.add(postID);
    }

    public synchronized void removePost(String postID){
        if(postID == null) return;
        posts.remove(postID);
    }

    public synchronized void addToFeed(String postID){
        if(postID == null) return;
        if(!isInFeed(postID)){  // evita duplicati
            feed.add(postID);
        }
    }

    public synchronized Boolean isInFeed(String postID){
        if(postID == null) return false;
        return feed.contains(postID);
    }

    public synchronized ArrayList<String> getFeed(){
        return new ArrayList<String>(this.feed); 
    }

    public synchronized double getWallet(){
        return this.wallet;
    }

    public synchronized ArrayList<Transaction> getTransactions(){
        return new ArrayList<Transaction>(this.transactions);
    }
}
