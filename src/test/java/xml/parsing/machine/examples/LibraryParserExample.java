package xml.parsing.machine.examples;

import org.junit.jupiter.api.Test;
import xml.parsing.machine.api.Handler;
import xml.parsing.machine.api.RootHandler;
import xml.parsing.machine.stax.StaxParser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LibraryParserExample {
    private static class BookBuilder {
        private final List<Book> books = new ArrayList<>();
        void newBook() { books.add(0, new Book()); }
        void setAuthor(String value) { books.get(0).setAuthor(value); }
        void setTitle(String value) { books.get(0).setTitle(value); }
        void addFile(String type, String path) {
            if ("cover".equals(type)) {
                books.get(0).setCoverFilename(path);
            } else if ("illustration".equals(type)) {
                books.get(0).getIllustrations().add(path);
            }
        }
        List<Book> getBooks() { return books; }
    }

    @Test
    public void populateBooks() throws IOException, XMLStreamException {
        BookBuilder builder = new BookBuilder();
        File f = new File(getClass().getClassLoader().getResource("books.xml").getFile());
        try (FileInputStream reader = new FileInputStream(f)) {
            XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
            StaxParser parser = new StaxParser(xmlFactory.createXMLStreamReader(reader));
            RootHandler root = RootHandler.instance();
            // Expect path library/book
            root.then("library").then("book")
                // When we met a book, we create a new Book instance to fill later
                .open(x -> builder.newBook())
                // We will be reading 'title', 'author', and 'file' children of book element
                .or("title", x -> x.text(builder::setTitle))
                .or("author", x -> x.text(builder::setAuthor))
                // As file can be cover or illustration, we collect it's child elements...
                .or("file", x -> x
                    .or("type", Handler::propagate)
                    .or("path", Handler::propagate)
                    // ... and process respecting the values we've got
                    .close(h -> builder.addFile(h.getProperty("type"), h.getProperty("path")))
                );
            parser.read(root);
        }
        builder.getBooks().stream().map(Book::toString).forEach(System.out::println);
    }

}
