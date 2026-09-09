package server;

import command.Command;
import core.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Database database;
    private boolean inTransaction = false;
    private final List<String[]> transactionQueue = new ArrayList<>();

    private final Map<String, Command> commands = new HashMap<>();

    private String[] readCommand (BufferedReader in) throws IOException {
        String firstLine = in.readLine();
        if (firstLine == null){
            return null;
        }
        if (firstLine.startsWith("*")){
            var numArgs = Integer.parseInt(firstLine.substring(1));
            String[] args = new String[numArgs];

            for (int i = 0; i < numArgs; i++){
                var lineLength = in.readLine();
                var line = in.readLine();
                args[i] = line;
            }
            return args;
        }
        throw new IOException("Unsupported protocol format");
    }

    public ClientHandler(Socket clientSocket, Database database) {
        this.clientSocket = clientSocket;
        this.database = database;

        commands.put("PING", (args, raw, out, db) -> {
            out.print("+PONG\r\n");
            out.flush();
        });

        commands.put("GET", (args, raw, out, db) -> {
            if (args.length >= 2) {
                out.print(db.get(raw));
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'GET' command\r\n");
                out.flush();
            }
        });

        commands.put("SET", (args, raw, out, db) -> {
            db.set(raw);
            out.print("+OK\r\n");
            out.flush();
        });

        commands.put("LPUSH", (args, raw, out, db) -> {
            if (args.length >= 3) {
                out.print(db.lpush(args));
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'lpush' command\r\n");
                out.flush();
            }
        });

        commands.put("RPUSH", (args, raw, out, db) -> {
            if (args.length >= 3) {
                out.print(db.rpush(args));
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'rpush' command\r\n");
                out.flush();
            }
        });

        commands.put("LPOP", (args, raw, out, db) -> {
            if (args.length >= 2) {
                out.print(db.lpop(args));
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'lpop' command\r\n");
                out.flush();
            }
        });

        commands.put("RPOP", (args, raw, out, db) -> {
            if (args.length >= 2) {
                out.print(db.rpop(args));
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'rpop' command\r\n");
                out.flush();
            }
        });

        commands.put("PUBLISH", (args, raw, out, db) -> {
            if (args.length >= 3) {
                String channel = args[1];
                String message = args[2];
                int receivers = db.publish(channel, message);
                out.print(":" + receivers + "\r\n");
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'publish' command\r\n");
                out.flush();
            }
        });

        commands.put("SUBSCRIBE", (args, raw, out, db) -> {
            if (args.length >= 2) {
                String channel = args[1];
                BlockingQueue<String> clientInbox = new LinkedBlockingQueue<>();

                db.addSubscriber(channel, clientInbox);

                out.print("*3\r\n$9\r\nsubscribe\r\n$" + channel.getBytes().length + "\r\n" + channel + "\r\n:1\r\n");
                out.flush();

                System.out.println("[PubSub] Client subscribed to channel: " + channel);

                try {
                    while (true) {
                        String incomingMessage = clientInbox.take();
                        out.print(incomingMessage);
                        out.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    db.removeSubscriber(channel, clientInbox);
                    System.out.println("[PubSub] Client unsubscribed to channel: " + channel);
                }

            } else {
                out.print("-ERR wrong number of arguments for 'subscribe' command\r\n");
                out.flush();
            }
        });

        commands.put("LRANGE", (args, raw, out, db) -> {
            if (args.length >= 4) {
                out.print(db.lrange(args));
                out.flush();
            } else {
                out.print("-ERR wrong number of arguments for 'lrange' command\r\n");
                out.flush();
            }
        });

        commands.put("MULTI", (args, raw, out, db) -> {
            inTransaction = true;
            out.print("+OK\r\n");
            out.flush();
        });

        commands.put("DISCARD", (args, raw, out, db) -> {
            if (!inTransaction) {
                out.print("-ERR DISCARD without MULTI\r\n");
                out.flush();
                return;
            }
            inTransaction = false;
            transactionQueue.clear();
            out.print("+OK\r\n");
            out.flush();
        });

        commands.put("EXEC", (args, raw, out, db) -> {
            if (!inTransaction) {
                out.print("-ERR EXEC without MULTI\r\n");
                out.flush();
                return;
            }

            inTransaction = false;

            if (transactionQueue.isEmpty()) {
                out.print("*0\r\n");
                out.flush();
                return;
            }

            out.print("*" + transactionQueue.size() + "\r\n");

            synchronized (db) {
                for (String[] queuedArgs : transactionQueue) {
                    Command queuedCmd = commands.get(queuedArgs[0].toUpperCase());
                    if (queuedCmd != null) {
                        String reconstructedRaw = String.join(" ", queuedArgs);
                        queuedCmd.execute(queuedArgs, reconstructedRaw, out, db);
                    }
                }
            }

            out.flush();
            transactionQueue.clear();
        });

        commands.put("INCR", (args, raw, out, db) -> {
            try {
                var count = db.incr(raw);
                out.print(":" + count + "\r\n");
                out.flush();
            } catch (NumberFormatException e){
                out.print("-ERR value is not an integer\r\n");
                out.flush();
            }
        });
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String[] args;
            while ((args = readCommand(in)) != null) {
                String commandName = args[0].toUpperCase();
                String message = String.join(" ", args);

                if (inTransaction && !commandName.equals("EXEC") && !commandName.equals("DISCARD") && !commandName.equals("MULTI")) {
                    transactionQueue.add(args);
                    out.print("+QUEUED\r\n");
                    out.flush();
                    continue;
                }

                Command command = commands.get(commandName);
                if(command != null) {
                    command.execute(args, message, out, database);
                } else {
                    out.print("-ERR unknown command\r\n");
                    out.flush();
                }
            }
            System.out.println("Client disconnected.");

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }
}