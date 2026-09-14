package command;

import protocol.RespWriter;
import server.ClientHandler;

public class PingCommand implements Command {
    @Override
    public  void execute (String[] args, RespWriter writer, ClientHandler client){
        writer.writeSimpleString("PONG");
    }
}
