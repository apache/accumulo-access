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

import java.util.List;

import org.apache.accumulo.access.impl.ParsedAccessExpressionImpl;

/**
 * Instances of this class are immutable and wrap a verified access expression and a parse tree for
 * the access expression. To create an instance of this class call
 * {@link Access#newParsedExpression(String)}. The Accumulo Access project has examples that show
 * how to use the parse tree.
 *
 * @since 1.0.0
 */
public sealed abstract class ParsedAccessExpression extends AccessExpression
    permits ParsedAccessExpressionImpl {

  private static final long serialVersionUID = 1L;

  protected ParsedAccessExpression() {}

  /**
   * @since 1.0.0
   */
  public enum ExpressionType {
    /**
     * Indicates an access expression uses and operators at the top level.
     */
    AND,
    /**
     * Indicates an access expression uses or operators at the top level.
     */
    OR,
    /**
     * Indicates an access expression is a single authorization. For this type
     * {@link #getExpression()} will return the authorization in quoted and escaped form. Depending
     * on the use case {@link Access#unquote(String)} may need to be called.
     */
    AUTHORIZATION,
    /**
     * Indicates this is the empty access expression.
     */
    EMPTY
  }

  public abstract ExpressionType getType();

  /**
   * When {@link #getType()} returns OR or AND, then this method will return the sub expressions.
   * When {@link #getType()} method returns AUTHORIZATION or EMPTY this method will return an empty
   * list.
   *
   * @return an immutable list of the sub expressions.
   */
  public abstract List<ParsedAccessExpression> getChildren();
}
