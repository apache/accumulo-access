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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.accumulo.access.impl.AccessEvaluatorImpl;
import org.apache.accumulo.access.impl.AccessExpressionImpl;
import org.apache.accumulo.access.impl.BytesWrapper;
import org.apache.accumulo.access.impl.ParsedAccessExpressionImpl;
import org.apache.accumulo.access.impl.ParserEvaluator;
import org.apache.accumulo.access.impl.Tokenizer;

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
public abstract sealed class AccessExpression implements Serializable
    permits ParsedAccessExpression, AccessExpressionImpl {

  private static final AccessExpression EMPTY = new AccessExpressionImpl("");

  private static final long serialVersionUID = 1L;

  protected AccessExpression() {}

  /**
   * @return the expression that was used to create this object.
   */
  public abstract String getExpression();

  /**
   * Parses the access expression if it was never parsed before. If this access expression was
   * created using {@link #parse(String)} or {@link #parse(byte[])} then it will have a parse from
   * inception and this method will return itself. If the access expression was created using
   * {@link #of(String)} or {@link #of(byte[])} then this method will create a parse tree the first
   * time its called and remember it, returning the remembered parse tree on subsequent calls.
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

  /**
   * Validates an access expression and returns an immutable AccessExpression object. If passing
   * access expressions as arguments in code, consider using this type instead of a String. The
   * advantage of passing this type over a String is that its known to be a valid expression. Also
   * this type is much more informative than a String type. Conceptually this method calls
   * {@link #validate(String)} and if that passes creates an immutable object that wraps the
   * expression.
   *
   * @throws InvalidAccessExpressionException if the given expression is not valid
   * @throws NullPointerException when the argument is null
   */
  public static AccessExpression of(String expression) throws InvalidAccessExpressionException {
    return new AccessExpressionImpl(expression);
  }

  /**
   * @see #of(String)
   */
  public static AccessExpression of(byte[] expression) throws InvalidAccessExpressionException {
    return new AccessExpressionImpl(expression);
  }

  /**
   * @return an empty AccessExpression that is immutable.
   */
  public static AccessExpression of() {
    return EMPTY;
  }

  /**
   * @see #parse(String)
   */
  public static ParsedAccessExpression parse(byte[] expression)
      throws InvalidAccessExpressionException {
    if (expression.length == 0) {
      return ParsedAccessExpressionImpl.EMPTY;
    }

    return ParsedAccessExpressionImpl.parseExpression(Arrays.copyOf(expression, expression.length));
  }

  /**
   * Validates an access expression and returns an immutable object with a parse tree. Creating the
   * parse tree is expensive relative to calling {@link #of(String)} or {@link #validate(String)},
   * so only use this method when the parse tree is always needed. If the code may only use the
   * parse tree sometimes, then it may be best to call {@link #of(String)} to create the access
   * expression and then call {@link AccessExpression#parse()} when needed.
   *
   * @throws NullPointerException when the argument is null
   * @throws InvalidAccessExpressionException if the given expression is not valid
   */
  public static ParsedAccessExpression parse(String expression)
      throws InvalidAccessExpressionException {
    if (expression.isEmpty()) {
      return ParsedAccessExpressionImpl.EMPTY;
    }
    // Calling expression.getBytes(UTF8) will create a byte array that only this code has access to,
    // so not need to copy that byte array. That is why parse(byte[]) is not called here because
    // that would copy the array again.
    return ParsedAccessExpressionImpl.parseExpression(expression.getBytes(UTF_8));
  }

  /**
   * Quickly validates that an access expression is properly formed.
   *
   * @param expression a potential access expression that is expected to be encoded using UTF-8
   * @throws InvalidAccessExpressionException if the given expression is not valid
   * @throws NullPointerException when the argument is null
   */
  public static void validate(byte[] expression) throws InvalidAccessExpressionException {
    if (expression.length > 0) {
      Predicate<Tokenizer.AuthorizationToken> atp = authToken -> true;
      ParserEvaluator.parseAccessExpression(expression, atp, atp);
    } // else empty expression is valid, avoid object allocation
  }

  /**
   * @see #validate(byte[])
   */
  public static void validate(String expression) throws InvalidAccessExpressionException {
    if (!expression.isEmpty()) {
      validate(expression.getBytes(UTF_8));
    } // else empty expression is valid, avoid object allocation
  }

  /**
   * Validates and access expression and finds all authorizations in it passing them to the
   * authorizationConsumer. For example, for the expression {@code (A&B)|(A&C)|(A&D)}, this method
   * would pass {@code A,B,A,C,A,D} to the consumer one at a time. The function will conceptually
   * call {@link #unquote(String)} prior to passing an authorization to authorizationConsumer.
   *
   * <p>
   * What this method does could also be accomplished by creating a parse tree using
   * {@link AccessExpression#parse(String)} and then recursively walking the parse tree. The
   * implementation of this method does not create a parse tree and is much faster. If a parse tree
   * is already available, then it would likely be faster to use it rather than call this method.
   * </p>
   *
   * @throws InvalidAccessExpressionException when the expression is not valid.
   * @throws NullPointerException when any argument is null
   */
  public static void findAuthorizations(String expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException {
    findAuthorizations(expression.getBytes(UTF_8), authorizationConsumer);
  }

  /**
   * @see #findAuthorizations(String, Consumer)
   */
  public static void findAuthorizations(byte[] expression, Consumer<String> authorizationConsumer)
      throws InvalidAccessExpressionException {
    var bytesWrapper = ParserEvaluator.lookupWrappers.get();
    Predicate<Tokenizer.AuthorizationToken> atp = authToken -> {
      bytesWrapper.set(authToken.data, authToken.start, authToken.len);
      authorizationConsumer.accept(AccessEvaluatorImpl.unescape(bytesWrapper));
      return true;
    };
    ParserEvaluator.parseAccessExpression(expression, atp, atp);
  }

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted (surrounded by quotation marks). Use this method to quote authorizations that occur in
   * an access expression. This method will only quote if it is needed.
   *
   * @throws NullPointerException when the argument is null
   */
  public static byte[] quote(byte[] term) {
    if (term.length == 0) {
      throw new IllegalArgumentException("Empty strings are not legal authorizations.");
    }

    boolean needsQuote = false;

    for (byte b : term) {
      if (!Tokenizer.isValidAuthChar(b)) {
        needsQuote = true;
        break;
      }
    }

    if (!needsQuote) {
      return term;
    }

    return AccessEvaluatorImpl.escape(term, true);
  }

  /**
   * Authorizations occurring in an access expression can only contain the characters listed in the
   * <a href=
   * "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification</a> unless
   * quoted (surrounded by quotation marks). Use this method to quote authorizations that occur in
   * an access expression. This method will only quote if it is needed.
   *
   * @throws NullPointerException when the argument is null
   */
  public static String quote(String term) {
    return new String(quote(term.getBytes(UTF_8)), UTF_8);
  }

  /**
   * Reverses what {@link #quote(String)} does, so will unquote and unescape an authorization if
   * needed. If the authorization is not quoted then it is returned as-is.
   *
   * @throws NullPointerException when the argument is null
   */
  public static String unquote(String term) {
    if (term.equals("\"\"") || term.isEmpty()) {
      throw new IllegalArgumentException("Empty strings are not legal authorizations.");
    }

    if (term.charAt(0) == '"' && term.charAt(term.length() - 1) == '"') {
      term = term.substring(1, term.length() - 1);
      return AccessEvaluatorImpl.unescape(new BytesWrapper(term.getBytes(UTF_8)));
    } else {
      return term;
    }
  }
}
