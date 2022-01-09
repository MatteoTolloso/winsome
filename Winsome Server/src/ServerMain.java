
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMain{
    
    private static Parametri parametri = new Parametri();
    
    public static void main(String args[]){
        
        
        System.err.println("Preparo l'avvio del server...");
        try{
            
            parametri.parseParametri("./config.txt");

            Runtime.getRuntime().addShutdownHook(new Terminazione(parametri.getDb(), parametri.getBackupFolder()));

            try{
                parametri.getDb().jsonRestore(parametri.getBackupFolder());
            }catch(InvalidPathException e){
                System.err.println("Nessuno stato di partenza");
            }

            startRMI();

            startMulticast();

            startDaemon();

            startSterver();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("impossibile avviare il server");
            System.exit(0);
        }
        
    }





    

    private static void startDaemon(){
        (new Daemon(parametri.getDb(), parametri.getMulticastAddr(), parametri.getMulticastPort(), parametri.getPeriodo(), parametri.getPercentualeAutore())).start();
    }

    private static void startMulticast() throws UnknownHostException, IllegalArgumentException{

        InetAddress multicastGroup = InetAddress.getByName(parametri.getMulticastAddr());
       
        if (!multicastGroup.isMulticastAddress()){
            throw new IllegalArgumentException();
        }
        

    }

    private static void startRMI(){

        try {
            RegistrationServiceImp registrationServiceObj = new RegistrationServiceImp(parametri.getDb());    // istanza dell'oggetto che permette la registrazione al social
            RegistrationService registrationServiceStub = (RegistrationService)UnicastRemoteObject.exportObject(registrationServiceObj, 0);  // esporto l'oggetto

            FollowersServiceServerImp followerServiceServerObj = new FollowersServiceServerImp(parametri.getDb(), parametri.getCallbackMap());
            FollowerServiceServer followerServiceServerStub = (FollowerServiceServer)UnicastRemoteObject.exportObject(followerServiceServerObj, 0);

            Registry registry = LocateRegistry.createRegistry(parametri.getRegistryPort());    // creo un registro

            registry.bind("register", registrationServiceStub);  // pubblico lo stub nel registro
            registry.bind("followers", followerServiceServerStub);

        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
            System.out.println("Impossibile avviare il server");
            System.exit(0);
        }

    }

    private static void startSterver(){

        ExecutorService pool = new ThreadPoolExecutor(4, 64, 3, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>());
        ServerSocket server;
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(parametri.getServerAddr(), parametri.getServerPort()));
            System.out.println("Server avviato");
			while (true) {
				Socket client = server.accept();
                System.out.println("nuova connessione");
				pool.execute(new ClientHandler(client, parametri.getDb(), parametri.getCallbackMap(), parametri.getMulticastAddr(),
                            parametri.getMulticastPort(), parametri.getTimeout()));
			}
            
		}
		catch(Exception e) {
			e.printStackTrace();
		}
        
		

        // TODO controllo della terminazione
        pool.shutdown();
        try {
            while(!pool.isTerminated())
                pool.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
		
	}
}

    /*

    public static void test() throws NullPointerException, UserAlrExiException, InvalidUsernameException, UserNotFoundException, PostNotFoundException, NotAllowedException{

        db.register("matteo", "password", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        db.register("pippo", "password", new ArrayList<String>(Arrays.asList("musica", "storia", "nuoto")));
        db.createPost("matteo", "testpost", "ciao");
        db.followUser("pippo", "matteo");
        db.ratePost("pippo", "matteo#1", true);


        


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
*/