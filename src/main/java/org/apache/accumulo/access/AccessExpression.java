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

/**
 * An opaque type that contains a parsed access expression. When this type is constructed with
 * {@link #of(String)} and then used with {@link AccessEvaluator#canAccess(AccessExpression)} it can
 * be more efficient and avoid re-parsing the expression.
 *
 * Below is an example of using this API.
 *
 * <pre>
 *     {@code
 * var auth1 = AccessExpression.quote("CAT");
 * var auth2 = AccessExpression.quote("🦕");
 * var auth3 = AccessExpression.quote("🦖");
 * var visExp = AccessExpression
 *     .of("(" + auth1 + "&" + auth3 + ")|(" + auth1 + "&" + auth2 + "&" + auth1 + ")");
 * System.out.println(visExp.getExpression());
 * System.out.println(visExp.normalize());
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
 * @see <a href="https://github.com/apache/accumulo-access">Accumulo Access Documentation</a>
 * @since 1.0.0
 */
public interface AccessExpression {

  /**
   * @return the expression that was used to create this object.
   */
  String getExpression();

  /**
   * Deduplicate, sort, and flatten expressions.
   *
   * <p>
   * As an example of flattening, the expression {@code A&(B&C)} can be flattened to {@code A&B&C}.
   *
   * <p>
   * As an example of sorting, the expression {@code (Z&Y)|(C&B)} can be sorted to
   * {@code (B&C)|(Y&Z)}
   *
   * <p>
   * As an example of deduplication, the expression {@code X&Y&X} is equivalent to {@code X&Y}
   *
   * @return A normalized version of the visibility expression that removes duplicates and orders
   *         the expression in a consistent way.
   */
  String normalize();

  /**
   * @return the unique authorizations that occur in the expression. For example, for the expression
   *         {@code (A&B)|(A&C)|(A&D)} this method would return {@code [A,B,C,D]]}
   */
  Authorizations getAuthorizations();

  static AccessExpression of(String expression) throws IllegalAccessExpressionException {
    return new AccessExpressionImpl(expression);
  }

  /**
   * @param expression is expected to be encoded using UTF-8
   */
  static AccessExpression of(byte[] expression) throws IllegalAccessExpressionException {
    byte[] copy = new byte[expression.length];
    System.arraycopy(expression, 0, copy, 0, expression.length);
    return new AccessExpressionImpl(copy);
  }

  /**
   * @return an empty VisibilityExpression.
   */
  static AccessExpression of() {
    return AccessExpressionImpl.EMPTY;
  }

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted. Use this method to quote authorizations that occur in an access expression. This method
   * will only quote if it is needed.
   */
  static byte[] quote(byte[] authorization) {
    return AccessExpressionImpl.quote(authorization);
  }

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted. Use this method to quote authorizations that occur in an access expression. This method
   * will only quote if it is needed.
   */
  static String quote(String authorization) {
    return AccessExpressionImpl.quote(authorization);
  }

}
