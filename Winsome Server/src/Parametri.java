import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.InvalidPathException;
import java.util.StringTokenizer;

public class Parametri {

    // porta del registro RMI (con indirizzo localhost)
    private int  registryPort = 8888;
    
    // periodo per il calcolo delle ricompense e backup
    private long periodo = 10000;
    
    // indirizzo e porta per il multicast UDP della notifia delle ricompese
    private String multicastAddr = "239.255.1.3";
    private int multicastPort = 9998;
    
    // indirizzo e porta per le connessioni TCP
    private String serverAddr = "127.0.0.1";
    private int serverPort = 9999;

    // timeout di attesa sulla socket TCP
    private  int timeout = 0;

    //cartella dove eseguire backup e ripristino dello stato del server
    String backupFolder = ".";

    public Parametri(){}

    public void parseParametri(String path) throws InvalidPathException, FileNotFoundException, IOException, NumberFormatException{

        File file = new File(path);
        if(!file.exists() || file.isDirectory() || !file.canRead()) throw new InvalidPathException(path, path);
        
        BufferedReader inReader =  new BufferedReader( new InputStreamReader(new FileInputStream(file)));
        
        String line;
        while( (line = inReader.readLine()) != null){
            if(line.length() == 0) continue; // ignora le righe vuote
            if(line.charAt(0) == '#') continue; // ignora le righe che iniziano con #

            StringTokenizer tokens = new StringTokenizer(line, "=");
            String type = tokens.nextToken();

            switch(type){
                case "registryPort":{
                    String regport = tokens.nextToken();
                    this.registryPort = Integer.parseInt(regport);
                    break;
                }
                case "perido":{
                    String per = tokens.nextToken();
                    this.periodo = Long.parseLong(per);
                    break;
                }
                case "multicastAddr":{
                    String multadd = tokens.nextToken();
                    this.multicastAddr = multadd;
                    break;
                }
                case "multicastPort":{
                    String multport = tokens.nextToken();
                    this.multicastPort = Integer.parseInt(multport);
                    break;
                }
                case "serverAddr":{
                    String servadd = tokens.nextToken();
                    this.serverAddr = servadd;
                    break;
                }
                case "serverPort":{
                    String servport = tokens.nextToken();
                    this.serverPort = Integer.parseInt(servport);
                    break;
                }
                case "timeout":{
                    String tout = tokens.nextToken();
                    this.timeout = Integer.parseInt(tout);
                    break;
                }
                case "backupFolder":{
                    String bckfol = tokens.nextToken();
                    this.backupFolder = bckfol;
                    break;
                }
            }
        }
        inReader.close();
    }

    public String getMulticastAddr() {
        return multicastAddr;
    }
    public int getMulticastPort() {
        return multicastPort;
    }
    public long getPeriodo() {
        return periodo;
    }
    public int getRegistryPort() {
        return registryPort;
    }
    public String getServerAddr() {
        return serverAddr;
    }
    public int getServerPort() {
        return serverPort;
    }
    public int getTimeout() {
        return timeout;
    }
}
