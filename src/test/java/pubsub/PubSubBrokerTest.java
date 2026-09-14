package pubsub;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class PubSubBrokerTest {

    @Test
    void shouldDeliverMessageToSubscriber() {
        PubSubBroker broker = new PubSubBroker();
        BlockingQueue<String> clientInbox = new LinkedBlockingQueue<>();

        broker.subscribe("news", clientInbox);
        int receivers = broker.publish("news", "Hello Redis!");

        assertEquals(1, receivers, "there should be exactly 1 reciver");

        String received = clientInbox.poll();
        assertNotNull(received, "inbox shouldnt be empty");
        assertTrue(received.contains("news"));
        assertTrue(received.contains("Hello Redis!"));
    }

    @Test
    void shouldNotDeliverMessageAfterUnsubscribe() {
        PubSubBroker broker = new PubSubBroker();
        BlockingQueue<String> clientInbox = new LinkedBlockingQueue<>();

        broker.subscribe("news", clientInbox);
        broker.unsubscribe("news", clientInbox);

        int receivers = broker.publish("news", "Nobody should hear this");

        assertEquals(0, receivers, "Po unsubscribe powinno być 0 odbiorców");
        assertTrue(clientInbox.isEmpty(), "inbox shouldnt be empty");
    }

    @Test
    void shouldBroadcastToMultipleSubscribers() {
        PubSubBroker broker = new PubSubBroker();
        BlockingQueue<String> client1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> client2 = new LinkedBlockingQueue<>();

        broker.subscribe("tech", client1);
        broker.subscribe("tech", client2);

        int receivers = broker.publish("tech", "Java 26 released!");

        assertEquals(2, receivers);
        assertNotNull(client1.poll());
        assertNotNull(client2.poll());
    }
}