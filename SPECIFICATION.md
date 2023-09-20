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

This document specifies the format of an Apache Accumulo AccessExpression. An AccessExpression
is an encoding of a boolean expression of the attributes that a subject is required to have to
access a particular piece of data.

## Syntax

The formal definition of the AccessExpression UTF-8 string representation is provided by
the following [ABNF][1]:

```
access-expression       = [expression] ; empty string is a valid access expression

expression              =  (access-token / paren-expression) [and-expression / or-expression]

paren-expression        =  "(" expression ")"

and-expression          =  "&" (access-token / paren-expression) [and-expression]

or-expression           =  "|" (access-token / paren-expression) [or-expression]

access-token            = 1*( ALPHA / DIGIT / "_" / "-" / "." / ":" / slash )
access-token            =/ DQUOTE 1*(utf8-subset / escaped) DQUOTE

utf8-subset             = %x20-21 / %x23-5B / %5D-7E / UVCHARBEYONDASCII ; utf8 minus '"' and '\'
escaped                 = "\" DQUOTE / "\\"
slash                   = "/"
```

The definition of utf8 was borrowed from this [ietf document][2]. TODO that doc defines unicode and not utf8

## Serialization

An AccessExpression is a UTF-8 string. It can be serialized using a byte array as long as it
can be deserialized back into the same UTF-8 string.

## Evaluation

Evaluation of access expressions performs a combination of [set][3] existence
checks and [boolean algebra][4]. Access expression use the following from
boolean algebra.

 * The symbol `&` in an access expression represents [logical conjunction][5]
   which is represented in a boolean algebra as `∧`.
 * The symbol `|` in an access expression represents [logical disjunction][6]
   which is represented in a boolean algebra as `∨`.

When evaluating an access expression set existence checks are done against a
subjects set of authorizations. The following is an algorithm for evaluation an
access expression.

 1. For each access-token in an access expression check if it exists in the
    subjects set of authorizations. Replace the access-token with `true` if it
    exists in the set and `false` otherwise.
 2. Evaluate the expression using boolean algebra and only if its true can the
    subject access the data labeled with the access expression.

The following is an example of evaluating the access expression
`RED&(BLUE|GREEN)` using boolean algebra for a subject with the authorization
set `{RED,GREEN}`. In the example below `RED ∈ {RED,GREEN}` translates to does
`RED` exist in the set `{RED,GREEN}` which it does, so it is true.

 1. RED ∈ {RED,GREEN} ∧ ( BLUE ∈ {RED,GREEN} ∨ GREEN ∈ {RED,GREEN} )
 2. true  ∧ ( false ∨ true )

Since `true  ∧ ( false ∨ true )` is true then the subject with authorizations
`{RED,GREEN}` can access data labeled with the access expression
`RED&(BLUE|GREEN)`. The access expression `(RED&BLUE)|(GREEN&PINK)` is an
example of an access expression that is false for a subject with authorizations
`{RED,GREEN}` and it would look like the following using boolean algebra.

 1. ( RED ∈ {RED,GREEN} ∧ BLUE ∈ {RED,GREEN} ) ∨ ( GREEN ∈ {RED,GREEN} ∧ PINK ∈
    {RED,GREEN} )
 2. ( true ∧ false ) ∨ ( true ∧ false )

An empty access expression always evaluates to true and this is only thing a
subject with the empty set of authorizations can access.

## Escaping

Access tokens can only contain alpha numeric characters or the characters
`_`,`-`,`.`,`:`, or `/` unless quoted using `"`. Within quotes the characters
`"` and `\` must escaped by prefixing with `\`. For example to use `abc\xyz` as
an access-token it would need to be quoted and escaped like `"abc\\xyz"`. When
checking if an access-token exists in the subjects authorizations set it must
be unquoted and unescaped.

Evaluating `"abc!12"&"abc\\xyz"&GHI`for a subject with authorizations
`{abc\xyz,abc!12}` looks like the following in boolean algebra which evaluates
to `false`.

 1. abc!12 ∈ {abc\xyz,abc!12} ∧ abc\xyz ∈ {abc\xyz,abc!12} ∧ GHI ∈
    {abc\xyz,abc!12}
 2. true ∧ true ∧ false

Notice above when checking if `"abc\\xyz"` exist in the set that it is unquoted
and the `\` character is unescaped.

[1]: https://www.rfc-editor.org/rfc/rfc5234
[2]: https://datatracker.ietf.org/doc/html/draft-seantek-unicode-in-abnf-03#section-4.2
[3]: https://en.wikipedia.org/wiki/Set_(mathematics)
[4]: https://en.wikipedia.org/wiki/Boolean_algebra
[5]: https://en.wikipedia.org/wiki/Logical_conjunction
[6]: https://en.wikipedia.org/wiki/Logical_disjunction

