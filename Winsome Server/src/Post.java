
import java.util.ArrayList;
import java.util.Calendar;

public class Post {
    private String id;  // identificatore univoco
    private String author;  // username dell'autore
    private String title;
    private String body;
    private ArrayList<String> upVote = new ArrayList<String>();
    private int upVoteIndex = 0;
    private ArrayList<String> downVote = new ArrayList<String>();
    private int downVoteIndex = 0;
    private ArrayList<Comment> comments = new ArrayList<Comment>();
    private int commentsIndex = 0;
    private Calendar timestamp = Calendar.getInstance();

    private int iterations = 0;

    public Post(String id, String author, String title, String body){
        this.id = id;
        this.author = author;
        this.title = title;
        this.body = body;
    }

    public Post(){

    }

    public Post(Post p){
        this.id = p.getID();
        this.author = p.getAuthor();
        this.title = p.getTitle();
        this.body = p.getBody();
        this.upVote = p.getUpVote();
        this.downVote = p.getDownVote();
        this.comments = p.getComments();
        this.timestamp = p.getTimestamp();
    }

    public void print(){
        System.out.println("    id: "+ id);
        System.out.println("    author: "+author);
        System.out.println("    title: "+title);
        System.out.println("    body: "+body);
        System.out.println("    upvote: "+ upVote.toString());
        System.out.println("    downvote: "+ downVote.toString());
        System.out.println("    comments: ");
        for(Comment c : comments) {
            System.out.println("        ----");
            c.print();
        }
        //System.out.println("timestamp: " + timestamp.toString());
    }

    public synchronized String getID(){
        return this.id;
    }
    public synchronized String getAuthor(){
        return this.author;
    }
    public synchronized String getTitle(){
        return this.title;
    }
    public synchronized String getBody(){
        return this.body;
    }
    public synchronized ArrayList<String> getUpVote(){
        return new ArrayList<String>(this.upVote);
    }
    public synchronized ArrayList<String> getDownVote(){
        return new ArrayList<String>(this.downVote);
    }
    public synchronized ArrayList<Comment> getComments(){
        return new ArrayList<Comment>(this.comments);
    }
    public synchronized Calendar getTimestamp(){
        return (Calendar)this.timestamp.clone();
    }
    public synchronized Boolean hasVote(String userID){
        if(userID == null) return false;
        return upVote.contains(userID) || downVote.contains(userID);
    }
    public synchronized void addUpVote(String userID){
        if(userID == null) return;
        upVote.add(userID);
    }
    public synchronized void addDownVote(String userID){
        if(userID == null) return;
        downVote.add(userID);
    }
    public synchronized void addComment(String userID, String comment){
        if(userID == null || comment == null) return;
        comments.add(new Comment(userID, comment));
    }
    public synchronized ArrayList<String> getNewUpvote(){
        ArrayList<String> newUpvote =  new ArrayList<String>(upVote.subList(upVoteIndex, upVote.size()));
        upVoteIndex = upVote.size();
        return newUpvote;
    }
    public synchronized ArrayList<String> getNewDownvote(){
        ArrayList<String> newDownVote =  new ArrayList<String>(downVote.subList(downVoteIndex, downVote.size()));
        downVoteIndex = downVote.size();
        return newDownVote;
    }
    public synchronized ArrayList<Comment> getNewComments(){
        ArrayList<Comment> newComments =  new ArrayList<Comment>(comments.subList(commentsIndex, comments.size()));
        commentsIndex = comments.size();
        return newComments;
    }
    public synchronized int incrAndGetIteration(){
        return ++iterations;
    }

}
