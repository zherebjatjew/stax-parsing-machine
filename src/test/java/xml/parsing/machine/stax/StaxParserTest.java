package xml.parsing.machine.stax;

import org.junit.jupiter.api.Test;
import xml.parsing.machine.api.Handler;

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
        StringReader reader = new StringReader("<item>value</item>");
        StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
        Handler root = Handler.root();
        List<String> books = new ArrayList<>();
        root.then("item").text(books::add);
        parser.read(root);
        assertEquals(1, books.size());
        assertEquals("value", books.get(0));
    }

    @Test
    public void shouldProcessMultipleElement() throws XMLStreamException {
        StringReader reader = new StringReader("<library><book>text1</book><book>text2</book></library>");
        StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
        Handler root = Handler.root();
        List<String> books = new ArrayList<>();
        root.then("library").then("book").text(books::add);
        parser.read(root);
        assertEquals(2, books.size());
        assertTrue(books.contains("text1"));
        assertTrue(books.contains("text2"));
    }

    @Test
    public void shouldProcessLongPaths() throws XMLStreamException {
        StringReader reader = new StringReader("<city><library><book>value</book></library></city>");
        StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
        Handler root = Handler.root();
        List<String> books = new ArrayList<>();
        root.then("city").then("library").then("book").text(books::add);
        parser.read(root);
        assertEquals(1, books.size());
        assertEquals("value", books.get(0));
    }
}