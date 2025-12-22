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

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.apache.accumulo.access.impl.AccessEvaluatorImpl;
import org.apache.accumulo.access.impl.AuthorizationsImpl;
import org.apache.accumulo.access.impl.MultiAccessEvaluatorImpl;

public sealed interface Authorizations extends Iterable<String>, Serializable
    permits AuthorizationsImpl {

  /**
   * An interface that is used to check if an authorization seen in an access expression is
   * authorized.
   *
   * @since 1.0.0
   */
  interface Authorizer {
    boolean isAuthorized(String auth);
  }

  /**
   * @return a pre-allocated empty Authorizations object
   */
  public static Authorizations of() {
    return AuthorizationsImpl.EMPTY;
  }

  /**
   * Creates an Authorizations object from the set of input authorization strings.
   *
   * @param authorizations set of authorization strings
   * @return Authorizations object
   */
  public static Authorizations of(Set<String> authorizations) {
    if (authorizations.isEmpty()) {
      return AuthorizationsImpl.EMPTY;
    } else {
      return new AuthorizationsImpl(authorizations);
    }
  }

  /**
   * Returns the set of authorization strings in this Authorization object
   *
   * @return immutable set of authorization strings
   */
  public Set<String> asSet();

  /**
   * Creates an AccessEvaluator from an Authorizations object
   *
   * @return AccessEvaluator object
   */
  AccessEvaluator evaluator();

  /**
   * Creates an AccessEvaluator from an Authorizer object
   *
   * @param authorizer authorizer to use in the AccessEvaluator
   * @return AccessEvaluator object
   */
  static AccessEvaluator using(Authorizer authorizer) {
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
  static AccessEvaluator evaluator(Collection<Authorizations> authorizationSets) {
    return new MultiAccessEvaluatorImpl(authorizationSets);
  }

}
