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


import javax.xml.stream.XMLStreamConstants;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;


/**
 * Pure XML traversing logic.
 */
public abstract class AbstractXmlParser {
    private final Deque<XmlNodeHandler> handlers = new ArrayDeque<>();

    public void read(XmlNodeHandler rootHandler) {
        if (rootHandler == null) {
            throw new IllegalArgumentException("Handler must not be null");
        }
        handlers.clear();
        handlers.push(rootHandler);
        int eventType;
        while ((eventType = next()) != XMLStreamConstants.END_DOCUMENT) {
            switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT: {
                    // Simply skip to the first element
                    break;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    XmlNodeHandler activeHandler = handlers.peek();
                    if (activeHandler.isActive()) {
                        XmlNodeHandler nextHandler = activeHandler.onStartElement(getElementName());
                        if (nextHandler == null || nextHandler == activeHandler) {
                            activeHandler.down();
                        } else {
                            handlers.push(nextHandler);
                            if (nextHandler.needAttributes()) {
                                nextHandler.onAttributes(getAttributes());
                            }
                        }
                    } else {
                        activeHandler.down();
                    }
                    break;
                }
                case XMLStreamConstants.CHARACTERS: {
                    handlers.peek().onText(this::getElementText);
                    break;
                }
                case XMLStreamConstants.END_ELEMENT: {
                    XmlNodeHandler activeHandler = handlers.peek();
                    if (activeHandler.up() == 0) {
                        handlers.pop();
                        if (handlers.isEmpty()) {
                            return;
                        }
                        activeHandler.onEndElement(handlers.peek());
                    }
                }
            }
        }
    }

    /**
     * Read next tag.
     *
     * @return see {@link XMLStreamConstants}
     */
    protected abstract int next();

    /**
     * Get element name
     * @return name
     */
    protected abstract String getElementName();

    /**
     * Get element text
     * @return text of element
     */
    protected abstract String getElementText();

    protected abstract Map<String, String> getAttributes();
}
