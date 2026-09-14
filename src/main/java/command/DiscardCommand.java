package command;

import protocol.RespWriter;
import server.ClientHandler;

public class DiscardCommand implements Command {
    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (!client.isInTransaction()) {
            writer.writeError("ERR DISCARD without MULTI");
            return;
        }
        client.resetTransaction();
        writer.writeSimpleString("OK");
    }
}