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
package org.apache.accumulo.access.impl;

import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AND;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AUTHORIZATION;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.OR;
import static org.apache.accumulo.access.impl.ByteUtils.AND_OPERATOR;
import static org.apache.accumulo.access.impl.ByteUtils.OR_OPERATOR;
import static org.apache.accumulo.access.impl.ByteUtils.isAndOrOperator;

import java.util.ArrayList;
import java.util.List;

import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.apache.accumulo.access.ParsedAccessExpression;

public final class ParsedAccessExpressionImpl extends ParsedAccessExpression {

  private static final long serialVersionUID = 1L;

  private final String expression;
  private final int offset;
  private final int length;

  private final ExpressionType type;
  private final List<ParsedAccessExpression> children;

  public static final ParsedAccessExpression EMPTY = new ParsedAccessExpressionImpl();

  private ParsedAccessExpressionImpl(char operator, String expression, int offset, int length,
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

  private ParsedAccessExpressionImpl(String expression) {
    this.type = AUTHORIZATION;
    this.expression = expression;
    this.offset = 0;
    this.length = expression.length();
    this.children = List.of();
  }

  ParsedAccessExpressionImpl() {
    this.type = ExpressionType.EMPTY;
    this.expression = "";
    this.offset = 0;
    this.length = 0;
    this.children = List.of();
  }

  @Override
  public String getExpression() {
    return expression.substring(offset, length + offset);
  }

  @Override
  public ParsedAccessExpression parse() {
    return this;
  }

  @Override
  public ExpressionType getType() {
    return type;
  }

  @Override
  public List<ParsedAccessExpression> getChildren() {
    return children;
  }

  public static ParsedAccessExpression parseExpression(String expression,
      AuthorizationValidator authorizationValidator) {
    if (expression.isEmpty()) {
      return ParsedAccessExpressionImpl.EMPTY;
    }

    Tokenizer tokenizer = ParserEvaluator.getPerThreadTokenizer(expression);
    var parsed =
        ParsedAccessExpressionImpl.parseExpression(tokenizer, expression, authorizationValidator);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + tokenizer.peek() + "'");
    }

    return parsed;
  }

  private static ParsedAccessExpressionImpl parseExpression(Tokenizer tokenizer,
      String wholeExpression, AuthorizationValidator authorizationValidator) {

    int beginOffset = tokenizer.curentOffset();
    ParsedAccessExpressionImpl node =
        parseParenExpressionOrAuthorization(tokenizer, wholeExpression, authorizationValidator);

    if (tokenizer.hasNext()) {
      var operator = tokenizer.peek();
      if (isAndOrOperator(operator)) {
        List<ParsedAccessExpression> nodes = new ArrayList<>();
        nodes.add(node);
        do {
          tokenizer.advance();
          ParsedAccessExpression next = parseParenExpressionOrAuthorization(tokenizer,
              wholeExpression, authorizationValidator);
          nodes.add(next);
        } while (tokenizer.hasNext() && tokenizer.peek() == operator);

        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }

        int endOffset = tokenizer.curentOffset();

        node = new ParsedAccessExpressionImpl(operator, wholeExpression, beginOffset,
            endOffset - beginOffset, nodes);
      }
    }

    return node;
  }

  private static ParsedAccessExpressionImpl parseParenExpressionOrAuthorization(Tokenizer tokenizer,
      String wholeExpression, AuthorizationValidator authorizationValidator) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == ParserEvaluator.OPEN_PAREN) {
      tokenizer.advance();
      var node = parseExpression(tokenizer, wholeExpression, authorizationValidator);
      tokenizer.next(ParserEvaluator.CLOSE_PAREN);
      return node;
    } else {
      var auth = tokenizer.nextAuthorization(true);
      CharSequence unquotedAuth;
      AuthorizationValidator.AuthorizationCharacters quoting;
      var wrapper = ParserEvaluator.lookupWrappers.get();
      wrapper.set(auth.data, auth.start, auth.len);
      if (ByteUtils.isQuoteSymbol(wrapper.charAt(0))) {
        unquotedAuth = AccessExpressionImpl.unquote(wrapper);
        quoting = AuthorizationValidator.AuthorizationCharacters.ANY;
      } else {
        unquotedAuth = wrapper;
        quoting = AuthorizationValidator.AuthorizationCharacters.BASIC;
      }
      if (!authorizationValidator.test(unquotedAuth, quoting)) {
        throw new InvalidAuthorizationException(unquotedAuth.toString());
      }
      return new ParsedAccessExpressionImpl(new String(auth.data, auth.start, auth.len));
    }
  }
}
