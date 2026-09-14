package command;

import protocol.RespWriter;
import pubsub.PubSubBroker;
import server.ClientHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SubscribeCommand implements Command {
    private final PubSubBroker broker;

    public SubscribeCommand(PubSubBroker broker) {
        this.broker = broker;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 2) {
            writer.writeError("ERR wrong number of arguments for 'subscribe' command");
            return;
        }

        String channel = args[1];
        BlockingQueue<String> clientInbox = new LinkedBlockingQueue<>();
        broker.subscribe(channel, clientInbox);

        writer.writeArrayHeader(3);
        writer.writeBulkString("subscribe");
        writer.writeBulkString(channel);
        writer.writeInteger(1);

        System.out.println("[PubSub] Client subscribed to channel: " + channel);

        try {
            while (!client.isClosed()) {
                String incomingMessage = clientInbox.take();
                writer.getRawWriter().print(incomingMessage);
                writer.getRawWriter().flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            broker.unsubscribe(channel, clientInbox);
            System.out.println("[PubSub] Client unsubscribed from channel: " + channel);
        }
    }
}