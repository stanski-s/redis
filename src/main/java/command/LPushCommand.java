package command;

import core.Database;
import protocol.RespWriter;
import server.ClientHandler;
import java.util.Arrays;

public class LPushCommand implements Command {
    private final Database database;

    public LPushCommand(Database database) {
        this.database = database;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 3) {
            writer.writeError("ERR wrong number of arguments for 'lpush' command");
            return;
        }

        String key = args[1];
        String[] values = Arrays.copyOfRange(args, 2, args.length);

        try {
            int newSize = database.lpush(key, values);
            writer.writeInteger(newSize);
        } catch (IllegalStateException e) {
            writer.writeError(e.getMessage());
        }
    }
}