package server;

import command.Command;
import command.CommandRegistry;
import protocol.RespWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CommandRegistry commandRegistry;
    private final RespWriter writer;
    private boolean inTransaction = false;
    private final List<String[]> transactionQueue = new ArrayList<>();

    public ClientHandler(Socket clientSocket, CommandRegistry commandRegistry) throws IOException {
        this.clientSocket = clientSocket;
        this.commandRegistry = commandRegistry;
        this.writer = new RespWriter(new PrintWriter(clientSocket.getOutputStream(), true));
    }

    private String[] readCommand(BufferedReader in) throws IOException {
        String firstLine = in.readLine();
        if (firstLine == null) {
            return null;
        }
        if (firstLine.startsWith("*")) {
            var numArgs = Integer.parseInt(firstLine.substring(1));
            String[] args = new String[numArgs];

            for (int i = 0; i < numArgs; i++) {
                in.readLine();
                args[i] = in.readLine();
            }
            return args;
        }
        throw new IOException("Unsupported protocol format");
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String[] args;
            while ((args = readCommand(in)) != null) {
                if (args.length == 0) continue;
                String commandName = args[0].toUpperCase();

                if (inTransaction && !commandName.equals("EXEC") && !commandName.equals("DISCARD") && !commandName.equals("MULTI")) {
                    transactionQueue.add(args);
                    writer.writeSimpleString("QUEUED");
                    continue;
                }

                Command command = commandRegistry.get(commandName);
                if (command != null) {
                    command.execute(args, writer, this);
                } else {
                    writer.writeError("ERR unknown command '" + args[0] + "'");
                }
            }
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
    }

    public List<String[]> getTransactionQueue() {
        return transactionQueue;
    }

    public void resetTransaction() {
        this.inTransaction = false;
        transactionQueue.clear();
    }

    public boolean isClosed() {
        return clientSocket.isClosed();
    }
}