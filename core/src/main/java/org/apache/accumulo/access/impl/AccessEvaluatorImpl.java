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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.accumulo.access.impl.ByteUtils.BACKSLASH;
import static org.apache.accumulo.access.impl.ByteUtils.QUOTE;
import static org.apache.accumulo.access.impl.ByteUtils.isQuoteOrSlash;
import static org.apache.accumulo.access.impl.ByteUtils.isQuoteSymbol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.ParsedAccessExpression;

public final class AccessEvaluatorImpl implements AccessEvaluator {

  private final Predicate<BytesWrapper> authorizedPredicate;

  /**
   * Create an AccessEvaluatorImpl using an Authorizer object
   */
  public AccessEvaluatorImpl(Authorizer authorizationChecker) {
    this.authorizedPredicate = auth -> authorizationChecker.isAuthorized(unescape(auth));
  }

  /**
   * Create an AccessEvaluatorImpl using a collection of authorizations
   */
  public AccessEvaluatorImpl(Authorizations authorizations) {
    var authsSet = authorizations.asSet();
    final Set<BytesWrapper> authBytes = new HashSet<>(authsSet.size());
    for (String authorization : authsSet) {
      var auth = authorization.getBytes(UTF_8);
      if (auth.length == 0) {
        throw new IllegalArgumentException("Empty authorization");
      }
      authBytes.add(new BytesWrapper(AccessEvaluatorImpl.escape(auth, false)));
    }

    authorizedPredicate = authBytes::contains;
  }

  public static String unescape(BytesWrapper auth) {
    int escapeCharCount = 0;
    for (int i = 0; i < auth.length(); i++) {
      byte b = auth.byteAt(i);
      if (isQuoteOrSlash(b)) {
        escapeCharCount++;
      }
    }

    if (escapeCharCount > 0) {
      if (escapeCharCount % 2 == 1) {
        throw new IllegalArgumentException("Illegal escape sequence in auth : " + auth);
      }

      byte[] unescapedCopy = new byte[auth.length() - escapeCharCount / 2];
      int pos = 0;
      for (int i = 0; i < auth.length(); i++) {
        byte b = auth.byteAt(i);
        if (b == BACKSLASH) {
          i++;
          b = auth.byteAt(i);
          if (!isQuoteOrSlash(b)) {
            throw new IllegalArgumentException("Illegal escape sequence in auth : " + auth);
          }
        } else if (isQuoteSymbol(b)) {
          // should only see quote after a slash
          throw new IllegalArgumentException(
              "Illegal character after slash in auth String : " + auth);
        }

        unescapedCopy[pos++] = b;
      }

      return new String(unescapedCopy, UTF_8);
    } else {
      return auth.toString();
    }
  }

  /**
   * Properly escapes an authorization string. The string can be quoted if desired.
   *
   * @param auth authorization string, as UTF-8 encoded bytes
   * @param shouldQuote true to wrap escaped authorization in quotes
   * @return escaped authorization string
   */
  public static byte[] escape(byte[] auth, boolean shouldQuote) {
    int escapeCount = 0;

    for (byte value : auth) {
      if (isQuoteOrSlash(value)) {
        escapeCount++;
      }
    }

    if (escapeCount > 0 || shouldQuote) {
      byte[] escapedAuth = new byte[auth.length + escapeCount + (shouldQuote ? 2 : 0)];
      int index = shouldQuote ? 1 : 0;
      for (byte b : auth) {
        if (isQuoteOrSlash(b)) {
          escapedAuth[index++] = BACKSLASH;
        }
        escapedAuth[index++] = b;
      }

      if (shouldQuote) {
        escapedAuth[0] = QUOTE;
        escapedAuth[escapedAuth.length - 1] = QUOTE;
      }

      auth = escapedAuth;
    }
    return auth;
  }

  @Override
  public boolean canAccess(AccessExpression expression) {
    if (expression.isParsed()) {
      return canAccess(expression.parse());
    } else {
      return canAccess(expression.getExpression());
    }
  }

  @Override
  public boolean canAccess(String expression) throws InvalidAccessExpressionException {
    return evaluate(expression.getBytes(UTF_8));
  }

  @Override
  public boolean canAccess(byte[] expression) throws InvalidAccessExpressionException {
    return evaluate(expression);
  }

  boolean evaluate(byte[] accessExpression) throws InvalidAccessExpressionException {
    var bytesWrapper = ParserEvaluator.lookupWrappers.get();
    Predicate<Tokenizer.AuthorizationToken> atp = authToken -> {
      bytesWrapper.set(authToken.data, authToken.start, authToken.len);
      return authorizedPredicate.test(bytesWrapper);
    };
    return ParserEvaluator.parseAccessExpression(accessExpression, atp, authToken -> true);
  }

  private boolean canAccess(ParsedAccessExpression pae) {
    switch (pae.getType()) {
      case AND:
        return canAccessParsedAnd(pae.getChildren());
      case OR:
        return canAccessParsedOr(pae.getChildren());
      case AUTHORIZATION:
        return canAccessParsedAuthorization(pae.getExpression());
      case EMPTY:
        return true;
      default:
        throw new IllegalArgumentException("Unhandled type: " + pae.getType());
    }
  }

  private boolean canAccessParsedAnd(List<ParsedAccessExpression> children) {
    requireNonNull(children, "null children list passed to method");
    if (children.isEmpty() || children.size() < 2) {
      throw new IllegalArgumentException(
          "Malformed AND expression, children: " + children.size() + " -> " + children);
    }
    boolean result = true;
    for (ParsedAccessExpression child : children) {
      result &= canAccess(child);
      if (result == false) {
        return result;
      }
    }
    return result;
  }

  private boolean canAccessParsedOr(List<ParsedAccessExpression> children) {
    requireNonNull(children, "null children list passed to method");
    if (children.isEmpty()) {
      throw new IllegalArgumentException("Malformed OR expression, no children");
    }
    for (ParsedAccessExpression child : children) {
      if (canAccess(child)) {
        return true;
      }
    }
    return false;
  }

  private boolean canAccessParsedAuthorization(String token) {
    var bytesWrapper = ParserEvaluator.lookupWrappers.get();
    var authToken = token.getBytes(UTF_8);
    if (authToken[0] == '"' && authToken[authToken.length - 1] == '"') {
      bytesWrapper.set(authToken, 1, authToken.length - 2);
    } else {
      bytesWrapper.set(authToken, 0, authToken.length);
    }
    return authorizedPredicate.test(bytesWrapper);
  }

}
