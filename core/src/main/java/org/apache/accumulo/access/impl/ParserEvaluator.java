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

import static org.apache.accumulo.access.impl.ByteUtils.isAndOrOperator;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.accumulo.access.InvalidAccessExpressionException;

/**
 * Code for parsing and evaluating an access expression at the same time.
 */
public final class ParserEvaluator {

  static final byte OPEN_PAREN = (byte) '(';
  static final byte CLOSE_PAREN = (byte) ')';

  private static final byte[] EMPTY = new byte[0];

  static final ThreadLocal<BytesWrapper> lookupWrappers =
      ThreadLocal.withInitial(() -> new BytesWrapper(EMPTY));
  private static final ThreadLocal<Tokenizer> tokenizers =
      ThreadLocal.withInitial(() -> new Tokenizer(EMPTY));

  public static void findAuthorizations(byte[] expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException {
    var bytesWrapper = ParserEvaluator.lookupWrappers.get();
    Predicate<Tokenizer.AuthorizationToken> atp = authToken -> {
      bytesWrapper.set(authToken.data, authToken.start, authToken.len);
      authorizationConsumer.accept(AccessEvaluatorImpl.unescape(bytesWrapper));
      return true;
    };
    ParserEvaluator.parseAccessExpression(expression, atp, atp);
  }

  public static boolean parseAccessExpression(byte[] expression,
      Predicate<Tokenizer.AuthorizationToken> authorizedPredicate,
      Predicate<Tokenizer.AuthorizationToken> shortCircuitPredicate) {
    var tokenizer = tokenizers.get();
    tokenizer.reset(expression);
    return parseAccessExpression(tokenizer, authorizedPredicate, shortCircuitPredicate);
  }

  private static boolean parseAccessExpression(Tokenizer tokenizer,
      Predicate<Tokenizer.AuthorizationToken> authorizedPredicate,
      Predicate<Tokenizer.AuthorizationToken> shortCircuitPredicate) {

    if (!tokenizer.hasNext()) {
      return true;
    }

    var node = parseExpression(tokenizer, authorizedPredicate, shortCircuitPredicate);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + (char) tokenizer.peek() + "'");
    }

    return node;
  }

  private static boolean parseExpression(Tokenizer tokenizer,
      Predicate<Tokenizer.AuthorizationToken> authorizedPredicate,
      Predicate<Tokenizer.AuthorizationToken> shortCircuitPredicate) {

    boolean result =
        parseParenExpressionOrAuthorization(tokenizer, authorizedPredicate, shortCircuitPredicate);

    if (tokenizer.hasNext()) {
      var operator = tokenizer.peek();
      if (operator == ByteUtils.AND_OPERATOR) {
        result = parseAndExpression(result, tokenizer, authorizedPredicate, shortCircuitPredicate);
        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }
      } else if (operator == ByteUtils.OR_OPERATOR) {
        result = parseOrExpression(result, tokenizer, authorizedPredicate, shortCircuitPredicate);
        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }
      }
    }

    return result;
  }

  private static boolean parseAndExpression(boolean result, Tokenizer tokenizer,
      Predicate<Tokenizer.AuthorizationToken> authorizedPredicate,
      Predicate<Tokenizer.AuthorizationToken> shortCircuitPredicate) {
    do {
      if (!result) {
        // Once the "and" expression is false, can avoid doing set lookups and only validate the
        // rest of the expression.
        authorizedPredicate = shortCircuitPredicate;
      }
      tokenizer.advance();
      var nextResult = parseParenExpressionOrAuthorization(tokenizer, authorizedPredicate,
          shortCircuitPredicate);
      result &= nextResult;
    } while (tokenizer.hasNext() && tokenizer.peek() == ByteUtils.AND_OPERATOR);
    return result;
  }

  private static boolean parseOrExpression(boolean result, Tokenizer tokenizer,
      Predicate<Tokenizer.AuthorizationToken> authorizedPredicate,
      Predicate<Tokenizer.AuthorizationToken> shortCircuitPredicate) {
    do {
      if (result) {
        // Once the "or" expression is true, can avoid doing set lookups and only validate the rest
        // of the expression.
        authorizedPredicate = shortCircuitPredicate;
      }
      tokenizer.advance();
      var nextResult = parseParenExpressionOrAuthorization(tokenizer, authorizedPredicate,
          shortCircuitPredicate);
      result |= nextResult;
    } while (tokenizer.hasNext() && tokenizer.peek() == ByteUtils.OR_OPERATOR);
    return result;
  }

  private static boolean parseParenExpressionOrAuthorization(Tokenizer tokenizer,
      Predicate<Tokenizer.AuthorizationToken> authorizedPredicate,
      Predicate<Tokenizer.AuthorizationToken> shortCircuitPredicate) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == OPEN_PAREN) {
      tokenizer.advance();
      var node = parseExpression(tokenizer, authorizedPredicate, shortCircuitPredicate);
      tokenizer.next(CLOSE_PAREN);
      return node;
    } else {
      var auth = tokenizer.nextAuthorization(false);
      return authorizedPredicate.test(auth);
    }
  }
}
