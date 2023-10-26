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

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

class AccessExpressionImpl implements AccessExpression {

  private final byte[] expression;

  final AeNode aeNode;

  private final AtomicReference<String> expressionString = new AtomicReference<>(null);

  @Override
  public String getExpression() {
    var expStr = expressionString.get();
    if (expStr != null) {
      return expStr;
    }

    return expressionString.updateAndGet(es -> es == null ? new String(expression, UTF_8) : es);
  }

  // must create this after creating EMPTY_NODE
  static final AccessExpression EMPTY = new AccessExpressionImpl("");

  @Override
  public String normalize() {
    StringBuilder builder = new StringBuilder();
    aeNode.normalize().stringify(builder, false);
    return builder.toString();
  }

  @Override
  public Authorizations getAuthorizations() {
    HashSet<String> auths = new HashSet<>();
    aeNode.getAuthorizations(auths::add);
    return Authorizations.of(auths);
  }

  /**
   * Creates an AccessExpression.
   *
   * @param expression An expression of the rights needed to see specific data. The expression
   *        syntax is defined within the <a href=
   *        "https://github.com/apache/accumulo-access/blob/main/SPECIFICATION.md">specification
   *        doc</a>
   */
  AccessExpressionImpl(String expression) {
    this(expression.getBytes(UTF_8));
    expressionString.set(expression);
  }

  /**
   * Creates an AccessExpression from a string already encoded in UTF-8 bytes.
   *
   * @param expression AccessExpression, encoded as UTF-8 bytes
   * @see #AccessExpressionImpl(String)
   */
  AccessExpressionImpl(byte[] expression) {
    this.expression = expression;
    aeNode = Parser.parseAccessExpression(expression);
  }

  @Override
  public String toString() {
    return getExpression();
  }

  /**
   * See {@link #equals(AccessExpressionImpl)}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AccessExpressionImpl) {
      return equals((AccessExpressionImpl) obj);
    }
    return false;
  }

  /**
   * Compares two AccessExpressions for string equivalence, not as a meaningful comparison of terms
   * and conditions.
   *
   * @param otherLe other AccessExpression
   * @return true if this AccessExpression equals the other via string comparison
   */
  boolean equals(AccessExpressionImpl otherLe) {
    return Arrays.equals(expression, otherLe.expression);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(expression);
  }

  static String quote(String term) {
    return new String(quote(term.getBytes(UTF_8)), UTF_8);
  }

  /**
   * Properly quotes terms in an AccessExpression. If no quoting is needed, then nothing is done.
   *
   * @param term term to quote, encoded as UTF-8 bytes
   * @return quoted term (unquoted if unnecessary), encoded as UTF-8 bytes
   * @see #quote(String)
   */
  static byte[] quote(byte[] term) {
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
}
