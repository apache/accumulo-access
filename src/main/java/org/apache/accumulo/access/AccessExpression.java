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

/**
 * This class offers the ability to validate, build, and normalize access expressions. An instance
 * of this class should wrap an immutable, validated access expression. If passing access
 * expressions as arguments in code, consider using this type instead of a String. The advantage of
 * passing this type over a String is that its known to be a valid expression.
 *
 * Normalization removes duplicates, sorts, flattens, and removes unneeded parentheses or quotes in
 * the expression. Normalization is an optional process that the user can choose to occur when
 * constructing an AccessExpression. The AccessEvaluator has the ability to short-circuit
 * evaluation, for example when the left hand side of an OR expression is valid, then it won't need
 * to evaluate the right side. The user may not want to perform normalization if they are
 * constructing their AccessExpressions to take advantage of this feature.
 *
 * <p>
 * Below is an example of how to use this API.
 *
 * <pre>
 * {@code
 * // The following authorization does not need quoting
 * // so the return value is the same as the input.
 * var auth1 = AccessExpression.quote("CAT");
 *
 * // The following two authorizations need quoting and the return values will be quoted.
 * var auth2 = AccessExpression.quote("🦕");
 * var auth3 = AccessExpression.quote("🦖");
 *
 * // Create an AccessExpression using auth1, auth2, and auth3
 * var exp = "(" + auth1 + "&" + auth3 + ")|(" + auth1 + "&" + auth2 + "&" + auth1 + ")";
 *
 * // Validate the expression, but do not normalize it
 * System.out.println(AccessExpression.of(exp).getExpression());
 *
 * // Validate and normalize the expression.
 * System.out.println(AccessExpression.of(exp, true).getExpression());
 *
 * // Print the unique authorization in the expression
 * System.out.println(visExp.getAuthorizations());
 * }
 * </pre>
 *
 * The above example will print the following.
 *
 * <pre>
 * (CAT&amp;"🦖")|(CAT&amp;"🦕"&amp;CAT)
 * ("🦕"&amp;CAT)|("🦖"&amp;CAT)
 * [🦖, CAT, 🦕]
 * </pre>
 *
 * The following code will throw an {@link InvalidAccessExpressionException} because the expression
 * is not valid.
 *
 * <pre>
 * {@code
 * AccessExpression.validate("A&B|C");
 * }
 * </pre>
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
public interface AccessExpression extends Serializable {

  /**
   * @return the expression that was used to create this object.
   */
  String getExpression();

  /**
   * @return the unique set of authorizations that occur in the expression. For example, for the
   *         expression {@code (A&B)|(A&C)|(A&D)}, this method would return {@code [A,B,C,D]}.
   */
  Authorizations getAuthorizations();

  /**
   * This is equivalent to calling {@code AccessExpression.of(expression, false);}
   */
  static AccessExpression of(String expression) throws InvalidAccessExpressionException {
    return new AccessExpressionImpl(expression, false);
  }

  /**
   * <p>
   * Validates an access expression and creates an immutable AccessExpression object.
   *
   * <p>
   * When the {@code normalize} parameter is true, then will deduplicate, sort, flatten, and remove
   * unneeded parentheses or quotes in the expressions. Normalization is done in addition to
   * validation. The following list gives examples of what each normalization step does.
   *
   * <ul>
   * <li>As an example of flattening, the expression {@code A&(B&C)} flattens to {@code A&B&C}.</li>
   * <li>As an example of sorting, the expression {@code (Z&Y)|(C&B)} sorts to
   * {@code (B&C)|(Y&Z)}</li>
   * <li>As an example of deduplication, the expression {@code X&Y&X} normalizes to {@code X&Y}</li>
   * <li>As an example of unneeded quotes, the expression {@code "ABC"&"XYZ"} normalizes to
   * {@code ABC&XYZ}</li>
   * <li>As an example of unneeded parentheses, the expression {@code (((ABC)|(XYZ)))} normalizes to
   * {@code ABC|XYZ}</li>
   * </ul>
   *
   * @param expression an access expression
   * @param normalize If true then the expression will be normalized, if false the expression will
   *        only be validated. Normalization is expensive so only use when needed. If repeatedly
   *        normalizing expressions, consider using a cache that maps un-normalized expressions to
   *        normalized ones. Since the normalization process is deterministic, the computation can
   *        be cached.
   * @throws InvalidAccessExpressionException when the expression is not valid.
   */
  static AccessExpression of(String expression, boolean normalize)
      throws InvalidAccessExpressionException {
    return new AccessExpressionImpl(expression, normalize);
  }

  /**
   * <p>
   * This is equivalent to calling {@code AccessExpression.of(expression, false);}
   */
  static AccessExpression of(byte[] expression) throws InvalidAccessExpressionException {
    return new AccessExpressionImpl(expression, false);
  }

  /**
   * <p>
   * Validates an access expression and creates an immutable AccessExpression object.
   *
   * <p>
   * If only validation is needed, then call {@link #validate(byte[])} because it will avoid copying
   * the expression like this method does. This method must copy the byte array into a String in
   * order to create an immutable AccessExpression.
   *
   * @see #of(String, boolean) for information about normlization.
   * @param expression an access expression that is expected to be encoded using UTF-8
   * @param normalize If true then the expression will be normalized, if false the expression will
   *        only be validated. Normalization is expensive so only use when needed.
   * @throws InvalidAccessExpressionException when the expression is not valid.
   */
  static AccessExpression of(byte[] expression, boolean normalize)
      throws InvalidAccessExpressionException {
    return new AccessExpressionImpl(expression, normalize);
  }

  /**
   * @return an empty AccessExpression that is immutable.
   */
  static AccessExpression of() {
    return AccessExpressionImpl.EMPTY;
  }

  /**
   * Quickly validates that an access expression is properly formed.
   *
   * @param expression a potential access expression that is expected to be encoded using UTF-8
   * @throws InvalidAccessExpressionException if the given expression is not valid
   */
  static void validate(byte[] expression) throws InvalidAccessExpressionException {
    AccessExpressionImpl.validate(expression);
  }

  /**
   * @see #validate(byte[])
   */
  static void validate(String expression) throws InvalidAccessExpressionException {
    AccessExpressionImpl.validate(expression);
  }

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted (surrounded by quotation marks). Use this method to quote authorizations that occur in
   * an access expression. This method will only quote if it is needed.
   */
  static byte[] quote(byte[] authorization) {
    return AccessExpressionImpl.quote(authorization);
  }

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted (surrounded by quotation marks). Use this method to quote authorizations that occur in
   * an access expression. This method will only quote if it is needed.
   */
  static String quote(String authorization) {
    return AccessExpressionImpl.quote(authorization);
  }

}
