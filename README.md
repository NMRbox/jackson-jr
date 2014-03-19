# Overview

Jackson jr is a compact alternative to full [Jackson Databind](../../../jackson-databind) component.
It implements a subset of functionality, for example for cases where:

1. Size of jar matters (jackson-jr size is about 50kB)
2. Startup time matters (jackson-jr has very low initialization overhead)

In addition to basic datatypes (core JDK types like `List`s, `Map`s, wrapper types),
package supports reading and writing of standard Java Beans (with JDK introspection; that is,
subset of POJOs that define setters/getters; no field access is used).

Jackson jr also adds  `composer` implementation that can be used to
construct JSON output with builder-style API, but without necessarily having
to build an in-memory representation: instead, it can directly use `streaming-api`
for direct output. It will be, however, also possible to build actual in-memory
JSON `String` or `byte[]` representation, if that is preferable.

Jackson jr artifact itself is currently about `50 kB` in size, and only depends on
[Jackson Streaming API](../../../jackson-core) package.
Combined size, for "all" jar is bit over 250 kilobytes (streaming API is about 200kB),
for use cases where a single jar is preferred over more modular approach.
Finally, use of jar minimizers like [ProGuard](http://proguard.sourceforge.net/) can bring the jar
size down even further.

## Usage

### Reading/writing Simple Objects, Beans

Functionality of this package is contained in Java package `com.fasterxml.jackson.jr.ob`.

All functionality is accessed through main `JSON` Object; you can either used singleton `JSON.std`,
or construct individual objects -- either way, `JSON` instances are ALWAYS immutable and hence thread-safe.

We can start by reading JSON

```java
String INPUT = "{\"a\":[1,2,{\"b\":true},3],\"c\":3}";
Object ob = JSON.std.from(INPUT);
// or
Map<String,Object> map = JSON.std.mapFrom(INPUT);
// or
MyBean bean = JSON.std.beanFrom(MyBean.class, INPUT);
```

as well as writing Objects as JSON:

```java
String json = JSON.std.asString(map);
JSON.std.write(ob, new File("/tmp/stuff.json");
// and with indentation; but skip writing of null properties
byte[] bytes = JSON.std
    .with(Feature.PRETTY_PRINT_OUTPUT)
    .without(Feature.WRITE_NULL_PROPERTIES)
    .asBytes(bean);
```

### Writing with composers

An alternative method exists for writing: "fluent" style output can be used as follows:

```java
String json = JSON.std
  .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
  .composeString()
  .startObject()
    .put("a", 1)
    .startArrayField("arr")
      .add(1).add(2).add(3)
    .end()
    .startObjectField("ob")
      .put("x", 3)
      .put("y", 4)
      .startArrayField("args").add("none").end()
    .end()
    .put("last", true)
  .end()
  .finish();
```

would produce (since pretty-printing is enabled)

```json
{
  "a" : 1,
  "arr" : [1,2,3],
  "ob" : {
    "x" : 3,
    "y" : 4,
    "args" : ["none"]
  },
  "last" : true
}
```


## License

Good old [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

## Get it!

The first official version (2.3.0) has been released, so you can use Maven dependency like:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.jr</groupId>
  <artifactId>jackson-jr-objects</artifactId>
  <version>2.3.0</version>
</dependency>
```

and then you can also download jars via [Central Maven repository](http://repo1.maven.org/maven2/com/fasterxml/jackson/simple/).

Or you can also clone the project and build it locally with `mvn clean install`.
