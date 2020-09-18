Overview
========

Stream XML parsers are in general faster than DOM-based parsers and less memory consuming.
However, they have significant drawbacks:
  * require very complex code to convert stream of tags to target data structure,
  * do not allow rewinding to an element.
  
This library helps to solve at least the first problem.
It provides to you an instrument for building a state machine
and easily operate with xml nodes respecting document structure.


How to use
==========

There are two main components: state machine builder and xml parser.
The parser takes the state machine and runs it against given XML.
Parser can use any Stax-based xml reader. In tests, I use the native one,
`java.xml.stream.XMLStreamReader`.

Let's go to the code. Imagine you have an xml:
```xml
<library>
    <book language="en">
        <title>American Gods</title>
        <author>Neil Gaiman</author>
    </book>
    <book language="pl">
        <author>Chuck Palahniuk</author>
        <title>Fight Club</title>
    </book>
</library>
```
and you need to print out authors and titles. With the tool
you can do it easily:
```java
try (StringReader reader = new StringReader(xml)) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance().createXMLStreamReader(reader));
    parser.read(RootHandler.instance(
        "library", r -> r.then("book")
        .or("author", x -> x.text(System.out::println))
        .or("title",  x -> x.text(System.out::println)))
    );
}

```
it will print out
```bash
American Gods
Neil Gaiman
Chuck Palahniuk
Fight Club
```

Also, you can chain you tags if you do not need to process the intermediate ones.
This is true for both `RootHandler.instance` and `Handler.then`:
```java
try (StringReader reader = new StringReader(xml)) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance().createXMLStreamReader(reader));
    parser.read(RootHandler.instance(
        "library/book", r -> r
        .or("author", x -> x.text(System.out::println))
        .or("title",  x -> x.text(System.out::println)))
    );
}

```

Value propagation
---

You can notice that it is not quite fancy because authors and titles go in a random order.
What if you have to prepare a CSV where first column contains authors and second column
contains book titles?

To solve that we can take advantage of a powerful feature called propagation.
It tells the machine to publish text of a node to its parent.
```java
try (StringReader reader = new StringReader(xml)) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance().createXMLStreamReader(reader));
    parser.read(RootHandler.instance(
        "library", r -> r.then("book")
        .or("author", x -> Handler::propagate)
        .or("title",  x -> Handler::propagate)
        .close(book -> System.out.println(book.getProperty("author") + ',' + book.getProperty("title"))))
    );
}
```
Output:
```text
Neil Gaiman,American Gods
Chuck Palahniuk,Fight Club
```
Values can be propagated further. In this case their names get path reflecting source of the value.
E.g., in the snippet above, if you propagated book node, you can address values from library node
as `book/author` and `book/title`.


Attributes
---

By default, the parser skips attributes for the sake of performance. You can turn on attribute fetching
explicitly for separate nodes.

Attributes can be propagated. Let's filter from our books only English versions. Notice
new `withAttributes` instruction, and that attribute is referenced with '@' prefix.

```java
try (StringReader reader = new StringReader(xml)) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance().createXMLStreamReader(reader));
    parser.read(RootHandler.instance(
        "library", r -> r.then("book").withAttributes()
        .or("author", x -> Handler::propagate)
        .or("title",  x -> Handler::propagate)
        .close(book -> {
            if ("en".equals(book.getProperty("@language") {
                System.out.println(book.getProperty("author") + ',' + book.getProperty("title"))))
            }
        }
    );
}
``` 
Ouput:
```text
Neil Gaiman,American Gods
```

Assumptions
---
Assumption to a node allows to skip child nodes conditionally. To the moment of executing assumption,
attributes of the node are already populated (if you did not forget to call `withAttributes`).
Child node does not take part in the decision.

The example above can be more concise by using assumptions:
```java
try (StringReader reader = new StringReader(xml)) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance().createXMLStreamReader(reader));
    parser.read(RootHandler.instance(
        "library", r -> r.then("book").withAttributes()
        .assume(f -> "en".equals(f.getProperty("@language"))
        .or("author", x -> Handler::propagate)
        .or("title",  x -> Handler::propagate)
        .close(book -> System.out.println(book.getProperty("author") + ',' + book.getProperty("title"))))
    );
}
``` 
Ouput:
```text
Neil Gaiman,American Gods
