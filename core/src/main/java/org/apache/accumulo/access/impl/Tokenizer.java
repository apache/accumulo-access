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

import static org.apache.accumulo.access.impl.CharUtils.isBackslashSymbol;
import static org.apache.accumulo.access.impl.CharUtils.isQuoteOrSlash;
import static org.apache.accumulo.access.impl.CharUtils.isQuoteSymbol;

import java.util.stream.IntStream;

import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.InvalidAccessExpressionException;

/**
 * A simple wrapper around a byte array that keeps some state and provides high level operations to
 * the {@link ParserEvaluator} class. The purpose of this class is to make {@link ParserEvaluator}
 * as simple and easy to understand as possible while still being performant.
 */
public final class Tokenizer {

  private static final boolean[] validAuthChars = new boolean[256];

  static {
    IntStream.range(0, 256).forEach(i -> validAuthChars[i] = false);

    IntStream numbers = IntStream.rangeClosed('0', '9');
    IntStream letters =
        IntStream.concat(IntStream.rangeClosed('A', 'Z'), IntStream.rangeClosed('a', 'z'));
    IntStream.concat(numbers, letters).forEach(i -> validAuthChars[i] = true);

    "_-:./".chars().forEach(c -> validAuthChars[c] = true);
  }

  public static boolean isValidAuthChar(char b) {
    return validAuthChars[0xff & b] && b < 256;
  }

  private char[] expression;
  private int len;
  private int index;

  private final AuthorizationToken authorizationToken = new AuthorizationToken();

  public static class AuthorizationToken {
    public char[] data;
    public int start;
    public int len;
    public boolean hasEscapes;
    public AuthorizationValidator.AuthorizationCharacters quoting;

  }

  Tokenizer(char[] expression) {
    reset(expression, expression.length);
  }

  void reset(char[] expression, int len) {
    this.expression = expression;
    this.index = 0;
    this.len = len;
    this.authorizationToken.data = expression;
  }

  boolean hasNext() {
    return index < len;
  }

  public void advance() {
    index++;
  }

  public void next(char expected) {
    if (!hasNext()) {
      error("Expected '" + expected + "' instead saw end of input");
    }

    if (expression[index] != expected) {
      error("Expected '" + expected + "' instead saw '" + (expression[index]) + "'");
    }
    index++;
  }

  public void error(String msg) {
    error(msg, index);
  }

  public void error(String msg, int idx) {
    throw new InvalidAccessExpressionException(msg, new String(expression, 0, len), idx);
  }

  char peek() {
    return expression[index];
  }

  public int curentOffset() {
    return index;
  }

  AuthorizationToken nextAuthorization(boolean includeQuotes) {
    if (isQuoteSymbol(expression[index])) {
      int start = ++index;

      boolean hasEscapes = false;
      while (index < len && !isQuoteSymbol(expression[index])) {
        if (isBackslashSymbol(expression[index])) {
          index++;
          if (index == len || !isQuoteOrSlash(expression[index])) {
            error("Invalid escaping within quotes", index - 1);
          }
          hasEscapes = true;
        }
        index++;
      }

      if (index == len) {
        error("Unclosed quote", start - 1);
      }

      if (start == index) {
        error("Empty authorization token in quotes", start - 1);
      }

      authorizationToken.start = start;
      authorizationToken.len = index - start;
      authorizationToken.hasEscapes = hasEscapes;
      authorizationToken.quoting = AuthorizationValidator.AuthorizationCharacters.ANY;

      if (includeQuotes) {
        authorizationToken.start--;
        authorizationToken.len += 2;
      }

      index++;

      return authorizationToken;

    } else if (isValidAuthChar(expression[index])) {
      int start = index;
      do {
        index++;
      } while (index < len && isValidAuthChar(expression[index]));
      authorizationToken.start = start;
      authorizationToken.len = index - start;
      authorizationToken.hasEscapes = false;
      authorizationToken.quoting = AuthorizationValidator.AuthorizationCharacters.BASIC;
      return authorizationToken;
    } else {
      error("Expected a '(' character or an authorization token instead saw '" + peek() + "'");
      return null;
    }
  }

}
