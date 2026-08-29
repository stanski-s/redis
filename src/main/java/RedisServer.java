import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RedisServer {
    public static void main(String[] args) {
        int port = 6379;
        Database database = new Database();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Redis server listening on port:  " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client from: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, database);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}