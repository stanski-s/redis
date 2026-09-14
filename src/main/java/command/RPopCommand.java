package command;

import core.Database;
import protocol.RespWriter;
import server.ClientHandler;

public class RPopCommand implements Command {
    private final Database database;

    public RPopCommand(Database database) {
        this.database = database;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 2) {
            writer.writeError("ERR wrong number of arguments for 'rpop' command");
            return;
        }

        String key = args[1];
        try {
            String item = database.rpop(key);
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