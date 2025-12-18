<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->

# Accumulo Access Library

Java library that provides the same functionality, semantics, and syntax as the
Apache Accumulo [ColumnVisibility][1] and [VisibilityEvaluator][2] classes.
This functionality is provided in a standalone java library that has no
dependencies (for example no Hadoop, Zookeeper, Thrift, etc dependencies).

For a conceptual overview of what an access expression is, see the
[specification](SPECIFICATION.md) document. See the [Getting Started
section](#getting-started) for an example of how to use this java library.

## Public API

The following types constitute the public API of this library. All other types
are package private and are not part of the public API.

  * [InvalidAccessExpressionException](src/main/java/org/apache/accumulo/access/InvalidAccessExpressionException.java).
  * [AccessEvaluator](src/main/java/org/apache/accumulo/access/AccessEvaluator.java).
  * [AccessExpression](src/main/java/org/apache/accumulo/access/AccessExpression.java).
  * [ParsedAccessExpression](src/main/java/org/apache/accumulo/access/ParsedAccessExpression.java).
  * [ExpressionType](src/main/java/org/apache/accumulo/access/ParsedAccessExpression.java).
  * [Authorizations](src/main/java/org/apache/accumulo/access/Authorizations.java).

## Getting Started

Add the library to your CLASSPATH. For Maven, use:

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.accumulo</groupId>
    <artifactId>accumulo-access</artifactId>
    <version>${version.accumulo-access}</version>
  </dependency>
</dependencies>
```

## Running the [Examples](src/test/java/example)

To run [AccessExample](examples/src/main/java/org/apache/accumulo/access/examples/AccessExample.java)

```
mvn clean package

java --module-path=core/target/accumulo-access-core-1.0.0-SNAPSHOT.jar:examples/target/accumulo-access-examples-1.0.0-SNAPSHOT.jar  --module=accumulo.access.examples/org.apache.accumulo.access.examples.AccessExample
java --module-path=core/target/accumulo-access-core-1.0.0-SNAPSHOT.jar:examples/target/accumulo-access-examples-1.0.0-SNAPSHOT.jar  --module=accumulo.access.examples/org.apache.accumulo.access.examples.AccessExample RED BLUE
```

Note that `data6` is always returned, because it has no access expression. And
remember, authorizations are case-sensitive.

To run [ParseExamples](examples/src/main/java/org/apache/accumulo/access/examples/ParseExamples.java)

```
mvn clean package
java --module-path=core/target/accumulo-access-core-1.0.0-SNAPSHOT.jar:examples/target/accumulo-access-examples-1.0.0-SNAPSHOT.jar --module=accumulo.access.examples/org.apache.accumulo.access.examples.ParseExamples
```

For an ANTLRv4 example, see its [README](antlr4-example/README.md).

## Running the Benchmark

This project includes a JMH Benchmark. To run it:

```
mvn clean verify -pl core -Pbenchmark
```


[1]: https://github.com/apache/accumulo/blob/rel/2.1.2/core/src/main/java/org/apache/accumulo/core/security/ColumnVisibility.java
[2]: https://github.com/apache/accumulo/blob/rel/2.1.2/core/src/main/java/org/apache/accumulo/core/security/VisibilityEvaluator.java
