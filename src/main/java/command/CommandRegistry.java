package command;

import core.Database;
import persistence.AofService;
import pubsub.PubSubBroker;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry(Database database, PubSubBroker pubSubBroker, AofService aofService) {
        register("PING", new PingCommand());
        register("GET", new GetCommand(database));
        register("SET", new SetCommand(database, aofService));
        register("INCR", new IncrCommand(database, aofService));
        register("LPUSH", new LPushCommand(database));
        register("RPUSH", new RPushCommand(database));
        register("LPOP", new LPopCommand(database));
        register("RPOP", new RPopCommand(database));
        register("LRANGE", new LRangeCommand(database));
        register("PUBLISH", new PublishCommand(pubSubBroker));
        register("SUBSCRIBE", new SubscribeCommand(pubSubBroker));
        register("MULTI", new MultiCommand());
        register("DISCARD", new DiscardCommand());
        register("EXEC", new ExecCommand(this));
    }

    public void register(String name, Command command) {
        commands.put(name.toUpperCase(), command);
    }

    public Command get(String name) {
        return commands.get(name.toUpperCase());
    }
}