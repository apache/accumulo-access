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
 * var evaluator = Authorizations.of(Set.of("ALPHA", "OMEGA")).evaluator();
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

}
