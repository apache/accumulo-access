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

# accumulo-access
Accumulo Access Control Library

This project is a work in progress with the following goals.

 * Create a standalone java library that offers the Accumulo visibility functionality
 * Support the same syntax and semantics as ColumnVisibility and VisibilityEvaluator initially.  This will allow ColumnVisibility and VisibilityEvaluator to adapt to use this new library for their implementation.
 * Have no dependencies for this new library
 * Use no external types (like Hadoop types) in its API.
 * Use semantic versioning.

The following types constitute the public API of this library.  All other types are package private and are not part of the public API.

  * [IllegalAccessExpressionException](src/main/java/org/apache/accumulo/access/IllegalAccessExpressionException.java).
  * [AccessEvaluator](src/main/java/org/apache/accumulo/access/AccessEvaluator.java).
  * [AccessExpression](src/main/java/org/apache/accumulo/access/AccessExpression.java).
  * [Authorizations](src/main/java/org/apache/accumulo/access/Authorizations.java).

For an example of using this library see the [unit test](src/test/java/org/apache/accumulo/access/AccessEvaluatorTest.java).

See the [specification][SPECIFICATION.md] for details about access expressions.
