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

import java.util.Map;
import java.util.function.Supplier;


/**
 * Interface that provides xml element callback methods called by parser.
 */
public interface XmlNodeHandler {
    /**
     * To be called when parser meets start of element.
     *
     * @param name element's name
     * @return the same handler of {@code null} if we do not want to handle this element and will wait for a next one.
     *         Returns new handler if we know how to process this element.
     */
    XmlNodeHandler onStartElement(String name);

    void onAttributes(Map<String, String> attributes);

    /**
     * Called when parser has got text of element.
     *
     * @param text method to process text
     */
    void onText(Supplier<String> text);

    /**
     * Called when parser exits from element.
     *
     * @param parent handler of parent element
     */
    void onEndElement(XmlNodeHandler parent);

    /**
     * Called when parser enters into a new nested element which is going to be skipped.
     *
     * @return current depth relative to the element matched to the handler
     */
    int down();

    /**
     * Called when parser exits from a nested element which is going to be skipped.
     * @return current depth relative to the element matched to the handler
     */
    int up();

    /**
     * Returns {@code true} while parser processes matching element.
     *
     * @return true if parser stays on element matching to the handler
     */
    boolean isActive();

    /**
     * Defines whether the handler needs node attributes.
     *
     * @return true to fill in attributes
     */
    boolean needAttributes();
}
