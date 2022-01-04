import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MulticastReciver extends Thread{

    private String multicastAddr;
    private int multicastPort;
    private InetAddress group;
    private int LEN = 2048;

    public MulticastReciver(String addr, int port) throws UnknownHostException{
        this.multicastAddr = addr;
        this.multicastPort = port;
        this.group = InetAddress.getByName(multicastAddr);
    }

    public void run(){
        
        while (true){
            try(
                MulticastSocket multSock = new MulticastSocket(this.multicastPort);
                ){
                
                multSock.joinGroup(this.group);
                DatagramPacket dat = new DatagramPacket(new byte[LEN], LEN);
                multSock.receive(dat);
                System.out.println("> Comunicazione dal server: " + new String(dat.getData(), dat.getOffset(), dat.getLength()));

            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}