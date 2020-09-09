package xml.parsing.machine.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandlerTest {
    @Test
    public void shouldCreateRootHandler() {
        assertNotNull(Handler.root());
    }

    public void shouldCreatePathHandlers() {
        Handler root = Handler.root();
        root.then("Library").then("Book").text(System.out::println);
    }

}