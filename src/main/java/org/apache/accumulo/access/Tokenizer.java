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
import static org.apache.accumulo.access.ByteUtils.isBackslashSymbol;
import static org.apache.accumulo.access.ByteUtils.isQuoteOrSlash;
import static org.apache.accumulo.access.ByteUtils.isQuoteSymbol;

import java.util.stream.IntStream;

/**
 * A simple wrapper around a byte array that keeps some state and provides high level operations to
 * the {@link ParserEvaluator} class. The purpose of this class is to make {@link ParserEvaluator}
 * as simple and easy to understand as possible while still being performant.
 */
final class Tokenizer {

  private static final boolean[] validAuthChars = new boolean[256];

  static {
    IntStream.range(0, 256).forEach(i -> validAuthChars[i] = false);

    IntStream numbers = IntStream.rangeClosed('0', '9');
    IntStream letters =
        IntStream.concat(IntStream.rangeClosed('A', 'Z'), IntStream.rangeClosed('a', 'z'));
    IntStream.concat(numbers, letters).forEach(i -> validAuthChars[i] = true);

    "_-:./".chars().forEach(c -> validAuthChars[c] = true);
  }

  static boolean isValidAuthChar(byte b) {
    return validAuthChars[0xff & b];
  }

  private byte[] expression;
  private int index;

  private final AuthorizationToken authorizationToken = new AuthorizationToken();

  static class AuthorizationToken {
    byte[] data;
    int start;
    int len;
  }

  Tokenizer(byte[] expression) {
    this.expression = expression;
    authorizationToken.data = expression;
  }

  public void reset(byte[] expression) {
    this.expression = expression;
    authorizationToken.data = expression;
    this.index = 0;
  }

  boolean hasNext() {
    return index < expression.length;
  }

  public void advance() {
    index++;
  }

  public void next(byte expected) {
    if (!hasNext()) {
      error("Expected '" + (char) expected + "' instead saw end of input");
    }

    if (expression[index] != expected) {
      error("Expected '" + (char) expected + "' instead saw '" + (char) (expression[index]) + "'");
    }
    index++;
  }

  public void error(String msg) {
    error(msg, index);
  }

  public void error(String msg, int idx) {
    throw new InvalidAccessExpressionException(msg, new String(expression, UTF_8), idx);
  }

  byte peek() {
    return expression[index];
  }

  public byte[] expression() {
    return expression;
  }

  public int curentOffset() {
    return index;
  }

  AuthorizationToken nextAuthorization(boolean includeQuotes) {
    if (isQuoteSymbol(expression[index])) {
      int start = ++index;

      while (index < expression.length && !isQuoteSymbol(expression[index])) {
        if (isBackslashSymbol(expression[index])) {
          index++;
          if (index == expression.length || !isQuoteOrSlash(expression[index])) {
            error("Invalid escaping within quotes", index - 1);
          }
        }
        index++;
      }

      if (index == expression.length) {
        error("Unclosed quote", start - 1);
      }

      if (start == index) {
        error("Empty authorization token in quotes", start - 1);
      }

      authorizationToken.start = start;
      authorizationToken.len = index - start;

      if (includeQuotes) {
        authorizationToken.start--;
        authorizationToken.len += 2;
      }

      index++;

      return authorizationToken;

    } else if (isValidAuthChar(expression[index])) {
      int start = index;
      while (index < expression.length && isValidAuthChar(expression[index])) {
        index++;
      }
      authorizationToken.start = start;
      authorizationToken.len = index - start;
      return authorizationToken;
    } else {
      error(
          "Expected a '(' character or an authorization token instead saw '" + (char) peek() + "'");
      return null;
    }
  }

}
