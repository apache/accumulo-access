<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership. The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

-->

# AccessExpression Specification

This document specifies the format of an Apache Accumulo AccessExpression. An AccessExpression is an
encoding of a boolean expression that defines the attributes an entity requires to access specific 
data.

## Concepts

* AccessExpression - A boolean expression detailing the attributes needed to access an object (e.g. Key/Value pair in Accumulo).
* Authorizations - A set of attributes, typically attributed to the entity trying to access an object.
* AccessEvaluator - An object that determines if an entity can access an object based on the 
entity's Authorizations and the object's AccessExpression.

## Syntax

The formal definition of the AccessExpression UTF-8 string representation is provided by
the following [ABNF][1]:

```ABNF
access-expression       = [expression] ; empty string is a valid access expression

expression              =  (access-token / paren-expression) [and-expression / or-expression]

paren-expression        =  "(" expression ")"

and-expression          =  "&" (access-token / paren-expression) [and-expression]

or-expression           =  "|" (access-token / paren-expression) [or-expression]

access-token            = 1*( ALPHA / DIGIT / "_" / "-" / "." / ":" / slash )
access-token            =/ DQUOTE 1*(utf8-subset / escaped) DQUOTE

utf8-subset             = %x20-21 / %x23-5B / %x5D-7E / unicode-beyond-ascii ; utf8 minus '"' and '\'
unicode-beyond-ascii    = %x0080-D7FF / %xE000-10FFFF
escaped                 = "\" DQUOTE / "\\"
slash                   = "/"
```

### Examples of Proper Expressions

 * `BLUE`
 * `RED&BLUE`
 * `RED&BLUE&GREEN`
 * `(RED&BLUE)|(GREEN&(PINK|PURPLE))`

### Examples of Improper Expressions

* `&BLUE` : Must start with an access token or a paren expression.
* `(RED&BLUE)|` : An access token or paren expression must follow a `|`.
* `RED&BLUE|GREEN` : Once a `&` is seen, then can only have `&` and not `|`, unless using parenthesis. 
* `RED|BLUE&GREEN` : Once a `|` is seen, then can only have `|`and not `&`, unless using parenthesis.

## Serialization

An AccessExpression is a UTF-8 string. It can be serialized using a byte array as long as it
can be deserialized back into the same UTF-8 string.

## Evaluation

The evaluation process combines [set][2] existence checks with [boolean algebra][3]. Specifically, 
AccessExpressions use:

 * The symbol `&` for [logical conjunction][4] (`∧` in boolean algebra).
 * The symbol `|` for [logical disjunction][5] (`∨` in boolean algebra).

When evaluating an AccessExpression, existence checks are done against an
entities Authorizations. The following is the algorithm for evaluation of an
AccessExpression.

 1. For each access-token in an AccessExpression check if it exists in the
    entities Authorizations. Replace the access-token with `true` if it
    exists in the set and `false` otherwise.
 2. Evaluate the resulting expression using boolean algebra. If the result is true, the entity can
    access the data associated with the AccessExpression.

The following is an example of evaluating the AccessExpression
`RED&(BLUE|GREEN)` using boolean algebra for an entity with the Authorizations
 `{RED,GREEN}`. In the example below `RED ∈ {RED,GREEN}` translates to does
`RED` exist in the set `{RED,GREEN}` which it does, so it is true.

 1. RED ∈ {RED,GREEN} ∧ ( BLUE ∈ {RED,GREEN} ∨ GREEN ∈ {RED,GREEN} )
 2. true  ∧ ( false ∨ true )

Since `true  ∧ ( false ∨ true )` is true then the entity with Authorizations of
`{RED,GREEN}` can access data labeled with the AccessExpression
`RED&(BLUE|GREEN)`. The AccessExpression `(RED&BLUE)|(GREEN&PINK)` is an
example of an AccessExpression that is false for an entity with Authorizations of
`{RED,GREEN}` and it would look like the following using boolean algebra.

 1. ( RED ∈ {RED,GREEN} ∧ BLUE ∈ {RED,GREEN} ) ∨ ( GREEN ∈ {RED,GREEN} ∧ PINK ∈
    {RED,GREEN} )
 2. ( true ∧ false ) ∨ ( true ∧ false )

An entity with empty Authorizations can only access data associated with an empty access 
expression. This is because an empty AccessExpression always evaluates to true.

## Escaping

Access tokens can only contain alphanumeric characters or the characters
`_`,`-`,`.`,`:`, or `/` unless quoted using `"`. Within quotes, the characters
`"` and `\` must escaped by prefixing them with `\`. For example, to use `abc\xyz` as
an access-token, it would need to be quoted and escaped like `"abc\\xyz"`. When
checking if an access-token exists in the entities Authorizations, it must
be unquoted and unescaped.

Evaluating `"abc!12"&"abc\\xyz"&GHI` for an entity with Authorizations of
`{abc\xyz,abc!12}` looks like the following in boolean algebra which evaluates
to `false`.

 1. abc!12 ∈ {abc\xyz,abc!12} ∧ abc\xyz ∈ {abc\xyz,abc!12} ∧ GHI ∈
    {abc\xyz,abc!12}
 2. true ∧ true ∧ false

It's important to note that when verifying the existence of "abc\\xyz" in the set of authorizations
within the Authorizations object, the token is unquoted, and the `\` character is unescaped.

[1]: https://www.rfc-editor.org/rfc/rfc5234
[2]: https://en.wikipedia.org/wiki/Set_(mathematics)
[3]: https://en.wikipedia.org/wiki/Boolean_algebra
[4]: https://en.wikipedia.org/wiki/Logical_conjunction
[5]: https://en.wikipedia.org/wiki/Logical_disjunction
