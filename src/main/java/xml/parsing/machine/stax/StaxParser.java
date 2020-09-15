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
package xml.parsing.machine.stax;

import xml.parsing.machine.api.AbstractXmlParser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of parser for streaming xml parser. See {@link XMLStreamReader}.
 */
public class StaxParser extends AbstractXmlParser {

    private final XMLStreamReader reader;

    public StaxParser(XMLStreamReader reader) {
        this.reader = reader;
    }

    @Override
    protected int next() {
        try {
            return reader.next();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getElementName() {
        return reader.getLocalName();
    }

    @Override
    protected String getElementText() {
        return reader.getText();
    }

    @Override
    protected Map<String, String> getAttributes() {
        int count = reader.getAttributeCount();
        Map<String, String> attributes = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }
        return attributes;
    }

}
