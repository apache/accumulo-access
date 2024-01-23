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

import static org.apache.accumulo.access.ByteUtils.isAndOrOperator;

import java.util.ArrayList;
import java.util.List;

class Normalizer {

  static String normalize(Tokenizer tokenizer) {

    if (!tokenizer.hasNext()) {
      return "";
    }

    var node = parseExpression(tokenizer);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + (char) tokenizer.peek() + "'");
    }

    StringBuilder builder = new StringBuilder();
    node.normalize().stringify(builder, false);
    return builder.toString();
  }

  private static AeNode parseExpression(Tokenizer tokenizer) {

    AeNode node = parseParenExpressionOrAuthorization(tokenizer);

    if (tokenizer.hasNext()) {
      var operator = tokenizer.peek();
      if (isAndOrOperator(operator)) {
        List<AeNode> nodes = new ArrayList<>();
        nodes.add(node);
        do {
          tokenizer.advance();
          AeNode next = parseParenExpressionOrAuthorization(tokenizer);
          nodes.add(next);
        } while (tokenizer.hasNext() && tokenizer.peek() == operator);

        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }

        node = AeNode.of(operator, nodes);
      }
    }

    return node;
  }

  private static AeNode parseParenExpressionOrAuthorization(Tokenizer tokenizer) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == ParserEvaluator.OPEN_PAREN) {
      tokenizer.advance();
      var node = parseExpression(tokenizer);
      tokenizer.next(ParserEvaluator.CLOSE_PAREN);
      return node;
    } else {
      var auth = tokenizer.nextAuthorization();
      return AeNode.of(auth);
    }
  }
}
