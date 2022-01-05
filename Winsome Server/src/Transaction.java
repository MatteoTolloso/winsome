
import java.util.Calendar;

public class Transaction {
    private double incremento;
    private Calendar timestamp = Calendar.getInstance();

    public Transaction(double incr){
        this.incremento = incr;
    }

    public Transaction(){
        
    }

    public double getIncremento(){
        return incremento;
    }
    public Calendar getTimestamp(){
        return (Calendar)timestamp.clone();
    }

}
