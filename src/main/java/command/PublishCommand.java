package command;

import protocol.RespWriter;
import pubsub.PubSubBroker;
import server.ClientHandler;

public class PublishCommand implements Command {
    private final PubSubBroker broker;

    public PublishCommand(PubSubBroker broker) {
        this.broker = broker;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 3) {
            writer.writeError("ERR wrong number of arguments for 'publish' command");
            return;
        }

        String channel = args[1];
        String message = args[2];
        int receivers = broker.publish(channel, message);

        writer.writeInteger(receivers);
    }
}