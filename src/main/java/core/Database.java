package core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Database {
    private final ConcurrentHashMap<String, DatabaseEntry> storage = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reaperExecutor = Executors.newSingleThreadScheduledExecutor();

    public Database() {
        startActiveExpiration();
    }

    private void startActiveExpiration() {
        reaperExecutor.scheduleAtFixedRate(this::cleanUpExpiredKeys, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void cleanUpExpiredKeys() {
        Object[] keys = storage.keySet().toArray();
        if (keys.length == 0) return;

        int sampleSize = Math.min(20, keys.length);
        var random = ThreadLocalRandom.current();
        int expiredCount;

        do {
            expiredCount = 0;

            for (int i = 0; i < sampleSize; i++) {
                int randomIndex = random.nextInt(keys.length);
                String key = (String) keys[randomIndex];

                DatabaseEntry entry = storage.get(key);
                if (entry != null && entry.isExpired()) {
                    if (storage.remove(key, entry)) {
                        expiredCount++;
                        System.out.println("[Active Expiration] Deleted expired key: " + key);
                    }
                }
            }
        } while (expiredCount > sampleSize / 4);
    }

    public Object get(String key) {
        DatabaseEntry entry = storage.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            storage.remove(key);
            return null;
        }

        return entry.value();
    }

    public int lpush(String key, String... values) {
        return pushToList(key, values, true);
    }

    public int rpush(String key, String... values) {
        return pushToList(key, values, false);
    }

    public String lpop(String key) {
        return popFromList(key, true);
    }

    public String rpop(String key) {
        return popFromList(key, false);
    }
    private String popFromList(String key, boolean isLeft) {
        DatabaseEntry entry = storage.get(key);

        if (entry == null || entry.isExpired()) {
            if (entry != null) storage.remove(key);
            return null;
        }

        if (!(entry.value() instanceof ConcurrentLinkedDeque<?> deque)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        @SuppressWarnings("unchecked")
        ConcurrentLinkedDeque<String> list = (ConcurrentLinkedDeque<String>) deque;

        String item = isLeft ? list.pollFirst() : list.pollLast();

        if (item == null) {
            storage.remove(key);
            return null;
        }

        if (list.isEmpty()) {
            storage.remove(key);
        }

        return item;
    }

    public List<String> lrange(String key, int start, int stop) {
        DatabaseEntry entry = storage.get(key);

        if (entry == null || entry.isExpired()) {
            if (entry != null) storage.remove(key);
            return List.of();
        }

        if (!(entry.value() instanceof ConcurrentLinkedDeque<?> deque)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        @SuppressWarnings("unchecked")
        ConcurrentLinkedDeque<String> list = (ConcurrentLinkedDeque<String>) deque;
        int size = list.size();

        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;

        if (start < 0) start = 0;
        if (start >= size || start > stop) return List.of();
        if (stop >= size) stop = size - 1;

        List<String> result = new ArrayList<>();
        int currentIndex = 0;
        for (String item : list) {
            if (currentIndex >= start && currentIndex <= stop) {
                result.add(item);
            }
            if (currentIndex > stop) {
                break;
            }
            currentIndex++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private int pushToList(String key, String[] values, boolean isLeft) {
        DatabaseEntry entry = storage.compute(key, (k, oldEntry) -> {
            ConcurrentLinkedDeque<String> list;

            if (oldEntry == null || oldEntry.isExpired()) {
                list = new ConcurrentLinkedDeque<>();
            } else if (oldEntry.value() instanceof ConcurrentLinkedDeque<?> existingList) {
                list = (ConcurrentLinkedDeque<String>) existingList;
            } else {
                throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            for (String val : values) {
                if (isLeft) {
                    list.addFirst(val);
                } else {
                    list.addLast(val);
                }
            }

            Long ttl = (oldEntry != null && !oldEntry.isExpired()) ? oldEntry.expiresAt() : null;
            return new DatabaseEntry(list, ttl);
        });

        ConcurrentLinkedDeque<String> list = (ConcurrentLinkedDeque<String>) entry.value();
        return list.size();
    }


    public long incr(String key) {
        DatabaseEntry updatedEntry = storage.compute(key, (k, oldEntry) -> {
            if (oldEntry == null || oldEntry.isExpired()) {
                return new DatabaseEntry("1", null);
            }
            Object val = oldEntry.value();
            if (!(val instanceof String strVal)) {
                throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            long newVal = Long.parseLong(strVal) + 1;

            return new DatabaseEntry(String.valueOf(newVal), oldEntry.expiresAt());
        });

        return Long.parseLong((String) updatedEntry.value());
    }

    public void set(String key, Object value, Long ttlMillis) {
        Long expiresAt = (ttlMillis != null) ? System.currentTimeMillis() + ttlMillis : null;
        storage.put(key, new DatabaseEntry(value, expiresAt));
    }

    public void set(String key, Object value) {
        set(key, value, null);
    }
}
