/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package xml.parsing.machine.api;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * This class is a brick you build your parsing model from.
 * Implements a state machine builder for xml structure.
 */
public class Handler extends RootHandler {
    protected int depth = 1;
    protected boolean active = false;
    protected boolean attributed = false;
    private final String token;
    private Map<String, String> values;
    protected Consumer<String> textConsumer = null;
    protected Consumer<Handler> startConsumer = null;
    protected Consumer<Handler> finallyConsumer = null;
    private Function<Handler, Boolean> assumption;

    /**
     * This method allows to combine tags so you can process different elements. For example,
     * this is how you can read {@code author} and {@code title} of books:
     * <pre>
     *     &lt;book&gt;
     *         &lt;author&gt;Charles Michael Palahniuk&lt;/author&gt;
     *         &lt;title&gt;Fight Club&lt;/title&gt;
     *     &lt;/book&gt;
     * </pre>
     * you can build this structure:
     * <pre>
     *     RootHandler root = RootHandler.instance();
     *     root.then("book")
     *         .or("author", a -&gt; a.text(MyClass::storeAuthor)
     *         .or("title", t -&gt; t.text(MyClass::storeTitle);
     * </pre>
     * An alternative can be
     * <pre>
     *     RootHandler root = RootHandler.instance();
     *     Handler book = root.then("book");
     *     book.then("author").text(MyClass::storeAuthor);
     *     book.then("title").text(MyClass::storeAuthor);
     * </pre>
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
     * <pre>
     *     RootHandler.instance("book", book -&gt; book
     *         .or("title", Handler::propagate)
     *         .or("author", Handler::propagate)
     *         .close(book -&gt; {
     *             MyClass::setAuthor(book.getProperty("author"));
     *             MyClass::setTitle(book.getProperty("title"));
     *         }
     * </pre>
     * <p>Propagation can be nested:</p>
     * <pre>
     *     RootHandler.instance("book", book -&gt; book
     *         .close(book -&gt; {
     *             MyClass::setAuthor(book.getProperty("meta/author"));
     *             MyClass::setTitle(book.getProperty("meta/title"));
     *         }
     *         .then("meta")
     *             .or("title", Handler::propagate)
     *             .or("author", Handler::propagate)
     *             .propagate();
     * </pre>
     * <p>NOTE The given example won't work correctly if {@code book} element has multiple {@code meta} children.
     * In this case you will get values of the last {@code meta} item.</p>
     *
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler propagate() {
        if (textConsumer != null) {
            throw new IllegalStateException("Method propagate() can not be combined with text()");
        }
        if (values == null) {
            values = new HashMap<>();
        }
        textConsumer = x -> values.put(null, x);
        return this;
    }

    /**
     * Tells the handler to collect node attributes.
     * <p>
     * Attributes then are stored in handler's properties (see {@link Handler#getProperty(String)} but
     * can be accessed only in {@link Handler#close(Consumer)}. They are not filled at the moment
     * when {@link Handler#open(Consumer)} is called.
     * </p>
     * <p>See also {@link Handler#assume(Function)}.</p>
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler withAttributes() {
        attributed = true;
        if (values == null) {
            values = new HashMap<>();
        }
        return this;
    }

    /**
     * Defines action to take when the system finds matching element.
     * Fires before any other actions for the element. Node attributes are not populated at this moment.
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
     * Allows to filter node on custom criteria.
     * <p>
     * The assumption is checked at the start of each child node. If it returns {@code FALSE},
     * the child node will be skipped.
     * </p>
     * <p>
     * NOTE The argument of the assumption is the active node, not it's child.
     * </p>
     * <p>
     * Useful for checking attributes. For example, this code will print content of Russian books only:
     * </p>
     * <pre>
     * StringReader reader = new StringReader("&lt;books&gt;&lt;book language='ru'&gt;&lt;content&gt;text 1&lt;/content&gt;&lt;/book&gt;&lt;book&gt;&lt;content&gt;text 2&lt;/content&gt;&lt;/book&gt;&lt;/books&gt;")) {
     * StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
     * parser.read(RootHandler.instance("books", r -&gt; r
     *     .then("book").withAttributes()
     *     .assume(book -&gt; "ru".equals(book.getProperty("@language")))
     *     .then("content").text(System.out::println)
     * ));
     * </pre>
     *
     * @param assumption function that controls processing of child node
     * @return {@code this} that allows to continue the pipeline
     */
    public Handler assume(Function<Handler, Boolean> assumption) {
        if (this.assumption == null) {
            this.assumption = assumption;
        } else {
            throw new IllegalStateException("Duplicate call to assume");
        }
        return this;
    }

    /**
     * Get property propagated to the handler by nested handlers.
     * <p>The method can also return note attributes (prefixed with '@') if {@link Handler#withAttributes()}
     * was called. Propagated attribute name starts with owner node name: {@code owner/@attributeName}</p>
     * <p>Please pay attention: attributes are not available in {@link Handler#open(Consumer)}.</p>
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
            if (assumption == null || !Boolean.FALSE.equals(assumption.apply(this))) {
                return super.onStartElement(name);
            }
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
    public void onAttributes(Map<String, String> values) {
        assert attributed;
        assert this.values != null;
        if (values == null) {
            throw new IllegalArgumentException("Argument must not be null");
        }
        values.forEach((key, value) -> this.values.put("@" + key, value));
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
        if (values != null) {
            values.clear();
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

    @Override
    public boolean needAttributes() {
        return attributed;
    }
}
