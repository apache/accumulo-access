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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.accumulo.access.ByteUtils.AND_OPERATOR;
import static org.apache.accumulo.access.ByteUtils.OR_OPERATOR;
import static org.apache.accumulo.access.ByteUtils.isAndOrOperator;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AND;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AUTHORIZATION;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.OR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// This class is intentionally package private
final class ParsedAccessExpressionImpl extends ParsedAccessExpression {

  private static final long serialVersionUID = 1L;

  private final byte[] expression;
  private final int offset;
  private final int length;

  private final ExpressionType type;
  private final List<ParsedAccessExpression> children;

  private final AtomicReference<String> stringExpression = new AtomicReference<>(null);

  static final ParsedAccessExpression EMPTY = new ParsedAccessExpressionImpl();

  ParsedAccessExpressionImpl(byte operator, byte[] expression, int offset, int length,
      List<ParsedAccessExpression> children) {
    if (children.isEmpty()) {
      throw new IllegalArgumentException("Must have children with an operator");
    }

    if (operator != AND_OPERATOR && operator != OR_OPERATOR) {
      throw new IllegalArgumentException("Unknown operator " + operator);
    } else if (operator == AND_OPERATOR) {
      this.type = AND;
    } else {
      this.type = OR;
    }

    this.expression = expression;
    this.offset = offset;
    this.length = length;
    this.children = List.copyOf(children);
  }

  ParsedAccessExpressionImpl(byte[] expression, int offset, int length) {
    this.type = AUTHORIZATION;
    this.expression = expression;
    this.offset = offset;
    this.length = length;
    this.children = List.of();
  }

  ParsedAccessExpressionImpl() {
    this.type = ExpressionType.EMPTY;
    this.offset = 0;
    this.length = 0;
    this.expression = new byte[0];
    this.children = List.of();
  }

  @Override
  public String getExpression() {
    String strExp = stringExpression.get();
    if (strExp != null) {
      return strExp;
    }
    strExp = new String(expression, offset, length, UTF_8);
    stringExpression.compareAndSet(null, strExp);
    return stringExpression.get();
  }

  @Override
  public ExpressionType getType() {
    return type;
  }

  @Override
  public List<ParsedAccessExpression> getChildren() {
    return children;
  }

  static ParsedAccessExpression parseExpression(byte[] expression) {
    if (expression.length == 0) {
      return ParsedAccessExpressionImpl.EMPTY;
    }

    Tokenizer tokenizer = new Tokenizer(expression);
    var parsed = ParsedAccessExpressionImpl.parseExpression(tokenizer, false);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + (char) tokenizer.peek() + "'");
    }

    return parsed;
  }

  private static ParsedAccessExpressionImpl parseExpression(Tokenizer tokenizer,
      boolean wrappedWithParens) {

    int beginOffset = tokenizer.curentOffset();
    ParsedAccessExpressionImpl node = parseParenExpressionOrAuthorization(tokenizer);

    if (tokenizer.hasNext()) {
      var operator = tokenizer.peek();
      if (isAndOrOperator(operator)) {
        List<ParsedAccessExpression> nodes = new ArrayList<>();
        nodes.add(node);
        do {
          tokenizer.advance();
          ParsedAccessExpression next = parseParenExpressionOrAuthorization(tokenizer);
          nodes.add(next);
        } while (tokenizer.hasNext() && tokenizer.peek() == operator);

        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }

        int endOffset = tokenizer.curentOffset();

        node = new ParsedAccessExpressionImpl(operator, tokenizer.expression(), beginOffset,
            endOffset - beginOffset, nodes);
      }
    }

    return node;
  }

  private static ParsedAccessExpressionImpl
      parseParenExpressionOrAuthorization(Tokenizer tokenizer) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == ParserEvaluator.OPEN_PAREN) {
      tokenizer.advance();
      var node = parseExpression(tokenizer, true);
      tokenizer.next(ParserEvaluator.CLOSE_PAREN);
      return node;
    } else {
      var auth = tokenizer.nextAuthorization(true);
      return new ParsedAccessExpressionImpl(auth.data, auth.start, auth.len);
    }
  }
}
