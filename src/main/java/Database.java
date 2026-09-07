import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Database {
    private java.util.Iterator<String> expirationIterator = null;
    private final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expires = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reaperExecutor = Executors.newSingleThreadScheduledExecutor();

    public Database() {
        loadAOF();
        startActiveExpiration();
    }

    private void startActiveExpiration() {
        reaperExecutor.scheduleAtFixedRate(this::cleanUpExpiredKeys, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void cleanUpExpiredKeys() {
        if (expires.isEmpty()) return;
        int sampleSize = 10;
        int expiredCount;

        do {
            expiredCount = 0;
            long now = System.currentTimeMillis();
            int checked = 0;

            while (checked < sampleSize) {
                if (expirationIterator == null || !expirationIterator.hasNext()) {
                    expirationIterator = expires.keySet().iterator();
                    if (!expirationIterator.hasNext()) break;
                }
                String key = expirationIterator.next();
                Long expirationTime = expires.get(key);
                checked++;

                if (expirationTime != null && expirationTime < now) {
                    database.remove(key);
                    expires.remove(key);
                    expiredCount++;
                    System.out.println("Deleted: " + key);
                }
            }
        }while (expiredCount > sampleSize / 4);
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
