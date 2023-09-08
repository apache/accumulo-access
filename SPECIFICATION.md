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

# AccessExpression Specification

This document specifies the format of an Apache Accumulo AccessExpression. An AccessExpression
is an encoding of a boolean expression of the attributes that a subject is required to have to
access a particular piece of data.

## Syntax

The formal definition of the AccessExpression UTF-8 string representation is provided by
the following [ABNF][1]:

```
access-expression       = [expression] ; empty string is a valid access expression

expression              =  and-expression / or-expression

and-expression          =  and-expression and-operator and-expression
and-expression          =/ lparen expression rparen
and-expression          =/ access-token

or-expression           =  or-expression or-operator or-expression
or-expression           =/ lparen expression rparen
or-expression           =/ access-token

access-token            = 1*( ALPHA / DIGIT / "_" / "-" / "." / ":" / slash )
access-token            =/ DQUOTE 1*(utf8-subset / escaped) DQUOTE

utf8-subset             = %x20-21 / %x23-5B / %x5D-7E / unicode-beyond-ascii ; utf8 minus '"' and '\'
unicode-beyond-ascii    = %x0080-D7FF / %xE000-10FFFF
escaped                 = "\" DQUOTE / "\\"
slash                   = "/"
or-operator             = "|"
and-operator            = "&"
lparen                  = "("
rparen                  = ")"
```

## Serialization

An AccessExpression is a UTF-8 string. It can be serialized using a byte array as long as it
can be deserialized back into the same UTF-8 string.

[1]: https://www.rfc-editor.org/rfc/rfc5234
