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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
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
     * More comfortable alternative to {@link RootHandler#instance()}.
     * <p>It eliminates the need of {@code RootHandler} variable. Instead of</p>
     * <pre>
     *     RootHandler root = RootHandler.instance();
     *     root.then("library");
     *     parser.read(root);
     * </pre>
     * <p>we can have</p>
     * <pre>
     *     parser.read(RootHandler.instance("library", h -&gt; h.then("library"));
     * </pre>
     *
     * @param rootToken name of root element
     * @param howToProcess function to customize handler
     * @return root handler that can be passed to {@link AbstractXmlParser#read(XmlNodeHandler)}
     */
    public static RootHandler instance(String rootToken, Consumer<Handler> howToProcess) {
        RootHandler result = new RootHandler();
        Handler h = result.then(rootToken);
        howToProcess.accept(h);
        return result;
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
    public void onAttributes(Map<String, String> attributes) {
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

    @Override
    public boolean needAttributes() {
        return false;
    }
}
