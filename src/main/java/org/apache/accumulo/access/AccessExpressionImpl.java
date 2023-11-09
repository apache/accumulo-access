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

import java.util.HashSet;
import java.util.function.Predicate;

class AccessExpressionImpl implements AccessExpression {

  static Authorizations getAuthorizations(byte[] expression) {
    HashSet<String> auths = new HashSet<>();
    Tokenizer tokenizer = new Tokenizer(expression);
    Predicate<BytesWrapper> predicate = bw -> true;
    ParserEvaluator.BytesWrapperFactory lookupWrapper = (bytes, offset, len) -> {
      auths.add(new String(bytes, offset, len, UTF_8));
      return null;
    };
    ParserEvaluator.parseAccessExpression(predicate, tokenizer, lookupWrapper);
    return Authorizations.of(auths);
  }

  static Authorizations getAuthorizations(String expression) {
    return getAuthorizations(expression.getBytes(UTF_8));
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

  static void validate(byte[] expression) throws IllegalAccessExpressionException {
    Tokenizer tokenizer = new Tokenizer(expression);
    Predicate<BytesWrapper> predicate = bw -> true;
    ParserEvaluator.BytesWrapperFactory lookupWrapper = (bytes, offset, len) -> null;
    ParserEvaluator.parseAccessExpression(predicate, tokenizer, lookupWrapper);
  }

  static void validate(String expression) throws IllegalAccessExpressionException {
    validate(expression.getBytes(UTF_8));
  }
}
