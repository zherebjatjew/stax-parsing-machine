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
