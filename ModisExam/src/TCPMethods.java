import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPMethods implements Serializable {

    public static void send(String ip, int port, Serializable message) throws IOException {
        Socket s = new Socket(ip, port);
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(message);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void send(InetAddress inetAddress, int port, Serializable message) throws IOException {
        Socket s = new Socket(inetAddress, port);
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(message);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void send(ReplicaManager.NodeAddress sendersAddress, Serializable message) throws IOException {
        send(sendersAddress.address, sendersAddress.port, message);
    }


    public static Object listen(int port) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        ObjectInputStream dis = new ObjectInputStream(socket.getInputStream());
        Object returnObject = dis.readObject();
        serverSocket.close();
        return returnObject;
    }
    public static Object listen(ServerSocket serverSocket) throws IOException, ClassNotFoundException {
        Socket socket = serverSocket.accept();
        ObjectInputStream dis = new ObjectInputStream(socket.getInputStream());
        Object returnObject = dis.readObject();
        serverSocket.close();
        return returnObject;
    }
}
