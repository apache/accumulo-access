/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.access;

import static org.apache.accumulo.access.ByteUtils.AND_OPERATOR;
import static org.apache.accumulo.access.ByteUtils.OR_OPERATOR;

import java.util.ArrayList;

/**
 * Code for parsing an access expression and creating a parse tree of type {@link AeNode}
 */
final class Parser {

  public static final byte OPEN_PAREN = (byte) '(';
  public static final byte CLOSE_PAREN = (byte) ')';

  public static AeNode parseAccessExpression(byte[] expression) {

    Tokenizer tokenizer = new Tokenizer(expression);

    if (!tokenizer.hasNext()) {
      return AeNode.of();
    }

    var node = parseExpression(tokenizer);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + (char) tokenizer.peek() + "'");
    }

    return node;
  }

  private static AeNode parseExpression(Tokenizer tokenizer) {

    AeNode first = parseParenExpressionOrAuthorization(tokenizer);

    if (tokenizer.hasNext()
        && (tokenizer.peek() == AND_OPERATOR || tokenizer.peek() == OR_OPERATOR)) {
      var nodes = new ArrayList<AeNode>();
      nodes.add(first);

      var operator = tokenizer.peek();

      do {
        tokenizer.advance();

        nodes.add(parseParenExpressionOrAuthorization(tokenizer));

      } while (tokenizer.hasNext() && tokenizer.peek() == operator);

      if (tokenizer.hasNext()
          && (tokenizer.peek() == OR_OPERATOR || tokenizer.peek() == AND_OPERATOR)) {
        // A case of mixed operators, lets give a clear error message
        tokenizer.error("Cannot mix '|' and '&'");
      }

      return AeNode.of(operator, nodes);
    } else {
      return first;
    }
  }

  private static AeNode parseParenExpressionOrAuthorization(Tokenizer tokenizer) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == OPEN_PAREN) {
      tokenizer.advance();
      var node = parseExpression(tokenizer);
      tokenizer.next(CLOSE_PAREN);
      return node;
    } else {
      return AeNode.of(tokenizer.nextAuthorization());
    }
  }
}
