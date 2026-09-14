package command;

import protocol.RespWriter;
import server.ClientHandler;

public class MultiCommand implements Command {
    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (client.isInTransaction()) {
            writer.writeError("ERR MULTI calls can not be nested");
            return;
        }
        client.setInTransaction(true);
        writer.writeSimpleString("OK");
    }
}