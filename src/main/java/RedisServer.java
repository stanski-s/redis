import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    public static void main(String[] args) {
        int port = 6379;
        Database database = new Database();

        try (ExecutorService threadPool = Executors.newFixedThreadPool(10);
             ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Redis server listening on port:  " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client from: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, database);

                threadPool.execute(clientHandler);
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}