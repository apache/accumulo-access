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

final class AccessExpressionImpl extends AccessExpression {

  private static final long serialVersionUID = 1L;

  public static final AccessExpression EMPTY = new AccessExpressionImpl("");

  private final String expression;
  private volatile ParsedAccessExpression parsed = null;

  AccessExpressionImpl(String expression) {
    validate(expression);
    this.expression = expression;
  }

  AccessExpressionImpl(byte[] expression) {
    validate(expression);
    this.expression = new String(expression, UTF_8);
  }

  @Override
  public String getExpression() {
    return expression;
  }

  @Override
  public ParsedAccessExpression parse() {
    if (parsed == null) {
      synchronized (this) {
        if (parsed == null) {
          parsed = ParsedAccessExpressionImpl.parseExpression(expression.getBytes(UTF_8));
        }
      }
    }
    return parsed;
  }
}
