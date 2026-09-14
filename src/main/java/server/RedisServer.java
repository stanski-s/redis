package server;

import command.CommandRegistry;
import core.Database;
import persistence.AofService;
import pubsub.PubSubBroker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    public static void main(String[] args) {
        int port = 6379;

        Database database = new Database();
        PubSubBroker broker = new PubSubBroker();
        AofService aof = new AofService("appendonly.aof");
        aof.replay(database);

        CommandRegistry registry = new CommandRegistry(database, broker, aof);

        try (ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Redis server listening on port: " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client from: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, registry);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}