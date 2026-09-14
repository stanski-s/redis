package command;

import core.Database;
import protocol.RespWriter;
import server.ClientHandler;

public class GetCommand implements Command{
    private final Database database;

    public GetCommand(Database database) {
        this.database = database;
    }
    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client){
        if (args.length < 2) {
            writer.writeError("ERR wrong number of arguments for 'get' command");
            return;
        }
        String key = args[1];
        Object value = database.get(key);

        if (value == null) {
            writer.writeNull();
        } else if (value instanceof String str) {
            writer.writeBulkString(str);
        } else {
            writer.writeError("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
    }
}
