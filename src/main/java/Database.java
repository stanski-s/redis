import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    private final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expires = new ConcurrentHashMap<>();

    public Database() {
        loadAOF();
    }

    public void loadAOF(){
        try (BufferedReader reader = new BufferedReader(new FileReader("appendonly.aof"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var commands = line.split(" ");
                if(commands[0].equalsIgnoreCase("SET")){
                    applySet(commands);
                } else if (commands[0].equalsIgnoreCase("INCR")) {
                    applyIncr(commands);
                }
            }
        } catch (IOException e) {
            System.out.println("error with AOF or AOF is empty.");
        }
    }

    public String get(String message)
    {
        var messages = message.split(" ");
        var expirationTime = expires.get(messages[1]);

        if (expirationTime != null && expirationTime < System.currentTimeMillis()) {
            database.remove(messages[1]);
            expires.remove(messages[1]);
            return ("$-1\r\n");
        } else {

            var output = database.get(messages[1]);
            if (output != null) {
                return ("$" + output.getBytes().length + "\r\n" + output + "\r\n");
            } else {
                return ("$-1\r\n");
            }
        }
    }

    public String incr(String message) {
        var messages = message.split(" ");
        String result = applyIncr(messages);
        appendToAOF(message);
        return result;
    }

    public String applyIncr(String[] messages){
        return database.compute(messages[1], (k, oldCount) -> {
            if (oldCount == null){
                return "1";
            } else {
                long number = Long.parseLong(oldCount);
                return String.valueOf(number + 1);
            }
        });
    }

    public void set(String message) {
        var messages = message.split(" ");
        applySet(messages);
        appendToAOF(message);
    }

    public void applySet(String[] messages){
        if(messages.length == 4){
            var expirationTime = Long.parseLong(messages[3]);
            expires.put(messages[1], System.currentTimeMillis() + expirationTime);
        } else {
            expires.remove(messages[1]);
        }
        database.put(messages[1], messages[2]);
    }

    private synchronized void appendToAOF(String message){
        try (PrintWriter fileOut = new PrintWriter(new FileWriter("appendonly.aof", true))) {
            fileOut.println(message);
        } catch (IOException e) {
            System.err.println("error while writing to AOF: " + e.getMessage());
        }
    }
}
