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
    <book>
        <title>American Gods</title>
        <author>Neil Gaiman</author>
    </book>
    <book>
        <author>Chuck Palahniuk</author>
        <title>Fight Club</title>
    </book>
</library>
```
and you need to print out authors and titles. With the tool
you can do it easily:
```java
try (StringReader reader = new StringReader(xml) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance()
        .createXMLStreamReader(reader));
    RootHandler root = RootHandler.instance();
    root.then("library").then("book")
        .or("author", x -> x.text(System.out::println))
        .or("title",  x -> x.text(System.out::println));
    parser.read(root);
}

```
it will print out
```bash
American Gods
Neil Gaiman
Chuck Palahniuk
Fight Club
```

You can notice that it is not quite fancy because authors and titles go in a random order.
What if you have to prepare a CSV where first column contains authors and second column
contains book titles?

To solve that we can take advantage of a powerful feature called propagation.
It tells the machine to publish text of a node to its parent.
```java
try (StringReader reader = new StringReader(xml) {
    StaxParser parser = new StaxParser(XMLInputFactory.newInstance()
        .createXMLStreamReader(reader));
    RootHandler root = RootHandler.instance();
    root.then("library").then("book")
        .or("author", x -> Handler::propagate)
        .or("title",  x -> Handler::propagate)
        .close(book -> System.out.println(book.getProperty("author") + ';' + book.getProperty("title")));
    parser.read(root);
}
```
Output:
```text
Neil Gaiman;American Gods
Chuck Palahniuk;Fight Club
```
Values can be propagated further. In this case their names get path reflecting source of the value.
E.g., in the snippet above, if you propagated book node, you can address values from library node
as `book/author` and `book/title`.


Limitations
===========

1. The current version does not support attributes.
1. The machine matches nodes by full node name.
 You cannot use patterns in methods `Handler#then`, `Handler#or`.
1. I did not publish the code to maven repository so you cannot include it as a dependency.