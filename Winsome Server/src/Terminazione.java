import java.io.IOException;
import java.nio.file.InvalidPathException;

public class Terminazione extends Thread{
    private Database db;
    private String backupFolder;

    public Terminazione(Database db, String backupFolder){
        this.db = db;
        this.backupFolder = backupFolder;
    }

    public void run(){
        System.err.println("Operazioni di terminazione...");

        try {
            db.jsonBackup(backupFolder);
        } catch (InvalidPathException | NullPointerException | IOException e) {
            e.printStackTrace();
            System.err.println("Non e' stato possibile eseguire il backup finale");
        }

        System.err.println("Arrivederci!");
    }
    
}
