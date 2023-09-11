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

# Accumulo Access Control Library

This library is a stand-alone java library that provides the functionality of the Apache Accumulo ColumnVisibility for use outside of Accumulo.

## Concepts

 * AccessExpression - a boolean expression of attributes required to access an object (e.g. Key/Value pair in Accumulo). See SPECIFICATION.md.
 * Authorizations - a set of attributes, typically attributed to the entity trying to access an object.
 * AccessEvaluator - An object that determines if the entity can access the object using the entity's Authorizations and the objects AccessExpression.

## Goals

 * Create a standalone java library that offers the Accumulo visibility functionality
 * Support the same syntax and semantics as ColumnVisibility and VisibilityEvaluator initially.  This will allow ColumnVisibility and VisibilityEvaluator to adapt to use this new library for their implementation.
 * Have no dependencies for this new library
 * Use no external types (like Hadoop types) in its API.
 * Use semantic versioning.

## Public API

The following types constitute the public API of this library. All other types are package private and are not part of the public API.

  * [IllegalAccessExpressionException](src/main/java/org/apache/accumulo/access/IllegalAccessExpressionException.java).
  * [AccessEvaluator](src/main/java/org/apache/accumulo/access/AccessEvaluator.java).
  * [AccessExpression](src/main/java/org/apache/accumulo/access/AccessExpression.java).
  * [Authorizations](src/main/java/org/apache/accumulo/access/Authorizations.java).

For an example of using this library see the [unit test](src/test/java/org/apache/accumulo/access/AccessEvaluatorTest.java).

See the [specification](SPECIFICATION.md) for details about access expressions.

## Usage in other languages

The src/main/antlr4/AccessExpression.g4 file is an [ANTLR v4](https://github.com/antlr/antlr4) grammar. ANTLR v4 provides support for several other
programming languages. Users of this library should be able to generate ANTLR parsers using the provided grammar to validate that the AccessExpression's
that they are creating are valid.
