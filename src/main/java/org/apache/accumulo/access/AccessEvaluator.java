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

/**
 * <p>
 * Used to decide if an entity with one more sets of authorizations can access zero or more access
 * expression.
 * <p>
 * <p>
 * Note: For performance improvements, especially in cases where expressions are expected to repeat,
 * it's recommended to wrap this evaluator with an external caching mechanism, such as Guava's
 * cache, to leverage its extensive caching options. Caching is only safe under the assumption that
 * for an AccessEvaluator instance, evaluating the same expression multiple times will always yield
 * the same result. When considering caching, any environmental factors that might change this
 * assumption may need to be mitigated.
 * </p>
 *
 * Below is an example that should print false and then print true.
 *
 * <pre>
 * {@code
 * var evaluator = AccessEvaluator.builder().authorizations("ALPHA", "OMEGA").build();
 *
 * System.out.println(evaluator.canAccess("ALPHA&BETA"));
 * System.out.println(evaluator.canAccess("(ALPHA|BETA)&(OMEGA|EPSILON)"));
 * }
 * </pre>
 *
 * @see <a href="https://github.com/apache/accumulo-access">Accumulo Access Documentation</a>
 * @since 1.0.0
 */
public interface AccessEvaluator {

  /**
   * @param accessExpression for this parameter a valid access expression is expected.
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   * @throws IllegalAccessExpressionException when the expression is not valid
   */
  boolean canAccess(String accessExpression) throws IllegalAccessExpressionException;

  /**
   * @param accessExpression for this parameter a valid access expression is expected.
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   * @throws IllegalAccessExpressionException when the expression is not valid
   */
  boolean canAccess(byte[] accessExpression) throws IllegalAccessExpressionException;

  /**
   * @param accessExpression a validated and parsed access expression. The implementation of this
   *        method may be able to reuse the internal parse tree and avoid re-parsing.
   * @return true if the expression is visible using the authorizations supplied at creation, false
   *         otherwise
   */
  boolean canAccess(AccessExpression accessExpression);

  /**
   * An interface that is used to check if an authorization seen in an access expression is
   * authorized.
   *
   * @since 1.0.0
   */
  interface Authorizer {
    boolean isAuthorized(String auth);
  }

  interface AuthorizationsBuilder {

    EvaluatorBuilder authorizations(Authorizations authorizations);

    /**
     * Allows providing multiple sets of authorizations. Each expression will be evaluated
     * independently against each set of authorizations and will only be deemed accessible if
     * accessible for all. For example the following code would print false, true, and then false.
     *
     * <pre>
     *     {@code
     * Collection<Authorizations> authSets =
     *     List.of(Authorizations.of("A", "B"), Authorizations.of("C", "D"));
     * var evaluator = AccessEvaluator.builder().authorizations(authSets).build();
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
     * authorization set. In order to return true for {@code canAccess()} the expression must
     * evaluate to true for each authorization set.
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
    EvaluatorBuilder authorizations(Collection<Authorizations> authorizations);

    /**
     * Allows specifying a single set of authorizations.
     */
    EvaluatorBuilder authorizations(String... authorizations);

    /**
     * Allows specifying an authorizer that is analogous to a single set of authorization.
     */
    EvaluatorBuilder authorizations(Authorizer authorizer);
  }

  interface EvaluatorBuilder {
    AccessEvaluator build();
  }

  static AuthorizationsBuilder builder() {
    return AccessEvaluatorImpl.builder();
  }
}
