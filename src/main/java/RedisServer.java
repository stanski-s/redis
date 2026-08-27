import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class RedisServer {
    public static void main(String[] args) {
        int port = 6379;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Redis server listening on port " + port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("We have a client from: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String message;
            while ((message = in.readLine()) !  = null) {
                if (message.equalsIgnoreCase("PING")) {
                    out.print("+PONG\r\n");
                    out.flush();
                } else {
                    out.print("-ERR unknown command\r\n");
                    out.flush();
                }
            }
            System.out.println("Client disconnected.");

        } catch (IOException e) {
            System.err.println("A network error occurred: " + e.getMessage());
        }


    }
}