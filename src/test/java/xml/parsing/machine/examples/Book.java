package xml.parsing.machine.examples;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String author;
    private String title;
    private String coverFilename;
    private final List<String> illustrations = new ArrayList<>();

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverFilename() {
        return coverFilename;
    }

    public void setCoverFilename(String coverFilename) {
        this.coverFilename = coverFilename;
    }

    public List<String> getIllustrations() {
        return illustrations;
    }

    @Override
    public String toString() {
        return String.format("Book(author=%s, title=%s, cover=%s, illustrations=(%s))",
                getAuthor(), getTitle(), getCoverFilename(), String.join(", ", getIllustrations()));
    }
}
