import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;

public class Daemon extends Thread {

    // calcolo delle ricompense, comunicazione attraverso multicast, backup in json


    private Database db;
    private double precetualeAutore = 0.7;
    private String multicastAddr;
    private int multicasPort;
    private long periodo;

    public Daemon(Database db, String multicastAddr, int multicasPort, long periodo){
        this.db = db;
        this.multicastAddr = multicastAddr;
        this.multicasPort = multicasPort;
        this.periodo = periodo;
    }

    public void run(){

        while(true){

            calcolaRicompense();

            inviaNotificaRicompense();

            try {
                db.jsonBackup(".");
            } catch (InvalidPathException | NullPointerException | IOException e1) {
                e1.printStackTrace();
            }
            
            System.out.println("Periodo terminato: ricomense calcolate, notifiche inviate, backup effettuato");
            
            try {
                Thread.sleep(periodo);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    private void calcolaRicompense(){

        for(Post p : db.getAllPosts()){ // ogni post nel database

            //System.err.println("DEBUG " + "valuto il post "+ p.getID() );

            // prima parte del numeratore
            ArrayList<String> new_people_upvote = p.getNewUpvote();
            ArrayList<String> new_people_downvote = p.getDownVote();
            double a = new_people_upvote.size() - new_people_downvote.size(); // like vale +1, dislike vale -1
            double b = a > 0 ? a : 0;   // se la somma è positiva ok, altrimenti prendo 0
            b = b + 1 ; // aggiungo 1
            
            //seconda parte del numeratore
            ArrayList<Comment> new_comments = p.getNewComments(); // tutti i commenti fatti dall'ultima iterazione
            ArrayList<String> new_people_commenting = removeDuplicateAndToString(new_comments); // ottieni un array di stringhe con i nomi delle persone che hanno commentato senza duplicati 
            double c = 0;
            for(String s : new_people_commenting){
                c = c + (2 / (1 + Math.pow(Math.E, -numbOfComments(s, new_comments) + 1  )  )  );
            }
            c = c + 1;

            // calcolo finale
            double numeratore = Math.log(b) + Math.log(c);
            double guadagno = numeratore / p.incrAndGetIteration();


            if(guadagno <= 0) continue; // il post non ha generato incassi

            // ricompensa l'autore del post
            double ricompensaAutore = guadagno*precetualeAutore;
            
            try {   
                db.addToWallet(p.getAuthor(), ricompensaAutore);
                //System.err.println("DEBUG " + "aggiungo  "+ Double.toString(ricompensaAutore) + " al portafoglio di  " + p.getAuthor() );

            } catch (NullPointerException | UserNotFoundException e1) {
                e1.printStackTrace();
            }

            // ricompenso i curatori
            ArrayList<String> curatori = new ArrayList<String>(new_people_upvote);  // persone che hanno messo upvote
            for(String s : new_people_commenting) if(!curatori.contains(s)) curatori.add(s);   // piu' percone che hanno commentato (no duplicati)

            if(curatori.size() == 0) continue;// in questo caso non c'è nessun curatore

            double ricompesaCuratore = (guadagno*(1-precetualeAutore)) / curatori.size();

            for(String utente : curatori){ // ricompensa i curatori che hanno commentato
                try {
                    db.addToWallet(utente, ricompesaCuratore);
                    //System.err.println("DEBUG " + "aggiungo  "+ Double.toString(ricompesaCuratore) + " al portafoglio di  " + utente );

                } catch (NullPointerException | UserNotFoundException e) {
                    // impossibile che un utente che ha commentato o messo like non sia iscritto
                    e.printStackTrace();
                }
            }
        }
    }

    private void inviaNotificaRicompense(){
        String notifica = "Sono state calcolate le ricompense dei post";
        try( 
            DatagramSocket sock = new DatagramSocket();
            ){

            DatagramPacket dat = new DatagramPacket(notifica.getBytes(), notifica.length(), InetAddress.getByName( multicastAddr ), multicasPort);

            sock.send(dat);
        }
        catch(Exception e){
            e.printStackTrace();
            System.err.println("impossibile inviare notifica ricompensa");
        }
        
    }


    private ArrayList<String> removeDuplicateAndToString(ArrayList<Comment> new_comments){
        ArrayList<String> new_people_commenting = new ArrayList<String>();

        for(Comment c : new_comments){
            if(!new_people_commenting.contains(c.getUsername())) new_people_commenting.add(c.getUsername());
        }

        return new_people_commenting;
    }

    private int numbOfComments(String s, ArrayList<Comment> new_comments){  // numero di commenti che un utente ha fatto di recente
        int count = 0;
        for(Comment c : new_comments){
            if(c.getUsername().equals(s)) count++;
        }

        return count;
    }
}

