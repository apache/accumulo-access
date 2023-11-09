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
 * A set of utility methods for operating on Access Expressions.
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
 * System.out.println(visExp);
 * System.out.println(AccessExpression.getAuthorizations(visExp));
 * }
 * </pre>
 *
 * The above example will print the following.
 *
 * <pre>
 * (CAT&amp;"🦖")|(CAT&amp;"🦕"&amp;CAT)
 * [🦖, CAT, 🦕]
 * </pre>
 *
 * The following code will throw an {@link IllegalAccessExpressionException} because the expression
 * is not valid.
 *
 * <pre>
 *     {@code
 * AccessExpression.validate("A&B|C");
 * }
 * </pre>
 *
 *
 * @see <a href="https://github.com/apache/accumulo-access">Accumulo Access Documentation</a>
 * @since 1.0.0
 */
public interface AccessExpression {

  /**
   * @return the unique set of authorizations that occur in the expression. For example, for the
   *         expression {@code (A&B)|(A&C)|(A&D)}, this method would return {@code [A,B,C,D]}.
   */
  static Authorizations getAuthorizations(byte[] expression) {
    return AccessExpressionImpl.getAuthorizations(expression);
  }

  /**
   * @see #getAuthorizations(byte[])
   */
  static Authorizations getAuthorizations(String expression) {
    return AccessExpressionImpl.getAuthorizations(expression);
  }

  /**
   * Quickly validates that an access expression is properly formed.
   *
   * @throws IllegalAccessExpressionException if the given expression is not valid
   */
  static void validate(byte[] expression) throws IllegalAccessExpressionException {
    AccessExpressionImpl.validate(expression);
  }

  /**
   * @see #validate(byte[])
   */
  static void validate(String expression) throws IllegalAccessExpressionException {
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
