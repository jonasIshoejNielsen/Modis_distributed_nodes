import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplicaManager {
    public static boolean debug = false;
    private int listeningPort;
    private ConcurrentLinkedQueue<Msg> messages;
    private AtomicInteger counter;
    private AtomicInteger ranking;
    private ConcurrentLinkedQueue<NodeAddress> otherControlNodes;
    private ConcurrentMap<String, Boolean> receivedINCMessages;
    private ConcurrentMap<String, Boolean> receivedGetMessages;


    private ArrayList<Thread> threads = new ArrayList<>();
    private AtomicBoolean stoped = new AtomicBoolean(false);

    public static void main(String[] args) {
        if(args.length %2 != 2 && args.length<2){
            System.out.println("Not valid input:");
            System.out.println("Need listening port, ranking, (port, address) pairs");
            return;
        }
        new ReplicaManager(args);

    }

    public ReplicaManager(String[] args) {
        listeningPort       = Integer.parseInt(args[0]);
        ranking             = new AtomicInteger(Integer.parseInt(args[1]));
        counter             = new AtomicInteger(0);
        messages            = new ConcurrentLinkedQueue();
        otherControlNodes   = new ConcurrentLinkedQueue();
        receivedINCMessages = new ConcurrentHashMap<>();
        receivedGetMessages = new ConcurrentHashMap<>();
        for(int i=2; i<args.length; i+=2){
            otherControlNodes.add(new NodeAddress(Integer.parseInt(args[i]), args[i+1]));
        }
        Thread handleMessageThread  = new Thread(new HandleMessage());
        handleMessageThread.start();
        Thread listenThread         = new Thread(new Listen());
        listenThread.start();

        threads.add(handleMessageThread);
        threads.add(listenThread);
    }

    public void stop() {
        stoped.set(true);
        for(Thread thread : threads){
            new Thread(() -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        try {
            TCPMethods.send("localhost", listeningPort, "");    //closing listening thread
        }catch (IOException e) {}
        try {
            Thread.sleep(1000);             //delay for testing, making sure listening is closed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class Listen implements Runnable {
        public void run() {
            Socket listeningSocket = null;
            while (true) {
                try {
                    while (true) {
                        if(stoped.get())return;
                        ServerSocket listeningServerSocket = new ServerSocket(listeningPort);
                        listeningSocket         = listeningServerSocket.accept();
                        ObjectInputStream dis   = new ObjectInputStream(listeningSocket.getInputStream());
                        Object content          = dis.readObject(); // blocking
                        messages.add(new Msg(content, listeningSocket.getInetAddress()));
                        listeningServerSocket.close();
                    }
                } catch (IOException e) {
                    if(debug)System.out.println(listeningPort+"  "+ranking.get()+"  readLine:" + e.getMessage());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class HandleMessage implements Runnable {
        public void run() {
            while (true) {
                if (stoped.get()) return;
                if (messages.size() == 0) continue;
                Msg msg = messages.poll();
                if (msg.content instanceof String) continue;
                try {
                    if (msg.content instanceof GETMessage) handleGET(msg);
                    else if (msg.content instanceof INCMessage) handleINC(msg);
                    else if (msg.content instanceof ValueChangedMessage) handleValueChangedMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        private void handleGET(Msg msg) throws IOException {
            if (debug) System.out.println(listeningPort+"  "+ranking.get()+"  "+msg.content);
            GETMessage getMessage = (GETMessage) msg.content;
            String receivedGetMessagesString = msg.toString();
            if(receivedGetMessages.containsKey(receivedGetMessagesString)) return;
            receivedGetMessages.put(receivedGetMessagesString, true);
            //reply to needed
            if (!send(msg, getMessage.listeningPort, counter.intValue())) send(msg, getMessage.listeningPort, counter.intValue());


        }

        private void handleINC(Msg msg) throws IOException {
            if (debug) System.out.println(listeningPort+"  "+ranking.get()+"  "+msg.content);
            INCMessage incMessage = (INCMessage) msg.content;
            String receivedINCMessagesString = msg.toString();
            boolean seenBefore = receivedINCMessages.containsKey(receivedINCMessagesString);
            if(!seenBefore) {
                counter.incrementAndGet();
                receivedINCMessages.put(receivedINCMessagesString, true);
            }

            //reply to needed
            if (!send(msg, incMessage.listeningPort, counter.intValue())) send(msg, incMessage.listeningPort, counter.intValue());
            if(ranking.get() == 0 && !seenBefore) {
                int rank = 0;
                ValueChangedMessage valueChangedMessage = new ValueChangedMessage(counter.get(), receivedINCMessages);
                for (NodeAddress controlNode : otherControlNodes){
                    if(rank == 0){rank++; continue;}     //wont sent to it self
                    try {
                        rank++;
                        TCPMethods.send(controlNode, valueChangedMessage);
                    } catch (SocketException e) {otherControlNodes.remove(controlNode);}
                }
            }
        }

        private void handleValueChangedMessage(Msg msg) throws IOException {
            int value = ((ValueChangedMessage) msg.content).value;
            if(counter.get()>value) return;
            counter.set(value);
            receivedINCMessages = ((ValueChangedMessage) msg.content).receivedINCMessages;
        }
        private boolean send(Msg msg, int port, int value) throws IOException {
            try {
                if (ranking.get() == 0) {
                    TCPMethods.send(msg.inetAddress, port, value);
                    return true;
                }
            } catch (SocketException e) {
                return true;    //font end dead, no one to send to
            }
            try {
                TCPMethods.send(otherControlNodes.peek(), "");
                return true;    //send to current primary
            } catch (SocketException e) {
                //dead primary move up a rank and return false to try again
                ranking.set(ranking.get() - 1);
                otherControlNodes.poll();
            }
            return false;
        }
    }




    public static class Msg implements Serializable {
        Serializable content;
        InetAddress inetAddress;

        public Msg(Object content, InetAddress inetAddress) {
            this.content = (Serializable) content;
            this.inetAddress = inetAddress;
        }

        @Override
        public String toString() {
            return "Msg{" +
                    "content=" + content +
                    ", inetAddress=" + inetAddress +
                    '}';
        }
    }
    public static class NodeAddress{
        public int port;
        public String address;

        public NodeAddress(int port, String address) {
            this.port = port;
            this.address = address;
        }
        @Override
        public String toString() {
            return "NodeAddress{" +
                    "port=" + port +
                    ", address='" + address + '\'' +
                    '}';
        }
    }
    public static class GETMessage implements Serializable {
        int listeningPort;
        LocalDateTime localDateTime;
        public GETMessage(int listeningPort, LocalDateTime localDateTime) {
            this.listeningPort = listeningPort;
            this.localDateTime = localDateTime;
        }

        @Override
        public String toString() {
            return "GETMessage{" +
                    "listeningPort=" + listeningPort +
                    ", localDateTime=" + localDateTime +
                    '}';
        }
    }
    public static class INCMessage implements Serializable {
        int listeningPort;
        LocalDateTime dateTime;
        public INCMessage(int listeningPort, LocalDateTime dateTime) {
            this.listeningPort = listeningPort;
            this.dateTime = dateTime;
        }

        @Override
        public String toString() {
            return "INCMessage{listeningPort=" + listeningPort +'}';
        }
    }
    public static class ValueChangedMessage implements Serializable {
        int value;
        ConcurrentMap<String, Boolean> receivedINCMessages;
        public ValueChangedMessage(int value, ConcurrentMap<String, Boolean> receivedINCMessages) {
            this.value                  = value;
            this.receivedINCMessages    = receivedINCMessages;
        }

        @Override
        public String toString() {
            return "ValueChangedMessage{value=" + value +'}';
        }
    }
}
