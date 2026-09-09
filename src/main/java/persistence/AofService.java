package persistence;

import core.Database;

import java.io.*;

public class AofService {
    private final String filename;

    public AofService(String filename) {
        this.filename = filename;
    }

    public synchronized void append(String command) {
        try (PrintWriter fileOut = new PrintWriter(new FileWriter(filename, true))) {
            fileOut.println(command);
        } catch (IOException e) {
            System.err.println("[AOF] Error while writing to AOF: " + e.getMessage());
        }
    }

    public void replay(Database database) {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 0) continue;

                String cmd = parts[0].toUpperCase();
                if (cmd.equals("SET") && parts.length >= 3) {
                    Long ttl = (parts.length >= 4) ? Long.parseLong(parts[3]) : null;
                    database.set(parts[1], parts[2], ttl);
                } else if (cmd.equals("INCR") && parts.length >= 2) {
                    database.incr(parts[1]);
                }
            }
            System.out.println("[AOF] Successfully replayed operations from " + filename);
        } catch (IOException e) {
            System.err.println("[AOF] Error replaying AOF: " + e.getMessage());
        }
    }
}
