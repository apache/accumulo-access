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
import static org.apache.accumulo.access.ByteUtils.BACKSLASH;
import static org.apache.accumulo.access.ByteUtils.QUOTE;
import static org.apache.accumulo.access.ByteUtils.isQuoteOrSlash;
import static org.apache.accumulo.access.ByteUtils.isQuoteSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

//this class is intentionally package private and should never be made public
final class AccessEvaluatorImpl implements AccessEvaluator {
  private final Collection<Predicate<BytesWrapper>> authorizedPredicates;

  private static final byte[] EMPTY = new byte[0];

  private final ThreadLocal<BytesWrapper> lookupWrappers =
      ThreadLocal.withInitial(() -> new BytesWrapper(EMPTY));
  private final ThreadLocal<Tokenizer> tokenizers =
      ThreadLocal.withInitial(() -> new Tokenizer(EMPTY));

  static Collection<List<byte[]>> convert(Collection<Authorizations> authorizationSets) {
    final List<List<byte[]>> authorizationLists = new ArrayList<>(authorizationSets.size());
    for (final Authorizations authorizations : authorizationSets) {
      final Set<String> authSet = authorizations.asSet();
      final List<byte[]> authList = new ArrayList<>(authSet.size());
      for (final String auth : authSet) {
        authList.add(auth.getBytes(UTF_8));
      }
      authorizationLists.add(authList);
    }
    return authorizationLists;
  }

  static Collection<List<byte[]>> convert(String... authorizations) {
    final List<byte[]> authList = new ArrayList<>(authorizations.length);
    for (final String auth : authorizations) {
      authList.add(auth.getBytes(UTF_8));
    }
    return Collections.singletonList(authList);
  }

  static Collection<List<byte[]>> convert(Authorizations authorizations) {
    final Set<String> authSet = authorizations.asSet();
    final List<byte[]> authList = new ArrayList<>(authSet.size());
    for (final String auth : authSet) {
      authList.add(auth.getBytes(UTF_8));
    }
    return Collections.singletonList(authList);
  }

  /**
   * Create an AccessEvaluatorImpl using an Authorizer object
   */
  AccessEvaluatorImpl(Authorizer authorizationChecker) {
    this.authorizedPredicates = List.of(auth -> authorizationChecker.isAuthorized(unescape(auth)));
  }

  /**
   * Create an AccessEvaluatorImpl using a collection of authorizations
   */
  AccessEvaluatorImpl(final Collection<List<byte[]>> authorizationSets) {

    for (final List<byte[]> auths : authorizationSets) {
      if (auths == null) {
        continue;
      }
      for (final byte[] auth : auths) {
        if (auth.length == 0) {
          throw new IllegalArgumentException("Empty authorization");
        }
      }
    }

    final List<Predicate<BytesWrapper>> predicates = new ArrayList<>(authorizationSets.size());
    for (final List<byte[]> authorizationList : authorizationSets) {
      if (authorizationList == null) {
        continue;
      }
      final Set<BytesWrapper> authBytes = new HashSet<>(authorizationList.size());
      for (final byte[] authorization : authorizationList) {
        authBytes.add(new BytesWrapper(AccessEvaluatorImpl.escape(authorization, false)));
      }
      predicates.add((auth) -> authBytes.contains(auth));
    }
    authorizedPredicates = List.copyOf(predicates);
  }

  static String unescape(BytesWrapper auth) {
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
  static byte[] escape(byte[] auth, boolean shouldQuote) {
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
    return canAccess(expression.getExpression());
  }

  @Override
  public boolean canAccess(String expression) throws IllegalAccessExpressionException {
    return evaluate(expression.getBytes(UTF_8));
  }

  @Override
  public boolean canAccess(byte[] expression) throws IllegalAccessExpressionException {
    return evaluate(expression);
  }

  boolean evaluate(byte[] accessExpression) throws IllegalAccessExpressionException {
    var bytesWrapper = lookupWrappers.get();

    for (var auths : authorizedPredicates) {
      var tokenizer = tokenizers.get();
      tokenizer.reset(accessExpression);
      Predicate<Tokenizer.AuthorizationToken> atp = authToken -> {
        bytesWrapper.set(authToken.data, authToken.start, authToken.len);
        return auths.test(bytesWrapper);
      };
      if (!ParserEvaluator.parseAccessExpression(tokenizer, atp, authToken -> true)) {
        return false;
      }
    }
    return true;
  }

}
