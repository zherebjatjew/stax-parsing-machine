package xml.parsing.machine.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Handler implements XmlNodeHandler {
    private int depth = 1;
    private boolean active = false;
    private String token;
    private Map<String, String> values;
    private Map<String, Handler> children = null;
    private Consumer<String> textConsumer = null;
    private Consumer<Handler> finallyConsumer = null;

    // builder
    public static Handler root() {
        Handler h = new Handler();
        h.active = true;
        return h;
    }

    public Handler then(String token) {
        Handler nextHandler = new Handler();
        nextHandler.token = token;
        if (children == null) {
            children = new HashMap<>();
        }
        if (children.put(token, nextHandler) != null) {
            throw new IllegalArgumentException("This element name already has a handler");
        }
        return nextHandler;
    }

    public Handler or(String token, Consumer<Handler> consumer) {
        consumer.accept(then(token));
        return this;
    }

    public Handler text(Consumer<String> consumer) {
        textConsumer = consumer;
        return this;
    }

    public Handler propagate() {
        if (token == null) {
            throw new IllegalStateException("Can not propagate root handler");
        }
        values = new HashMap<>();
        textConsumer = x -> values.put(null, x);
        return this;
    }

    public Handler close(Consumer<Handler> consumer) {
        finallyConsumer = consumer;
        return this;
    }

    public String getProperty(String name) {
        if (values == null) {
            return null;
        } else {
            return values.get(name);
        }
    }

    protected Handler() {}

    @Override
    public XmlNodeHandler onStartElement(String name) {
        if (depth != 1) {
            return null;
        }
        if (active) {
            Handler next = children.get(name);
            if (next == null) {
                return this;
            }
            next.active = true;
            next.depth = 1;
            return next;
        }
        return this;
    }

    @Override
    public void onText(Supplier<String> text) {
        if (active && textConsumer != null) {
            textConsumer.accept(text.get());
        }
    }

    @Override
    public void onEndElement(XmlNodeHandler parent) {
        if (!active) {
            throw new IllegalStateException("Can be called for active handlers only");
        }
        active = false;
        if (finallyConsumer != null) {
            finallyConsumer.accept(this);
        }
        if (values != null && parent instanceof Handler) {
            Handler h = (Handler) parent;
            if (h.values == null) {
                h.values = new HashMap<>();
            }
            values.forEach((k,v) -> {
                String key;
                if (k == null) {
                    key = token;
                } else {
                    key = token + '/' + k;
                }
                h.values.put(key, v);
            });
        }
    }

    @Override
    public int down() {
        return ++depth;
    }

    @Override
    public int up() {
        depth--;
        if (depth < 0) {
            throw new IllegalStateException("Depth can not be negative. Something is wrong with parser structure");
        }
        return depth;
    }

    @Override
    public boolean isActive() {
        return depth == 1;
    }
}
