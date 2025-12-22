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

import static org.apache.accumulo.access.impl.ByteUtils.BACKSLASH;
import static org.apache.accumulo.access.impl.ByteUtils.QUOTE;
import static org.apache.accumulo.access.impl.ByteUtils.isQuoteOrSlash;
import static org.apache.accumulo.access.impl.ByteUtils.isQuoteSymbol;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.InvalidAuthorizationException;

public final class AccessEvaluatorImpl implements AccessEvaluator {

  private final Predicate<CharSequence> authorizedPredicate;
  // TODO set
  private final AuthorizationValidator authorizationValidator;

  /**
   * Create an AccessEvaluatorImpl using an Authorizer object
   */
  public AccessEvaluatorImpl(Authorizer authorizationChecker,
      AuthorizationValidator authorizationValidator) {
    this.authorizedPredicate = auth -> authorizationChecker.isAuthorized(auth.toString());
    this.authorizationValidator = authorizationValidator;
  }

  /**
   * Create an AccessEvaluatorImpl using a collection of authorizations
   */
  public AccessEvaluatorImpl(Authorizations authorizations,
      AuthorizationValidator authorizationValidator) {
    var authsSet = authorizations.asSet();
    final Set<CharsWrapper> wrappedAuths = new HashSet<>(authsSet.size());
    for (String authorization : authsSet) {
      if (authorization.isEmpty()) {
        throw new IllegalArgumentException("Empty authorization");
      }

      wrappedAuths.add(new CharsWrapper(authorization));
    }

    this.authorizedPredicate = auth -> {
      if (auth instanceof CharsWrapper) {
        return wrappedAuths.contains(auth);
      } else {
        return wrappedAuths.contains(new CharsWrapper(auth));
      }
    };
    this.authorizationValidator = authorizationValidator;
  }

  public static CharSequence unescape(CharSequence auth) {
    int escapeCharCount = 0;
    for (int i = 0; i < auth.length(); i++) {
      char c = auth.charAt(i);
      if (isQuoteOrSlash(c)) {
        escapeCharCount++;
      }
    }

    if (escapeCharCount > 0) {
      if (escapeCharCount % 2 == 1) {
        throw new IllegalArgumentException("Illegal escape sequence in auth : " + auth);
      }

      char[] unescapedCopy = new char[auth.length() - escapeCharCount / 2];
      int pos = 0;
      for (int i = 0; i < auth.length(); i++) {
        char c = auth.charAt(i);
        if (c == BACKSLASH) {
          i++;
          c = auth.charAt(i);
          if (!isQuoteOrSlash(c)) {
            throw new IllegalArgumentException("Illegal escape sequence in auth : " + auth);
          }
        } else if (isQuoteSymbol(c)) {
          // should only see quote after a slash
          throw new IllegalArgumentException(
              "Illegal character after slash in auth String : " + auth);
        }

        unescapedCopy[pos++] = c;
      }

      return new String(unescapedCopy);
    } else {
      return auth;
    }
  }

  /**
   * Properly escapes an authorization string. The string can be quoted if desired.
   *
   * @param auth authorization string, as UTF-8 encoded bytes
   * @param shouldQuote true to wrap escaped authorization in quotes
   * @return escaped authorization string
   */
  public static CharSequence escape(CharSequence auth, boolean shouldQuote) {
    int escapeCount = 0;

    for (int i = 0; i < auth.length(); i++) {
      if (isQuoteOrSlash(auth.charAt(i))) {
        escapeCount++;
      }
    }

    if (escapeCount > 0 || shouldQuote) {
      char[] escapedAuth = new char[auth.length() + escapeCount + (shouldQuote ? 2 : 0)];
      int index = shouldQuote ? 1 : 0;
      for (int i = 0; i < auth.length(); i++) {
        char c = auth.charAt(i);
        if (isQuoteOrSlash(c)) {
          escapedAuth[index++] = BACKSLASH;
        }
        escapedAuth[index++] = c;
      }

      if (shouldQuote) {
        escapedAuth[0] = QUOTE;
        escapedAuth[escapedAuth.length - 1] = QUOTE;
      }

      auth = new String(escapedAuth);
    }
    return auth;
  }

  @Override
  public boolean canAccess(AccessExpression expression) {
    return canAccess(expression.getExpression());
  }

  @Override
  public boolean canAccess(String expression) throws InvalidAccessExpressionException {
    return evaluate(expression);
  }

  boolean evaluate(String accessExpression) throws InvalidAccessExpressionException {
    var charsWrapper = ParserEvaluator.lookupWrappers.get();
    Predicate<Tokenizer.AuthorizationToken> atp = authToken -> {
      var authorization = ParserEvaluator.unescape(authToken, charsWrapper);
      if (!authorizationValidator.test(authorization)) {
        throw new InvalidAuthorizationException(authorization.toString());
      }
      return authorizedPredicate.test(authorization);
    };

    // This is used once the expression is known to always be true or false. For this case only need
    // to validate authorizations, do not need to look them up in a set.
    Predicate<Tokenizer.AuthorizationToken> shortCircuit = authToken -> {
      var authorization = ParserEvaluator.unescape(authToken, charsWrapper);
      if (!authorizationValidator.test(authorization)) {
        throw new InvalidAuthorizationException(authorization.toString());
      }
      return true;
    };

    return ParserEvaluator.parseAccessExpression(accessExpression, atp, shortCircuit);
  }
}
