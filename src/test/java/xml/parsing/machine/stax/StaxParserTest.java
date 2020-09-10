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
            RootHandler root = RootHandler.instance();
            root.then("item").text(books::add);
            parser.read(root);
        }
        assertEquals(1, books.size());
        assertEquals("value", books.get(0));
    }

    @Test
    public void shouldProcessMultipleElement() throws XMLStreamException {
        List<String> books = new ArrayList<>();
        try (StringReader reader = new StringReader("<library><book>text1</book><book>text2</book></library>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            RootHandler root = RootHandler.instance();
            root.then("library").then("book").text(books::add);
            parser.read(root);
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
            RootHandler root = RootHandler.instance();
            root.then("library").then("book").text(books::add);
            parser.read(root);
        }
        assertEquals(1, books.size());
        assertTrue(books.contains("text1"));
    }

    @Test
    public void shouldProcessLongPaths() throws XMLStreamException {
        List<String> books = new ArrayList<>();
        try (StringReader reader = new StringReader("<city><library><book>value</book></library></city>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            RootHandler root = RootHandler.instance();
            root.then("city").then("library").then("book").text(books::add);
            parser.read(root);
        }
        assertEquals(1, books.size());
        assertEquals("value", books.get(0));
    }

    @Test
    public void shouldProcessMultipleOptions() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book><author>a</author><title>x</title></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            RootHandler root = RootHandler.instance();
            root.then("book")
                    .or("author", h -> h.text(s -> fields.add("author=" + s)))
                    .or("title", h -> h.text(s -> fields.add("title=" + s)));
            parser.read(root);
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
            RootHandler root = RootHandler.instance();
            root.then("book").close(h -> fields.add(h.getProperty("author"))).then("author").propagate();
            parser.read(root);
        }
        assertEquals(1, fields.size());
        assertEquals("a", fields.get(0));
    }

    @Test
    public void shouldPropagateMultipleValues() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book><author>a</author><title>x</title></book>")) {
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            RootHandler root = RootHandler.instance();
            root.then("book")
                    .or("author", Handler::propagate)
                    .or("title", Handler::propagate)
                    .close(h -> {
                        fields.add("author=" + h.getProperty("author"));
                        fields.add("title=" + h.getProperty("title"));
                    });
            parser.read(root);
        }
        assertEquals(2, fields.size());
        assertTrue(fields.contains("author=a"));
        assertTrue(fields.contains("title=x"));
    }

    @Test
    public void shouldReportForgottenPropagate() throws XMLStreamException {
        List<String> fields = new ArrayList<>();
        try (StringReader reader = new StringReader("<book></book>")) {
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
            RootHandler root = RootHandler.instance();
            root.then("book")
                    .close(h -> {
                        fields.add("author=" + h.getProperty("meta/author"));
                        fields.add("title=" + h.getProperty("meta/title"));
                    }).then("meta")
                    .or("author", Handler::propagate)
                    .or("title", Handler::propagate)
                    .propagate();
            parser.read(root);
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
            RootHandler root = RootHandler.instance();
            root.then("book").text(fields::add);
            parser.read(root);
        }
        assertEquals("text", fields.get(0));
    }
}