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

import java.util.function.Predicate;

/**
 * Code for parsing and evaluating an access expression at the same time.
 */
final class ParserEvaluator {

  public static final byte OPEN_PAREN = (byte) '(';
  public static final byte CLOSE_PAREN = (byte) ')';
  private static final byte[] EMPTY = new byte[0];
  private static final ThreadLocal<BytesWrapper> lookupWrappers =
      ThreadLocal.withInitial(() -> new BytesWrapper(EMPTY));
  private static final ThreadLocal<Tokenizer> tokenizers =
      ThreadLocal.withInitial(() -> new Tokenizer(EMPTY));

  public static boolean parseAccessExpression(byte[] expression,
      Predicate<BytesWrapper> authorizedPredicate) {

    Tokenizer tokenizer = tokenizers.get();
    tokenizer.reset(expression);

    if (!tokenizer.hasNext()) {
      return true;
    }

    BytesWrapper lookupWrapper = lookupWrappers.get();
    var node = parseExpression(tokenizer, authorizedPredicate, lookupWrapper);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + (char) tokenizer.peek() + "'");
    }

    return node;
  }

  private static boolean parseExpression(Tokenizer tokenizer,
      Predicate<BytesWrapper> authorizedPredicate, BytesWrapper lookupWrapper) {

    boolean result =
        parseParenExpressionOrAuthorization(tokenizer, authorizedPredicate, lookupWrapper);

    if (tokenizer.hasNext()) {
      var operator = tokenizer.peek();
      if (operator == '&') {
        result = parseAndExpression(result, tokenizer, authorizedPredicate, lookupWrapper);
        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }
      } else if (operator == '|') {
        result = parseOrExpression(result, tokenizer, authorizedPredicate, lookupWrapper);
        if (tokenizer.hasNext() && isAndOrOperator(tokenizer.peek())) {
          // A case of mixed operators, lets give a clear error message
          tokenizer.error("Cannot mix '|' and '&'");
        }
      }
    }

    return result;
  }

  private static boolean parseAndExpression(boolean result, Tokenizer tokenizer,
      Predicate<BytesWrapper> authorizedPredicate, BytesWrapper lookupWrapper) {
    do {
      tokenizer.advance();
      var nextResult =
          parseParenExpressionOrAuthorization(tokenizer, authorizedPredicate, lookupWrapper);
      result &= nextResult;
    } while (tokenizer.hasNext() && tokenizer.peek() == '&');
    return result;
  }

  private static boolean parseOrExpression(boolean result, Tokenizer tokenizer,
      Predicate<BytesWrapper> authorizedPredicate, BytesWrapper lookupWrapper) {
    do {
      tokenizer.advance();
      var nextResult =
          parseParenExpressionOrAuthorization(tokenizer, authorizedPredicate, lookupWrapper);
      result |= nextResult;
    } while (tokenizer.hasNext() && tokenizer.peek() == '|');
    return result;
  }

  private static boolean parseParenExpressionOrAuthorization(Tokenizer tokenizer,
      Predicate<BytesWrapper> authorizedPredicate, BytesWrapper lookupWrapper) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == OPEN_PAREN) {
      tokenizer.advance();
      var node = parseExpression(tokenizer, authorizedPredicate, lookupWrapper);
      tokenizer.next(CLOSE_PAREN);
      return node;
    } else {
      var auth = tokenizer.nextAuthorization();
      lookupWrapper.set(auth.data, auth.start, auth.len);
      return authorizedPredicate.test(lookupWrapper);
    }
  }
}
