package command;

import core.Database;
import protocol.RespWriter;
import server.ClientHandler;

public class LPopCommand implements Command {
    private final Database database;

    public LPopCommand(Database database) {
        this.database = database;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 2) {
            writer.writeError("ERR wrong number of arguments for 'lpop' command");
            return;
        }

        String key = args[1];
        try {
            String item = database.lpop(key);
            if (item == null) {
                writer.writeNull();
            } else {
                writer.writeBulkString(item);
            }
        } catch (IllegalStateException e) {
            writer.writeError(e.getMessage());
        }
    }
}