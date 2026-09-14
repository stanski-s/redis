package core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    @Test
    void shouldSetAndGetKey() {
        Database db = new Database();

        db.set("foo", "bar");
        assertEquals("bar", db.get("foo"));
    }

    @Test
    void shouldReturnNullWhenKeyDoesNotExist() {
        Database db = new Database();

        assertNull(db.get("non_existing"));
    }

    @Test
    void shouldExpireKeyAfterTtl() throws InterruptedException {
        Database db = new Database();

        db.set("temp", "secret", 50L);
        assertEquals("secret", db.get("temp"));

        Thread.sleep(60);
        assertNull(db.get("temp"));
    }

    @Test
    void shouldIncrementCounter() {
        Database db = new Database();

        long val1 = db.incr("counter");
        long val2 = db.incr("counter");

        assertEquals(1, val1);
        assertEquals(2, val2);
    }
}