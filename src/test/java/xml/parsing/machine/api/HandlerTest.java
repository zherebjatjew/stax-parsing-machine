package xml.parsing.machine.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandlerTest {
    @Test
    public void shouldDenyNullTokens() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RootHandler.instance().then(null));
    }

    @Test
    public void shouldDenyBlankToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RootHandler.instance().then(""));
    }

    @Test
    public void shouldDenyProcessingTextForPropagatingHandler() {
        assertThrows(
                IllegalStateException.class,
                () -> RootHandler.instance().then("test").text(t -> {}).propagate());
    }

    @Test
    public void shouldDenyDuplicateText() {
        assertThrows(
                IllegalStateException.class,
                () -> RootHandler.instance().then("test").text(t -> {}).text(t -> {}));
    }

    @Test
    public void shouldDenyDuplicateOpen() {
        assertThrows(
                IllegalStateException.class,
                () -> RootHandler.instance().then("test").open(t -> {}).open(t -> {}));
    }

    @Test
    public void shouldDenyDuplicateClose() {
        assertThrows(
                IllegalStateException.class,
                () -> RootHandler.instance().then("test").close(t -> {}).close(t -> {}));
    }

    @Test
    public void shouldDenyNullOpen() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RootHandler.instance().then("test").open(null));
    }

    @Test
    public void shouldDenyNullClose() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RootHandler.instance().then("test").close(null));
    }

    @Test
    public void shouldDenyNullText() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RootHandler.instance().then("test").text(null));
    }
}