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

import org.junit.jupiter.api.Test;
import xml.parsing.machine.api.Handler;
import xml.parsing.machine.api.RootHandler;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaxParserTest {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();

    @Test
    public void shouldProcessSingleElement() throws XMLStreamException {
        List<String> books = new ArrayList<>();
        try (StringReader reader = new StringReader("<item>value</item>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("item", h -> h.text(books::add)));
        }
        assertEquals(1, books.size());
        assertEquals("value", books.get(0));
    }

    @Test
    public void shouldProcessMultipleElement() throws XMLStreamException {
        List<String> books = new ArrayList<>();
        try (StringReader reader = new StringReader("<library><book>text1</book><book>text2</book></library>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("library", h -> h.then("book").text(books::add)));
        }
        assertEquals(2, books.size());
        assertTrue(books.contains("text1"));
        assertTrue(books.contains("text2"));
    }

    @Test
    public void shouldSkipNodes() throws XMLStreamException {
        List<String> books = new ArrayList<>();
        try (StringReader reader = new StringReader("<library><disc>text0</disc><book>text1</book></library>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("library", h -> h.then("book").text(books::add)));
        }
        assertEquals(1, books.size());
        assertTrue(books.contains("text1"));
    }

    @Test
    public void shouldProcessLongPaths() throws XMLStreamException {
        List<String> books = new ArrayList<>();
        try (StringReader reader = new StringReader("<city><library><book>value</book></library></city>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("city", h -> h.then("library").then("book").text(books::add)));
        }
        assertEquals(1, books.size());
        assertEquals("value", books.get(0));
    }

    @Test
    public void shouldProcessMultipleOptions() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book><author>a</author><title>x</title></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("book", r -> r
                    .or("author", h -> h.text(s -> fields.add("author=" + s)))
                    .or("title", h -> h.text(s -> fields.add("title=" + s)))));
        }
        assertEquals(2, fields.size());
        assertTrue(fields.contains("author=a"));
        assertTrue(fields.contains("title=x"));
    }

    @Test
    public void shouldPropagateChildValue() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book><author>a</author></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("book", r -> r
                    .close(h -> fields.add(h.getProperty("author"))).then("author").propagate()));
        }
        assertEquals(1, fields.size());
        assertEquals("a", fields.get(0));
    }

    @Test
    public void shouldPropagateMultipleValues() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book><author>a</author><title>x</title></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance(
                    "book", r -> r
                    .or("author", Handler::propagate)
                    .or("title", Handler::propagate)
                    .close(h -> {
                        fields.add("author=" + h.getProperty("author"));
                        fields.add("title=" + h.getProperty("title"));
                    }))
            );
        }
        assertEquals(2, fields.size());
        assertTrue(fields.contains("author=a"));
        assertTrue(fields.contains("title=x"));
    }

    @Test
    public void shouldReportForgottenPropagate() throws XMLStreamException {
        try (StringReader reader = new StringReader("<book></book>")) {
            List<String> fields = new ArrayList<>();
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            RootHandler root = RootHandler.instance();
            root.then("book")
                    .or("author", x -> {})
                    .or("title", x -> {})
                    .close(h -> {
                        fields.add("author=" + h.getProperty("author"));
                        fields.add("title=" + h.getProperty("title"));
                    });
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> parser.read(root));
            assertEquals("None of the children provided a value. Did you forgot propagate()?", ex.getMessage());
        }
    }

    @Test
    public void shouldPropagateNestedValues() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book><meta><author>a</author><title>x</title></meta></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance(
                    "book", r -> r
                    .close(h -> {
                        fields.add("author=" + h.getProperty("meta/author"));
                        fields.add("title=" + h.getProperty("meta/title"));
                    }).then("meta")
                    .or("author", Handler::propagate)
                    .or("title", Handler::propagate)
                    .propagate())
            );
        }
        assertEquals(2, fields.size());
        assertTrue(fields.contains("author=a"));
        assertTrue(fields.contains("title=x"));
    }

    @Test
    public void shouldSkipAttributes() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book name='book title'>text</book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("book", r -> r.text(fields::add)));
        }
        assertEquals("text", fields.get(0));
    }

    @Test
    public void shouldReadAttributes() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book name='book title'>text</book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("book", r -> r
                    .withAttributes()
                    .close(h -> fields.add(h.getProperty("@name")))));
        }
        assertEquals("book title", fields.get(0));
    }

    @Test
    public void shouldReadAttributesFromMultipleNodes() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader(
                "<library><book name='book 1'>text</book><book name='book 2'>text</book></library>"))
        {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("library", b -> b.then("book")
                    .withAttributes()
                    .close(h -> fields.add("@name=" + h.getProperty("@name")))));
        }
        assertTrue(fields.contains("@name=book 1"));
        assertTrue(fields.contains("@name=book 2"));
    }

    @Test
    public void shouldCombineAttributesWithPropagatedValues() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book name='book title'><teaser>text</teaser></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("book", r -> r
                    .withAttributes()
                    .close(h -> {
                        fields.add("@name=" + h.getProperty("@name"));
                        fields.add("teaser=" + h.getProperty("teaser"));
                    })
                    .then("teaser").propagate()
            ));
        }
        assertTrue(fields.contains("@name=book title"));
        assertTrue(fields.contains("teaser=text"));
    }

    @Test
    public void shouldResetPropertiesOfPrefNode() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<books><book language='ru'><content>text 1</content></book><book><content>text 2</content></book></books>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("books", r -> r
                    .then("book").withAttributes().close(h -> {
                        if ("ru".equals(h.getProperty("@language"))) {
                            fields.add(h.getProperty("content"));
                        }
                    }).then("content").propagate()
            ));
        }
        assertTrue(fields.contains("text 1"));
        assertFalse(fields.contains("text 2"));
    }

    @Test
    public void shouldTakeOnlyNodesWhereAssumptionIsTrue() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<books><book language='ru'><content>text 1</content></book><book><content>text 2</content></book></books>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("books", r -> r
                    .then("book").withAttributes()
                    .assume(book -> "ru".equals(book.getProperty("@language")))
                    .then("content").text(fields::add)
            ));
        }
        assertTrue(fields.contains("text 1"));
        assertFalse(fields.contains("text 2"));
    }

    @Test
    public void shouldRecognizeTokenChains() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<city><libs><library><books><book><text>content</text></book></books><text>should not appear in fields</text></library></libs></city>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            parser.read(RootHandler.instance("city", r -> r
                    .then("libs/library/books/book/text").text(fields::add)
            ));
        }
        assertEquals(1, fields.size());
        assertTrue(fields.contains("content"));
    }
}