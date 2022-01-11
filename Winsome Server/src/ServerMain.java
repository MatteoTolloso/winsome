
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
    
    //contiene tutti i parametri e attributi necessari al server
    private static Parametri parametri = new Parametri();
    
    public static void main(String args[]){

        if(args.length < 1){
            System.err.println("Devi passare come argomento da linea di comando il path del file di configurazione");
            System.exit(0);
        }
          
        System.err.println("Preparo l'avvio del server...");
        try{
            
            parametri.parseParametri(args[0]);  // inizializza i parametri

            Runtime.getRuntime().addShutdownHook(new Terminazione(parametri.getDb(), parametri.getBackupFolder())); // configura funzione di terminazione

            try{
                parametri.getDb().jsonRestore(parametri.getBackupFolder()); // ripristina un backup precedente
            }catch(InvalidPathException e){
                System.err.println("Nessuno stato di partenza");
            }

            startRMI(); // avvia i servizi RMI

            startMulticast();   // verifica che l'idirizzo per il multicast sia consentito

            startDaemon();  // avvia il thread demone 

            startServer();  // avvia la ServerSocket e il threadpool
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

    private static void startServer(){

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