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
                                set(messages);
                                appendToAOF(message);
                                out.print("+OK\r\n");
                                out.flush();
                            } else if (messages[0].equalsIgnoreCase("GET")&& messages.length >=2) {
                                    out.print(get(messages));
                                    out.flush();
                            } else if (messages[0].equalsIgnoreCase("INCR") && messages.length >= 2) {
                                try{
                                    var count = incr(messages);
                                    out.print(":" + count + "\r\n");
                                    out.flush();
                                    appendToAOF(message);
                                } catch (NumberFormatException e){
                                    out.print("-ERR value is not an integer\r\n");
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