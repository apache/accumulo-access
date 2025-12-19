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

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.AccumuloAccess;
import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.apache.accumulo.access.ParsedAccessExpression;

public class AccumuloAccessImpl implements AccumuloAccess {

  private final AuthorizationValidator authValidator;

  private void validateAuthorization(CharSequence auth) {
    if (!authValidator.test(auth)) {
      throw new InvalidAuthorizationException(auth.toString());
    }
  }

  public AccumuloAccessImpl(AuthorizationValidator authValidator) {
    this.authValidator = authValidator;
  }

  @Override
  public AccessExpression newExpression(String expression) {
    // TODO push this down into the parsing code, this parses twice
    AccessExpression.findAuthorizations(expression, this::validateAuthorization);

    return AccessExpression.of(expression);
  }

  @Override
  public ParsedAccessExpression newParsedExpression(String expression) {
    // TODO push this down into the parsing code, this parses twice
    AccessExpression.findAuthorizations(expression, this::validateAuthorization);

    return AccessExpression.parse(expression);
  }

  @Override
  public Authorizations newAuthorizations() {
    return Authorizations.of();
  }

  @Override
  public Authorizations newAuthorizations(Set<String> authorizations) {
    authorizations.forEach(this::validateAuthorization);

    return Authorizations.of(authorizations);
  }

  @Override
  public void findAuthorizations(String expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException {
    // TODO push this down into the parsing code, this parses twice
    AccessExpression.findAuthorizations(expression, this::validateAuthorization);
    AccessExpression.findAuthorizations(expression, authorizationConsumer);
  }

  @Override
  public String quote(String authorization) {
    validateAuthorization(authorization);
    return AccessExpression.quote(authorization);
  }

  @Override
  public String unquote(String authorization) {
    var unquoted = AccessExpression.unquote(authorization);
    validateAuthorization(unquoted);
    return unquoted;
  }

  @Override
  public void validate(String expression) throws InvalidAccessExpressionException {
    // TODO push this down into the parsing code, this parses twice
    AccessExpression.findAuthorizations(expression, this::validateAuthorization);
    AccessExpression.validate(expression);
  }

  // TODO remove this class and push the authorization validation down into AccessEvaluatorImpl
  public final class ValidatingAccessEvaluator implements AccessEvaluator {

    private final AccessEvaluator evaluator;

    private ValidatingAccessEvaluator(AccessEvaluator evaluator) {
      this.evaluator = evaluator;
    }

    @Override
    public boolean canAccess(String accessExpression) throws InvalidAccessExpressionException {
      // TODO push this down into the parsing code, this parses twice
      AccessExpression.findAuthorizations(accessExpression,
          AccumuloAccessImpl.this::validateAuthorization);
      return evaluator.canAccess(accessExpression);
    }

    @Override
    public boolean canAccess(byte[] accessExpression) throws InvalidAccessExpressionException {
      // this method would eventually go away in the super type when byte methods are removed
      return evaluator.canAccess(accessExpression);
    }

    @Override
    public boolean canAccess(AccessExpression accessExpression) {
      // TODO push this down into the parsing code, this parses twice
      AccessExpression.findAuthorizations(accessExpression.getExpression(),
          AccumuloAccessImpl.this::validateAuthorization);
      return evaluator.canAccess(accessExpression);
    }
  }

  @Override
  public AccessEvaluator newEvaluator(Authorizations authorizations) {
    return new ValidatingAccessEvaluator(AccessEvaluator.of(authorizations));
  }

  @Override
  public AccessEvaluator newEvaluator(AccessEvaluator.Authorizer authorizer) {
    return new ValidatingAccessEvaluator(AccessEvaluator.of(authorizer));
  }

  @Override
  public AccessEvaluator newEvaluator(Collection<Authorizations> authorizationSets) {
    return new ValidatingAccessEvaluator(AccessEvaluator.of(authorizationSets));
  }
}
