package xml.parsing.machine.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * This class is a brick you build your parsing model from.
 * Implements a state machine builder for xml structure.
 *
 * @see xml.parsing.machine.examples.LibraryParserExample
 */
public class Handler implements XmlNodeHandler {
    private int depth = 1;
    private boolean active = false;
    private String token;
    private Map<String, String> values;
    private Map<String, Handler> children = null;
    private Consumer<String> textConsumer = null;
    private Consumer<Handler> startConsumer = null;
    private Consumer<Handler> finallyConsumer = null;

    /**
     * Constructs a new handler which handles root item of xml file.
     * You should pass this item to {@link AbstractXmlParser#read}.
     *
     * @return new handler
     */
    public static Handler root() {
        Handler h = new Handler();
        h.active = true;
        return h;
    }

    /**
     * Defines a handler of nested element. For example, if you need to process {@code book} elements nested
     * into {@code library} element, you can build the following structure:
     * <code>
     *     Handler root = Handler.root();
     *     root.then("library").then("book");
     * </code>
     * <p>See {@link Handler#or} if you need to process multiple different element tags.</p>
     *
     * @param token name of element to search
     * @return      the created handler so you can build a pipeline
     */
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

    /**
     * This method allows to combine tags so you can process different elements. For example,
     * this is how you can read {@code author} and {@title} of books:
     * <code>
     *     &lt;book&gt;
     *         &lt;author&gt;Charles Michael Palahniuk&lt;/author&gt;
     *         &lt;title&gt;Fight Club&lt;/title&gt;
     *     &lt;/book&gt
     * </code>
     * you can build this structure:
     * <code>
     *     Handler root = Handler.root();
     *     root.then("book")
     *         .or("author", a -> a.text(MyClass::storeAuthor)
     *         .or("title", t -> t.text(MyClass::storeTitle);
     * </code>
     *
     * @param token element name to wait for
     * @param consumer function to customize the nested handler
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler or(String token, Consumer<Handler> consumer) {
        consumer.accept(then(token));
        return this;
    }

    /**
     * Defines how to process text of element.
     *
     * <p>This functionality can not be combined with {@link Handler#propagate()}.</p>
     *
     * @param consumer function that processes text of element
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler text(Consumer<String> consumer) {
        textConsumer = consumer;
        return this;
    }

    /**
     * Makes handler to share it's text value with parent handler.
     * The feature is useful for collecting simple values from sub-elements
     * so they can be processed in one place.
     * <p>The values can be accessed through {@link Handler#getProperty(String)} where property name is
     * the same as corresponding element name:</p>
     * <code>
     *     Handler root = Handler.root();
     *     root.then("book")
     *         .or("title", Handler::propagate)
     *         .or("author", Handler::propagate)
     *         .close(book -> {
     *             MyClass::setAuthor(book.getProperty("author"));
     *             MyClass::setTitle(book.getProperty("title"));
     *         }
     * </code>
     * <p>Propagation can be nested:</p>
     * <code>
     *     Handler root = Handler.root();
     *     root.then("book")
     *         .close(book -> {
     *             MyClass::setAuthor(book.getProperty("meta/author"));
     *             MyClass::setTitle(book.getProperty("meta/title"));
     *         }
     *         .then("meta")
     *             .or("title", Handler::propagate)
     *             .or("author", Handler::propagate)
     *             .propagate();
     * </code>
     * <p>The given example wont work correctly if {@code book} element has multiple {@code meta} children.
     * In this case you will get values of the last {@code meta} item.</p>
     *
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler propagate() {
        if (token == null) {
            throw new IllegalStateException("Can not propagate root handler");
        }
        values = new HashMap<>();
        textConsumer = x -> values.put(null, x);
        return this;
    }

    /**
     * Defines action to take when the system finds matching element.
     * Fires before any other actions for the element.
     *
     * @param consumer action
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler open(Consumer<Handler> consumer) {
        startConsumer = consumer;
        return this;
    }

    /**
     * Defines action to take when the system completes parsing of matching element.
     *
     * @param consumer action
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler close(Consumer<Handler> consumer) {
        finallyConsumer = consumer;
        return this;
    }

    /**
     * Get property propagated to the handler by nested handlers(2).
     *
     * @param name property name. See {@link Handler#propagate()}
     * @return {@code this} that allows to continue the pipeline
     */
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
            if (next.startConsumer != null) {
                next.startConsumer.accept(next);
            }
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
