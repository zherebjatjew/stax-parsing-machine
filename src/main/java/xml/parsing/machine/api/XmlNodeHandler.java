package xml.parsing.machine.api;

import java.util.function.Supplier;

public interface XmlNodeHandler {
    XmlNodeHandler onStartElement(String name);
    void onText(Supplier<String> text);
    void onEndElement(XmlNodeHandler parent);
    int down();
    int up();
    boolean isActive();
}
