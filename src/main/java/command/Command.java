package command;

import core.Database;
import protocol.RespWriter;
import server.ClientHandler;

import java.io.PrintWriter;

@FunctionalInterface
public interface Command {
    void execute(String[] args, RespWriter writer, ClientHandler client);
}
