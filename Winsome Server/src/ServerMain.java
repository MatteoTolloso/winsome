
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.InvalidPathException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMain{
    
    private static Database db = new Database(); 
    private static int  registryPort = 8888;
    private static RegistrationServiceImp obj;
    private static RegistrationService stub;
    
    public static void main(String args[]){

        System.out.println("Server avviato");

        test_createPost();
        test_followUser();

        /*
        try {
            db.jsonBackup(".");
            db.jsonRestore(".");
        } catch (InvalidPathException | NullPointerException | IOException e) {
            e.printStackTrace();
        }
        */

        //startRMIRegister();

        startSterver();

        db.print();
        
    }

    public static void startRMIRegister(){

        try {
            obj = new RegistrationServiceImp(db);    // istanza dell'oggetto che permette la registrazione al social
            
            stub = (RegistrationService)UnicastRemoteObject.exportObject(obj, 0);  // esporto l'oggetto

            Registry registry = LocateRegistry.createRegistry(registryPort);    // creo un registro

            registry.bind("register", stub);  // pubblico lo stub nel registro

        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
            System.out.println("Impossibile avviare il server");
            System.exit(0);
        }

    }

    public static void startSterver(){

        ExecutorService pool = new ThreadPoolExecutor(8, 64, 3, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>());
        
        try {
            ServerSocket server = new ServerSocket();
            server.bind(new InetSocketAddress("127.0.0.1", 9999));
		
			while (true) {
				Socket client = server.accept();
                System.out.println("nuova connessione");
				pool.execute(new ClientHandler(client, db));
			}			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
        
		

        // controllo della terminazione
        pool.shutdown();
        try {
            while(!pool.isTerminated())
                pool.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
		
	}
    

    public static void test_listUsers(){

        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        
        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("informatica", "canto", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e2) {
            e2.printStackTrace();
        }
        

       
        try {
            db.register("paperino", "qwerty", new ArrayList<String>(Arrays.asList("musica", "informatica", "sport")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }
        

        try {
            System.out.println(db.listUsers("pippo"));
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void test_followUser(){

        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        
        try {
            db.register("mario", "qwerty", new ArrayList<String>(Arrays.asList("chitarra", "pianoforte", "cucina")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }
       

        try {
            db.followUser("matteo", "mario");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

        try {
            db.followUser("matteo", "luca");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

    }

    public static void test_listFollowing(){
        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("informatica", "canto", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }
    
        try {
            db.register("paperino", "qwerty", new ArrayList<String>(Arrays.asList("musica", "informatica", "sport")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }
        

        try {
            db.followUser("matteo", "pippo");
        }catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
        e.printStackTrace();
         }
        
        try {
            db.followUser("matteo", "pippo");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

        try {
            db.followUser("matteo", "paperino");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

        try {
            db.followUser("matteo", "pluto");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

        try {
            db.followUser("pippo", "matteo");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }


    }

    public static void test_unfollowUser(){

        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

    
        try {
            db.register("mario", "qwerty", new ArrayList<String>(Arrays.asList("chitarra", "pianoforte", "cucina")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }
    

        try {
            db.followUser("matteo", "mario");
        } catch (NullPointerException | UserNotFoundException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.followUser("matteo", "mario");
        } catch (NullPointerException | UserNotFoundException| InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.unfollowUser("matteo", "mario");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.unfollowUser("mario", "matteo");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void test_createPost(){

        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.createPost("matteo", "secondo post", "ciaoo");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("calcio", "storia", "fisica")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.register("pluto", "qwerty", new ArrayList<String>(Arrays.asList("pizza", "storia", "pasta")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

    }

    public static void test_deletePost(){
        
        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }
            

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.createPost("matteo", "secondo post", "ciaoo");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.deletePost("matteo#1");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (PostNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void test_viewBlog(){
        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.createPost("matteo", "secondo post", "ciaoo");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            System.out.println(db.viewBlog("matteo"));
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void test_showFeed(){
        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.createPost("matteo", "secondo post", "ciaoo");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("latte", "biscotti", "cioccolato")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.followUser("pippo", "matteo");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

        ArrayList<Post> feed = null;

        try {
            feed = db.showFeed("pippo");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        for(Post p : feed) p.print();

    }

    public static void test_rewinPost(){
        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("latte", "biscotti", "cioccolato")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.followUser("pippo", "matteo");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }

        try {
            db.rewinPost("pippo", "matteo#1");
        } catch (NullPointerException | NotAllowedException |UserNotFoundException | PostNotFoundException e2) {
            e2.printStackTrace();
        }

        try {
            db.register("pluto", "qwerty", new ArrayList<String>(Arrays.asList("latte", "biscotti", "cioccolato")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.followUser("pluto", "pippo");
        } catch (NullPointerException | UserNotFoundException |InvalidUsernameException e) {
            e.printStackTrace();
        }


    }

    public static void test_ratePost(){

        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("latte", "biscotti", "cioccolato")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.followUser("pippo", "matteo");
        } catch (NullPointerException | UserNotFoundException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.ratePost("pippo", "matteo#1", false);
        } catch (NullPointerException | UserNotFoundException | PostNotFoundException | NotAllowedException e) {
            e.printStackTrace();
        }
        try {
            db.ratePost("pippo", "matteo#1", false);
        } catch (NullPointerException | UserNotFoundException | PostNotFoundException | NotAllowedException e) {
            e.printStackTrace();
        }
    }

    public static void test_addComment(){
        try {
            db.register("matteo", "qwerty", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.createPost("matteo", "primo post", "hello world, questo è il mio primo post");
        } catch (NullPointerException | UserNotFoundException e) {
            e.printStackTrace();
        }

        try {
            db.register("pippo", "qwerty", new ArrayList<String>(Arrays.asList("latte", "biscotti", "cioccolato")));
        } catch (NullPointerException | UserAlrExiException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.followUser("pippo", "matteo");
        } catch (NullPointerException | UserNotFoundException | InvalidUsernameException e1) {
            e1.printStackTrace();
        }

        try {
            db.addComment("pippo", "matteo#1", "che bel post!");
        } catch (NullPointerException | UserNotFoundException | PostNotFoundException | NotAllowedException e) {
            e.printStackTrace();
        }
    }

}