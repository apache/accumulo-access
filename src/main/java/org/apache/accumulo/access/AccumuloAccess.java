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

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.accumulo.access.impl.BuilderImpl;

// TODO javadoc
// TODO remove all of the static entry points and use this instead
public interface AccumuloAccess {

  // TODO maybe move to top level
  interface AuthorizationValidator extends Predicate<CharSequence> {}

  interface Builder {
    /**
     * TODO document that users should make this as specific as possible in order to avoid creating
     * unexpected expressions
     *
     * TODO document performance reasons for passing CharSequence (allows avoiding obj alloc)
     *
     */
    Builder authorizationValidator(AuthorizationValidator validator);

    AccumuloAccess build();
  }

  public static Builder builder() {
    // TODO avoid object allocation when creating default
    return new BuilderImpl();
  }

  AccessExpression newExpression(String expression);

  ParsedAccessExpression newParsedExpression(String expression);

  Authorizations newAuthorizations();

  // TODO this could throw an exception now
  Authorizations newAuthorizations(Set<String> authorizations);

  void findAuthorizations(String expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException;

  String quote(String authorization);

  String unquote(String authorization);

  void validate(String expression) throws InvalidAccessExpressionException;

  AccessEvaluator newEvaluator(Authorizations authorizations);

  AccessEvaluator newEvaluator(AccessEvaluator.Authorizer authorizer);

  AccessEvaluator newEvaluator(Set<Authorizations> authorizationSets);
}
