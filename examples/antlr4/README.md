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
# ANTLR Example

This contrib example contains an [ANTLRv4](https://www.antlr.org/) grammar file (see [AccessExpression.g4](src/main/antlr4/AccessExpression.g4)) that can be used to create AccessExpression parsers in languages supported by ANTLRv4. For example, a project could use this to validate that AccessExpression's are valid before sending them to Accumulo in the ColumnVisibility field of the Key.

An example [parser](src/test/java/org/apache/accumulo/access/grammar/antlr/AccessExpressionAntlrParser.java) and [evaluator](src/test/java/org/apache/accumulo/access/grammar/antlr/AccessExpressionAntlrEvaluator.java) are used when building this project to confirm that the parsing and evaluation are consistent with the reference Java implementation.

## Running the Benchmark

ANTLR was evaluated as a replacement for the existing custom Java parser, but it doesn't parse as fast as the custom implementation. You can view the performance differences by running the JMH benchmark in this contrib project and the one in the main project.

To run the benchmark in this project:
```
mvn clean package
mvn exec:exec -Dexec.executable="java" -Dexec.classpathScope=test -Dexec.args="-classpath %classpath org.apache.accumulo.access.grammar.antlr.AccessExpressionAntlrBenchmark"
```
