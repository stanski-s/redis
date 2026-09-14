package command;

import core.Database;
import persistence.AofService;
import protocol.RespWriter;
import server.ClientHandler;

public class IncrCommand implements Command {
    private final Database database;
    private final AofService aofService;

    public IncrCommand(Database database, AofService aofService) {
        this.database = database;
        this.aofService = aofService;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 2) {
            writer.writeError("ERR wrong number of arguments for 'incr' command");
            return;
        }

        String key = args[1];
        try {
            long result = database.incr(key);
            aofService.append("INCR " + key);
            writer.writeInteger(result);
        } catch (NumberFormatException e) {
            writer.writeError("ERR value is not an integer or out of range");
        } catch (IllegalStateException e) {
            writer.writeError(e.getMessage());
        }
    }
}