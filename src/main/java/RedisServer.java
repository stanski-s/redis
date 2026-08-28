import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {
    private static final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> expires = new ConcurrentHashMap<>();

    private static void loadAOF(){
        try (BufferedReader reader = new BufferedReader(new FileReader("appendonly.aof"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var commands = line.split(" ");
                    if(commands[0].equalsIgnoreCase("SET")){
                        set(commands);
                    } else if (commands[0].equalsIgnoreCase("INCR")) {
                        incr(commands);
                    }
            }
        } catch (IOException e) {
            System.out.println("error with AOF or AOF is empty.");
        }
    }

    private static String get(String[] messages) {
        var expirationTime = expires.get(messages[1]);

        if (expirationTime != null && expirationTime < System.currentTimeMillis()) {
            database.remove(messages[1]);
            expires.remove(messages[1]);
            return ("$-1\r\n");
        } else {

            var output = database.get(messages[1]);
            if (output != null) {
                return ("+" + output + "\r\n");
            } else {
                return ("$-1\r\n");
            }
        }
    }

    private static String incr(String[] messages){
        return database.compute(messages[1], (k, oldCount) -> {
            if (oldCount == null){
                return "1";
            } else {
                long number = Long.parseLong(oldCount);
                return String.valueOf(number + 1);
            }
        });
    }

    private static void set(String[] messages){
        if(messages.length == 4){
            var expirationTime = Long.parseLong(messages[3]);
            expires.put(messages[1], System.currentTimeMillis() + expirationTime);
        } else {
            expires.remove(messages[1]);
        }
        database.put(messages[1], messages[2]);
    }

    private static synchronized void appendToAOF(String message){
        try (PrintWriter fileOut = new PrintWriter(new FileWriter("appendonly.aof", true))) {
            fileOut.println(message);
        } catch (IOException e) {
            System.err.println("error while writing to AOF: " + e.getMessage());
        }
    }

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