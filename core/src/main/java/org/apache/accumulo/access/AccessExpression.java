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

import org.apache.accumulo.access.impl.AccessExpressionImpl;

// TODO update or remove example code... maybe remove it because the project has example code that is tested
/**
 * This class offers the ability to operate on access expressions.
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
 * var exp = "(" + auth1 + "&" + auth3 + ")|(" + auth1 + "&" + auth2 + ")";
 *
 * // Validate the expression w/o creating an object
 * AccessExpression.validate(exp);
 * System.out.println(exp);
 *
 * // Validate the expression and create an immutable AccessExpression object.  This object can be passed around in code and other code knows it valid and does not need to revalidate.
 * AccessExpression accessExpression = AccessExpression.of(exp);
 * System.out.println(accessExpression);
 *
 * // Print the authorization in the expression
 * AccessExpression.findAuthorizations(exp, System.out::println);
 *
 * // Create an AccessExpression with a parse tree.  Creating this is more expensive than calling AccessExpression.of(), so it should only be used if the parse tree is needed.
 * ParsedAccessExpression parsed = AccessExpression.parse(exp);
 * System.out.println("type:"+parsed.getType()+" child[0]:"+parsed.getChildren().get(0)+" child[1]:"+  child[1]:"+parsed.getChildren().get(1));
 *
 * }
 * </pre>
 *
 * The above example will print the following.
 *
 * <pre>
 * {@code
 * (CAT&"🦖")|(CAT&"🦕")
 * (CAT&"🦖")|(CAT&"🦕")
 * CAT
 * 🦖
 * CAT
 * 🦕
 * type:OR child[0]:CAT&"🦖" child[1]:CAT&"🦕"
 * }
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
public sealed abstract class AccessExpression implements Serializable
    permits AccessExpressionImpl, ParsedAccessExpression {

  private static final long serialVersionUID = 1L;

  protected AccessExpression() {}

  /**
   * @return the expression that was used to create this object.
   */
  public abstract String getExpression();

  /**
   * Parses the access expression if it was never parsed before. If this access expression was
   * created using {@link Access#newParsedExpression(String)} then it will have a parse from
   * inception and this method will return itself. If the access expression was created using
   * {@link Access#newExpression(String)} then this method will create a parse tree the first time
   * its called and remember it, returning the remembered parse tree on subsequent calls.
   */
  public abstract ParsedAccessExpression parse();

  @Override
  public boolean equals(Object o) {
    if (o instanceof AccessExpression) {
      return ((AccessExpression) o).getExpression().equals(getExpression());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return getExpression().hashCode();
  }

  @Override
  public String toString() {
    return getExpression();
  }
}
