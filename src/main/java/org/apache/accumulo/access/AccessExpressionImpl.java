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

import static org.apache.accumulo.access.ByteUtils.EMPTY_BYTES;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

final class AccessExpressionImpl extends AccessExpression {

  private static final long serialVersionUID = 1L;

  public static final AccessExpression EMPTY = new AccessExpressionImpl(EMPTY_BYTES);

  private final byte[] expression;
  private final AtomicReference<ParsedAccessExpression> parseTreeRef = new AtomicReference<>();

  AccessExpressionImpl(byte[] expression) {
    validate(expression);
    this.expression = Arrays.copyOf(expression, expression.length);
  }

  @Override
  public byte[] getExpression() {
    return expression;
  }

  @Override
  public ParsedAccessExpression parse() {
    ParsedAccessExpression parseTree = parseTreeRef.get();
    if (parseTree == null) {
      parseTreeRef.compareAndSet(null, ParsedAccessExpressionImpl.parseExpression(expression));
      // must get() again in case another thread won w/ the compare and set, this ensures this
      // method always returns the exact same object
      parseTree = parseTreeRef.get();
    }
    return parseTree;
  }
}
