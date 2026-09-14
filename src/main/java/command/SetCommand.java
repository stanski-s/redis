package command;

import core.Database;
import persistence.AofService;
import protocol.RespWriter;
import server.ClientHandler;

public class SetCommand implements Command {
    private final Database database;
    private final AofService aofService;

    public SetCommand(Database database, AofService aofService) {
        this.database = database;
        this.aofService = aofService;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (args.length < 3) {
            writer.writeError("ERR wrong number of arguments for 'set' command");
            return;
        }

        String key = args[1];
        String value = args[2];
        Long ttlMillis = null;

        if (args.length == 4) {
            try {
                ttlMillis = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                writer.writeError("ERR value is not an integer or out of range");
                return;
            }
        } else if (args.length >= 5) {
            try {
                if (args[3].equalsIgnoreCase("EX")) {
                    ttlMillis = Long.parseLong(args[4]) * 1000;
                } else if (args[3].equalsIgnoreCase("PX")) {
                    ttlMillis = Long.parseLong(args[4]);
                }
            } catch (NumberFormatException e) {
                writer.writeError("ERR value is not an integer or out of range");
                return;
            }
        }

        database.set(key, value, ttlMillis);
        aofService.append(String.join(" ", args));
        writer.writeSimpleString("OK");
    }

}