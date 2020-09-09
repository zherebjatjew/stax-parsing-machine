package xml.parsing.machine.api;


import javax.xml.stream.XMLStreamConstants;
import java.util.ArrayDeque;
import java.util.Deque;

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

    protected abstract int next();

    protected abstract String getElementName();

    protected abstract String getElementText();
}
