package pubsub;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubBroker {
    private final ConcurrentHashMap<String, Set<BlockingQueue<String>>> channels = new ConcurrentHashMap<>();

    public void subscribe(String channel, BlockingQueue<String> clientQueue) {
        channels.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(clientQueue);
    }

    public void unsubscribe(String channel, BlockingQueue<String> clientQueue) {
        Set<BlockingQueue<String>> subscribers = channels.get(channel);
        if (subscribers != null) {
            subscribers.remove(clientQueue);
            if (subscribers.isEmpty()) {
                channels.remove(channel);
            }
        }
    }

    public int publish(String channel, String message) {
        Set<BlockingQueue<String>> subscribers = channels.get(channel);
        if (subscribers == null || subscribers.isEmpty()) {
            return 0;
        }

        String respMessage = "*3\r\n" +
                "$7\r\nmessage\r\n" +
                "$" + channel.getBytes().length + "\r\n" + channel + "\r\n" +
                "$" + message.getBytes().length + "\r\n" + message + "\r\n";

        for (var queue : subscribers) {
            queue.offer(respMessage);
        }
        return subscribers.size();
    }
}