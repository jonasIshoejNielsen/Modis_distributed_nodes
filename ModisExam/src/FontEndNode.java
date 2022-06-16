import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FontEndNode {
    public static boolean useBetterPrints;
    public static boolean debugging;
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private AtomicInteger listeningPort;
    private Thread listenThread;

    //get/inc, listeningPort, (controlnodePort, controlNodeAddress ) pairs
    public static void main(String[] args) {
        new FontEndNode(args);
    }
    public FontEndNode(String[] args){
        listeningPort = new AtomicInteger(Integer.parseInt(args[1]));
        listenThread = new Thread(new Listen(listeningPort.get()));
        listenThread.start();
        Send(args, false);
    }
    Serializable message;
    private void  Send(String[] args, boolean resend){
        if(listeningPort.get() == 7014)
            stopped.set(stopped.get());
        if(args[0].toLowerCase().equals("get")) message = new ReplicaManager.GETMessage(listeningPort.get(), LocalDateTime.now());
        else if(!resend) message = new ReplicaManager.INCMessage(listeningPort.get(), LocalDateTime.now());
        for(int i=args.length-2; i>=2; i-=2){
            ReplicaManager.NodeAddress nodeAddress = new ReplicaManager.NodeAddress(Integer.parseInt(args[i]), args[i+1]);
            try {
                TCPMethods.send(nodeAddress, message);
            }
            catch (SocketException e){
                //was close, it will now move on to next
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        for(int i=0;i<10; i++) {
            try {
                if(stopped.get()) {
                    if(debugging)System.out.println(listeningPort +"   stopped sending");
                    return;
                }
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(debugging)System.out.println(listeningPort.get() +"   resend");
        Send(args, true);
    }

    public void  Stop(){
        stopped.set(true);
        new Thread(() -> {
            try {
                listenThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        try {
            TCPMethods.send("localhost", listeningPort.get(), "");
        } catch (IOException e) {
            //stoped before sending, great :)
        }
    }

    private class Listen implements Runnable {
        int listeningPort;
        public Listen(int listeningPort) {this.listeningPort = listeningPort;}
        public void run() {
            ServerSocket listeningServerSocket = null;
            Socket listeningSocket = null;
            while (true) {
                try {
                    while (true) {
                        if(stopped.get()) return;
                        listeningServerSocket = new ServerSocket(listeningPort);
                        listeningSocket = listeningServerSocket.accept();
                        ObjectInputStream dis = new ObjectInputStream(listeningSocket.getInputStream());
                        Object content = dis.readObject(); // blocking
                        if(stopped.get()) return;
                        if(useBetterPrints) System.out.println(listeningPort +"   "+content);
                        else System.out.println(content);
                        Stop();
                        if(debugging)System.out.println(listeningPort +"   stopped listening");
                        listeningServerSocket.close();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
