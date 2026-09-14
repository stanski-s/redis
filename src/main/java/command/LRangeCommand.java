package command;

import core.Database;
import protocol.RespWriter;
import server.ClientHandler;
import java.util.List;

public class LRangeCommand implements Command {
    private final Database database;

    public LRangeCommand(Database database) {
        this.database = database;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 4) {
            writer.writeError("ERR wrong number of arguments for 'lrange' command");
            return;
        }

        String key = args[1];
        int start;
        int stop;
        try {
            start = Integer.parseInt(args[2]);
            stop = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            writer.writeError("ERR value is not an integer or out of range");
            return;
        }

        try {
            List<String> items = database.lrange(key, start, stop);
            writer.writeArray(items);
        } catch (IllegalStateException e) {
            writer.writeError(e.getMessage());
        }
    }
}