package xml.parsing.machine.api;

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
}
