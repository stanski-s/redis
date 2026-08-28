import java.io.PrintWriter;

@FunctionalInterface
public interface Command {
    void execute(String[] args, String rawMessage, PrintWriter out, Database database);
}
