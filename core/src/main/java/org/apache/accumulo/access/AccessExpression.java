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

/**
 * An immutable wrapper for a validated access expression.
 *
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
