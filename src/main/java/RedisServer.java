import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {
    static void main(String[] args) {
        int port = 6379;
        loadAOF();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Redis server listening on port " + port);
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("We have a client from: " + clientSocket.getInetAddress());
                Thread clientHandler = new Thread(() -> {

                });
                clientHandler.start();
                }
            } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}