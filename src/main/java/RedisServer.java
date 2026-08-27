import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {
    private static final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 6379;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Redis server listening on port " + port);
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("We have a client from: " + clientSocket.getInetAddress());
                Thread clientHandler = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        String message;
                        while ((message = in.readLine()) != null) {
                            String[] messages = message.split(" ");
                            if (messages[0].equalsIgnoreCase("PING")) {
                                out.print("+PONG\r\n");
                                out.flush();
                            } else if (messages[0].equalsIgnoreCase("SET") && messages.length >=3) {
                                database.put(messages[1], messages[2]);
                                out.print("+OK\r\n");
                                out.flush();
                            } else if (messages[0].equalsIgnoreCase("GET")&& messages.length >=2) {
                                var output = database.get(messages[1]);
                                if(output != null){
                                    out.print("+"+output+"\r\n");
                                    out.flush();
                                }else {
                                    out.print("$-1\r\n");
                                    out.flush();
                                }

                            } else  {
                                out.print("-ERR unknown command\r\n");
                                out.flush();
                            }
                        }
                        System.out.println("Client disconnected.");

                    } catch (IOException e) {
                        System.out.println("Client error: " + e.getMessage());
                    } finally {
                        try { clientSocket.close(); } catch (IOException _) {}
                    }
                });
                clientHandler.start();
                }
            } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}