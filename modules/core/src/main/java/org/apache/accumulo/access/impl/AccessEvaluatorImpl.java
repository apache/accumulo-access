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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.InvalidAccessExpressionException;

public final class AccessEvaluatorImpl implements AccessEvaluator {

  private final Predicate<CharSequence> authorizedPredicate;
  private final AuthorizationValidator authorizationValidator;

  /**
   * Create an AccessEvaluatorImpl using an Authorizer object
   */
  AccessEvaluatorImpl(Predicate<String> authorizationChecker,
      AuthorizationValidator authorizationValidator) {
    this.authorizedPredicate = auth -> authorizationChecker.test(auth.toString());
    this.authorizationValidator = authorizationValidator;
  }

  /**
   * Create an AccessEvaluatorImpl using a collection of authorizations
   */
  AccessEvaluatorImpl(Set<String> authorizations, Consumer<String> authArgumentValidator,
      AuthorizationValidator authorizationValidator) {

    final Set<CharsWrapper> wrappedAuths = new HashSet<>(authorizations.size());
    authorizations.forEach(authArgumentValidator
        .andThen(auth -> wrappedAuths.add(new CharsWrapper(auth.toCharArray()))));

    this.authorizedPredicate =
        auth -> auth instanceof CharsWrapper ? wrappedAuths.contains((CharsWrapper) auth)
            : wrappedAuths.contains(new CharsWrapper(auth.toString().toCharArray()));
    this.authorizationValidator = authorizationValidator;
  }

  @Override
  public boolean canAccess(String expression) throws InvalidAccessExpressionException {
    return evaluate(expression);
  }

  boolean evaluate(String accessExpression) throws InvalidAccessExpressionException {
    var charsWrapper = ParserEvaluator.lookupWrappers.get();
    Predicate<Tokenizer.AuthorizationToken> atp = authToken -> authorizedPredicate
        .test(ParserEvaluator.validateAuth(authorizationValidator, authToken, charsWrapper));

    // This is used once the expression is known to always be true or false. For this case only need
    // to validate authorizations, do not need to look them up in a set.
    Predicate<Tokenizer.AuthorizationToken> shortCircuit = authToken -> {
      ParserEvaluator.validateAuth(authorizationValidator, authToken, charsWrapper);
      return true;
    };

    return ParserEvaluator.parseAccessExpression(accessExpression, atp, shortCircuit);
  }
}
