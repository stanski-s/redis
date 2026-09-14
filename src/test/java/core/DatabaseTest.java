package core;

import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.AssertJUnit.assertEquals;

public class DatabaseTest {
    @Test
    void shouldSetAndGetKey(){
        Database db = new Database();

        db.set("set foo bar");
        assertEquals("$3\r\nbar \r\n", db.get("foo"));

    }
}
