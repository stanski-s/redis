package command;

import protocol.RespWriter;
import server.ClientHandler;

import java.util.List;

public class ExecCommand implements Command {
    private final CommandRegistry registry;

    public ExecCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void execute(String[] args, RespWriter writer, ClientHandler client) {
        if (!client.isInTransaction()) {
            writer.writeError("ERR EXEC without MULTI");
            return;
        }

        List<String[]> queue = client.getTransactionQueue();
        client.setInTransaction(false);

        if (queue.isEmpty()) {
            writer.writeArrayHeader(0);
            return;
        }

        writer.writeArrayHeader(queue.size());

        for (String[] queuedArgs : queue) {
            String cmdName = queuedArgs[0].toUpperCase();
            Command cmd = registry.get(cmdName);
            if (cmd != null) {
                cmd.execute(queuedArgs, writer, client);
            } else {
                writer.writeError("ERR unknown command in transaction");
            }
        }

        client.resetTransaction();
    }
}