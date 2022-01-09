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
    
    // indirizzo e porta per le connessioni TCP
    private String serverAddr = "127.0.0.1";
    private int serverPort = 9999;


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
            }
        }
        inReader.close();
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
}
