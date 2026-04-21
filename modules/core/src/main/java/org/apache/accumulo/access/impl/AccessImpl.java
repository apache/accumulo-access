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

import static org.apache.accumulo.access.AuthorizationValidator.AuthorizationCharacters.ANY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.apache.accumulo.access.ParsedAccessExpression;

public class AccessImpl implements Access {

  private final AuthorizationValidator authValidator;

  private void validateAuthArgument(CharSequence auth) {
    if (auth.length() == 0) {
      throw InvalidAuthorizationException.emptyString();
    }
    if (!authValidator.test(auth, ANY)) {
      throw InvalidAuthorizationException.invalidChars(auth);
    }
  }

  public AccessImpl(AuthorizationValidator authValidator) {
    this.authValidator = Objects.requireNonNull(authValidator);
  }

  @Override
  public AccessExpression newExpression(String expression) {
    if (expression.isEmpty()) {
      return AccessExpressionImpl.EMPTY;
    }
    validateExpression(expression);
    return new AccessExpressionImpl(expression);
  }

  @Override
  public ParsedAccessExpression newParsedExpression(String expression) {
    return ParsedAccessExpressionImpl.parseExpression(expression, authValidator);
  }

  @Override
  public void findAuthorizations(String expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException {
    ParserEvaluator.findAuthorizations(expression, authorizationConsumer, authValidator);
  }

  @Override
  public String quote(String authorization) {
    validateAuthArgument(authorization);
    boolean needsQuote = false;
    final int len = authorization.length();
    for (int i = 0; i < len; i++) {
      if (!Tokenizer.isValidAuthChar(authorization.charAt(i))) {
        needsQuote = true;
        break;
      }
    }
    return needsQuote ? CharUtils.escape(authorization, true) : authorization;
  }

  @Override
  public String unquote(String authorization) {
    String unquoted = authorization;
    final int len = unquoted.length();
    if (len >= 1) {
      final boolean firstIsQuote = unquoted.charAt(0) == '"';
      final boolean lastIsQuote = unquoted.charAt(len - 1) == '"';
      if (firstIsQuote || lastIsQuote) {
        if (len == 1 || (firstIsQuote != lastIsQuote)) {
          throw InvalidAuthorizationException.unablancedQuotes(authorization); // unbalanced quotes
        }
        unquoted = len == 2 ? "" : CharUtils.unescape(unquoted.substring(1, len - 1)).toString();
      }
    }
    validateAuthArgument(unquoted);
    return unquoted;
  }

  @Override
  public void validateExpression(String expression) throws InvalidAccessExpressionException {
    ParserEvaluator.validate(expression, authValidator);
  }

  @Override
  public AccessEvaluator newEvaluator(Set<String> authorizations) {
    return new AccessEvaluatorImpl(authorizations, this::validateAuthArgument, authValidator);
  }

  @Override
  public AccessEvaluator newEvaluator(Predicate<String> authorizer) {
    return new AccessEvaluatorImpl(authorizer, authValidator);
  }

  @Override
  public AccessEvaluator newEvaluator(Collection<Set<String>> authorizationSets) {
    var evaluators = new ArrayList<AccessEvaluator>(authorizationSets.size());
    authorizationSets.forEach(set -> evaluators.add(newEvaluator(set)));
    return new MultiAccessEvaluatorImpl(evaluators);
  }
}
