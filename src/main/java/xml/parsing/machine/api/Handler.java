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
public class Handler extends RootHandler {
    protected int depth = 1;
    protected boolean active = false;
    private final String token;
    private Map<String, String> values;
    protected Consumer<String> textConsumer = null;
    protected Consumer<Handler> startConsumer = null;
    protected Consumer<Handler> finallyConsumer = null;

    /**
     * This method allows to combine tags so you can process different elements. For example,
     * this is how you can read {@code author} and {@code title} of books:
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
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer must not be null");
        }
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
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer must not be null");
        }
        if (textConsumer != null) {
            throw new IllegalStateException("Duplicate call to text()");
        }
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
        if (textConsumer != null) {
            throw new IllegalStateException("Method propagate() can not be combined with text()");
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
        if (startConsumer != null) {
            throw new IllegalStateException("Duplicate call of open()");
        }
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer must not be null");
        }
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
        if (finallyConsumer != null) {
            throw new IllegalStateException("Duplicate call of close()");
        }
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer must not be null");
        }
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
            throw new IllegalStateException("None of the children provided a value. Did you forgot propagate()?");
        } else {
            return values.get(name);
        }
    }

    protected Handler(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token must not be empty");
        }
        this.token = token;
    }

    @Override
    public XmlNodeHandler onStartElement(String name) {
        if (depth != 1) {
            return null;
        }
        if (active) {
            return super.onStartElement(name);
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
