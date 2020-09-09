package xml.parsing.machine.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RootHandler implements XmlNodeHandler {
    private Map<String, Handler> children = null;

    /**
     * Constructs a new handler which handles root item of xml file.
     * You should pass this item to {@link AbstractXmlParser#read}.
     *
     * @return new handler
     */
    public static RootHandler instance() {
        return new RootHandler();
    }

    /**
     * Defines a handler of nested element. For example, if you need to process {@code book} elements nested
     * into {@code library} element, you can build the following structure:
     * <pre>
     *     Handler root = Handler.root();
     *     root.then("library").then("book");
     * </pre>
     * <p>See {@link Handler#or} if you need to process multiple different element tags.</p>
     *
     * @param token name of element to search
     * @return      the created handler so you can build a pipeline
     */
    public Handler then(String token) {
        Handler nextHandler = new Handler(token);
        if (children == null) {
            children = new HashMap<>();
        }
        if (children.put(token, nextHandler) != null) {
            throw new IllegalArgumentException("This element name already has a handler");
        }
        return nextHandler;
    }

    protected RootHandler() {}

    @Override
    public XmlNodeHandler onStartElement(String name) {
        Handler next = children.get(name);
        if (next == null) {
            return this;
        }
        next.active = true;
        next.depth = 1;
        if (next.startConsumer != null) {
            next.startConsumer.accept(next);
        }
        return next;
    }

    @Override
    public void onText(Supplier<String> text) {
    }

    @Override
    public void onEndElement(XmlNodeHandler parent) {
    }

    @Override
    public int down() {
        return 1;
    }

    @Override
    public int up() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
