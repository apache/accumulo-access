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
import java.util.Set;
import java.util.function.Consumer;

import org.apache.accumulo.access.impl.BuilderImpl;

/**
 * The entry point into Accumulo Access to create access expressions, expression evaluators, and
 * authorization sets.
 *
 * @see #builder()
 * @since 1.0
 */
public interface Access {

  interface Builder {
    /**
     * Provide a validator to accumulo access to narrow the set of valid authorizations for your
     * specific use case. If one is not provided then {@link AuthorizationValidator#DEFAULT} will be
     * used.
     *
     * <p>
     * The provided validator is called very frequently within accumulo access and implementations
     * that are slow will slow down accumulo access.
     */
    Builder authorizationValidator(AuthorizationValidator validator);

    Access build();
  }

  /**
   * Used to create an instance of AccumuloAccess. For efficiency, the recommend way to use this is
   * to create a single instance and somehow make it available to an entire project for use. In
   * addition to being efficient this ensures the entire project is using the same configuration.
   */
  static Builder builder() {
    return new BuilderImpl();
  }

  /**
   * Validates an access expression and returns an immutable AccessExpression object. If passing
   * access expressions as arguments in code, consider using this type instead of a String. The
   * advantage of passing this type over a String is that its known to be a valid expression. Also,
   * this type is much more informative than a String type. Conceptually this method calls
   * {@link #validateExpression(String)} and if that passes creates an immutable object that wraps
   * the expression.
   *
   * @throws InvalidAccessExpressionException if the given expression is not valid
   * @throws InvalidAuthorizationException when the expression contains an authorization that is not
   *         valid
   * @throws NullPointerException when the argument is null
   */
  AccessExpression newExpression(String expression)
      throws InvalidAccessExpressionException, InvalidAuthorizationException;

  /**
   * Quickly validates that an access expression is properly formed.
   *
   * @param expression a potential access expression that
   * @throws InvalidAccessExpressionException if the given expression is not valid
   * @throws InvalidAuthorizationException if the expression contains an invalid authorization
   * @throws NullPointerException when the argument is null
   */
  void validateExpression(String expression)
      throws InvalidAccessExpressionException, InvalidAuthorizationException;

  /**
   * Validates an access expression and returns an immutable object with a parse tree. Creating the
   * parse tree is expensive relative to calling {@link #newExpression(String)} or
   * {@link #validateExpression(String)}, so only use this method when the parse tree is always
   * needed. If the code may only use the parse tree sometimes, then it may be best to call
   * {@link #newExpression(String)} to create the access expression and then call
   * {@link AccessExpression#parse()} when needed.
   *
   * @throws NullPointerException when the argument is null
   * @throws InvalidAuthorizationException when the expression contains an authorization that is not
   *         valid
   * @throws InvalidAccessExpressionException if the given expression is not valid
   */
  ParsedAccessExpression newParsedExpression(String expression)
      throws InvalidAccessExpressionException, InvalidAuthorizationException;

  /**
   * Creates an Authorizations object from the set of input authorization strings.
   *
   * @param authorizations set of authorization strings
   * @throws InvalidAuthorizationException when the expression contains an authorization that is not
   *         valid
   * @return Authorizations object
   */
  Authorizations newAuthorizations(Set<String> authorizations) throws InvalidAuthorizationException;

  /**
   * Validates an access expression and finds all authorizations in it passing them to the
   * authorizationConsumer. For example, for the expression {@code (A&B)|(A&C)|(A&D)}, this method
   * would pass {@code A,B,A,C,A,D} to the consumer one at a time. The function will conceptually
   * call {@link #unquote(String)} prior to passing an authorization to authorizationConsumer.
   *
   * <p>
   * What this method does could also be accomplished by creating a parse tree using
   * {@link Access#newParsedExpression(String)} and then recursively walking the parse tree. The
   * implementation of this method does not create a parse tree and is much faster. If a parse tree
   * is already available, then it would likely be faster to use it rather than call this method.
   * </p>
   *
   * @throws InvalidAccessExpressionException when the expression is not valid.
   * @throws InvalidAuthorizationException when the expression contains an authorization that is not
   *         valid
   * @throws NullPointerException when any argument is null
   */
  void findAuthorizations(String expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException, InvalidAuthorizationException;

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted (surrounded by quotation marks). Use this method to quote authorizations that occur in
   * an access expression. This method will only quote if it is needed.
   *
   * @throws NullPointerException when the argument is null
   */
  String quote(String authorization) throws InvalidAuthorizationException;

  /**
   * Reverses what {@link #quote(String)} does, so will unquote and unescape an authorization if
   * needed. If the authorization is not quoted then it is returned as-is.
   *
   * @throws NullPointerException when the argument is null
   */
  String unquote(String authorization) throws InvalidAuthorizationException;

  /**
   * Creates an AccessEvaluator from an Authorizations object
   *
   * @param authorizations auths to use in the AccessEvaluator
   * @return AccessEvaluator object
   */
  AccessEvaluator newEvaluator(Authorizations authorizations);

  /**
   * Creates an AccessEvaluator from an Authorizer
   *
   * @param authorizer authorizer to use in the AccessEvaluator
   * @return AccessEvaluator object
   */
  AccessEvaluator newEvaluator(AccessEvaluator.Authorizer authorizer);

  /**
   * Creates an AccessEvaluator from multiple sets of authorizations. Each expression will be
   * evaluated independently against each set of authorizations and will only be deemed accessible
   * if accessible for all. For example the following code would print false, true, and then false.
   *
   * <pre>
   *     {@code
   * AccumuloAccess accumuloAccess = ...;
   * Collection<Authorizations> authSets =
   *     List.of(Authorizations.of("A", "B"), Authorizations.of("C", "D"));
   * var evaluator = accumuloAccess.newEvaluator(authSets);
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
   */
  AccessEvaluator newEvaluator(Collection<Authorizations> authorizationSets);
}
