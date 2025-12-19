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

import java.util.Collection;

import org.apache.accumulo.access.impl.AccessEvaluatorImpl;
import org.apache.accumulo.access.impl.MultiAccessEvaluatorImpl;

/**
 * This class is used to decide if an entity with a given set of authorizations can access
 * subsequent access expressions.
 *
 * <p>
 * For example:
 *
 * <pre>
 * {@code
 * var evaluator = AccessEvaluator.of("ALPHA", "OMEGA");
 *
 * System.out.println(evaluator.canAccess("ALPHA&BETA")); // should print 'false'
 * System.out.println(evaluator.canAccess("(ALPHA|BETA)&(OMEGA|EPSILON)")); // should print 'true'
 * }
 * </pre>
 *
 * <p>
 * Note: for performance, especially in cases where expressions are expected to repeat, it's
 * recommended to cache the results of these evaluations. Caching is only safe under the assumption
 * that for an AccessEvaluator instance, evaluating the same expression multiple times will always
 * yield the same result. When considering caching, any environmental factors that might change this
 * assumption may need to be mitigated.
 *
 * <p>
 * Instances of this class are thread-safe.
 *
 * <p>
 * Note: The underlying implementation uses UTF-8 when converting between bytes and Strings.
 *
 * @see <a href="https://github.com/apache/accumulo-access">Accumulo Access Documentation</a>
 * @since 1.0.0
 */
public sealed interface AccessEvaluator permits AccessEvaluatorImpl, MultiAccessEvaluatorImpl {

  /**
   * @param accessExpression for this parameter a valid access expression is expected.
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   * @throws InvalidAccessExpressionException when the expression is not valid
   */
  boolean canAccess(String accessExpression) throws InvalidAccessExpressionException;

  /**
   * @param accessExpression for this parameter a valid access expression is expected.
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   * @throws InvalidAccessExpressionException when the expression is not valid
   */
  boolean canAccess(byte[] accessExpression) throws InvalidAccessExpressionException;

  /**
   * @param accessExpression previously validated access expression
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   */
  boolean canAccess(AccessExpression accessExpression);

  /**
   * @param parsedAccessExpression object resulting from call to AccessExpression.parse. This method
   *        would be useful if passing a single AccessExpression to multiple AccessEvaluators.
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   */
  boolean canAccess(ParsedAccessExpression parsedAccessExpression);

  /**
   * Creates an AccessEvaluator from an Authorizations object
   *
   * @param authorizations auths to use in the AccessEvaluator
   * @return AccessEvaluator object
   */
  static AccessEvaluator of(Authorizations authorizations) {
    return new AccessEvaluatorImpl(authorizations);
  }

  /**
   * Creates an AccessEvaluator from an Authorizer object
   *
   * @param authorizer authorizer to use in the AccessEvaluator
   * @return AccessEvaluator object
   */
  static AccessEvaluator of(Authorizer authorizer) {
    return new AccessEvaluatorImpl(authorizer);
  }

  /**
   * Allows providing multiple sets of authorizations. Each expression will be evaluated
   * independently against each set of authorizations and will only be deemed accessible if
   * accessible for all. For example the following code would print false, true, and then false.
   *
   * <pre>
   *     {@code
   * Collection<Authorizations> authSets =
   *     List.of(Authorizations.of("A", "B"), Authorizations.of("C", "D"));
   * var evaluator = AccessEvaluator.of(authSets);
   *
   * System.out.println(evaluator.canAccess("A"));
   * System.out.println(evaluator.canAccess("A|D"));
   * System.out.println(evaluator.canAccess("A&D"));
   *
   * }
   * </pre>
   *
   * <p>
   * The following table shows how each expression in the example above will evaluate for each
   * authorization set. In order to return true for {@code canAccess()} the expression must evaluate
   * to true for each authorization set.
   *
   * <table>
   * <caption>Evaluations</caption>
   * <tr>
   * <td></td>
   * <td>[A,B]</td>
   * <td>[C,D]</td>
   * </tr>
   * <tr>
   * <td>A</td>
   * <td>True</td>
   * <td>False</td>
   * </tr>
   * <tr>
   * <td>A|D</td>
   * <td>True</td>
   * <td>True</td>
   * </tr>
   * <tr>
   * <td>A&amp;D</td>
   * <td>False</td>
   * <td>False</td>
   * </tr>
   *
   * </table>
   *
   *
   *
   */
  static AccessEvaluator of(Collection<Authorizations> authorizationSets) {
    return MultiAccessEvaluatorImpl.of(authorizationSets);
  }

  /**
   * An interface that is used to check if an authorization seen in an access expression is
   * authorized.
   *
   * @since 1.0.0
   */
  interface Authorizer {
    boolean isAuthorized(String auth);
  }
}
