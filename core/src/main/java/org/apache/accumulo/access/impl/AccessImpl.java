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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.apache.accumulo.access.ParsedAccessExpression;

public class AccessImpl implements Access {

  private final AuthorizationValidator authValidator;

  private void validateAuthorization(CharSequence auth,
      AuthorizationValidator.AuthorizationCharacters quoting) {
    if (auth.isEmpty()) {
      throw new IllegalArgumentException("Empty string is not a valid authorization");
    }
    if (!authValidator.test(auth, quoting)) {
      throw new InvalidAuthorizationException(auth.toString());
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
  public Authorizations newAuthorizations(Set<String> authorizations) {
    if (authorizations.isEmpty()) {
      return AuthorizationsImpl.EMPTY;
    } else {
      authorizations.forEach(auth -> validateAuthorization(auth, ANY));
      return new AuthorizationsImpl(authorizations);
    }
  }

  @Override
  public void findAuthorizations(String expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException {
    ParserEvaluator.findAuthorizations(expression, authorizationConsumer, authValidator);
  }

  @Override
  public String quote(String authorization) {
    validateAuthorization(authorization, ANY);
    return AccessExpressionImpl.quote(authorization).toString();
  }

  @Override
  public String unquote(String authorization) {
    var unquoted = AccessExpressionImpl.unquote(authorization);
    validateAuthorization(unquoted, ANY);
    return unquoted.toString();
  }

  @Override
  public void validateExpression(String expression) throws InvalidAccessExpressionException {
    ParserEvaluator.validate(expression, authValidator);
  }

  @Override
  public AccessEvaluator newEvaluator(Authorizations authorizations) {
    return new AccessEvaluatorImpl(authorizations, authValidator);
  }

  @Override
  public AccessEvaluator newEvaluator(AccessEvaluator.Authorizer authorizer) {
    return new AccessEvaluatorImpl(authorizer, authValidator);
  }

  @Override
  public AccessEvaluator newEvaluator(Collection<Authorizations> authorizationSets) {
    return new MultiAccessEvaluatorImpl(authorizationSets, authValidator);
  }
}
