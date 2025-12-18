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
package org.apache.accumulo.access.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AND;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AUTHORIZATION;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.EMPTY;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.OR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.ParsedAccessExpression;
import org.junit.jupiter.api.Test;

public class ParsedAccessExpressionTest {
  @Test
  public void testParsing() {
    String expression = "(BLUE&(RED|PINK|YELLOW))|((YELLOW|\"GREEN/GREY\")&(RED|BLUE))|BLACK";
    for (var parsed : List.of(AccessExpression.parse(expression),
        AccessExpression.parse(expression.getBytes(UTF_8)), AccessExpression.of(expression).parse(),
        AccessExpression.of(expression.getBytes(UTF_8)).parse())) {
      // verify root node
      verify("(BLUE&(RED|PINK|YELLOW))|((YELLOW|\"GREEN/GREY\")&(RED|BLUE))|BLACK", OR, 3, parsed);

      // verify all nodes at level 1 in the tree
      verify("BLUE&(RED|PINK|YELLOW)", AND, 2, parsed, 0);
      verify("(YELLOW|\"GREEN/GREY\")&(RED|BLUE)", AND, 2, parsed, 1);
      verify("BLACK", AUTHORIZATION, 0, parsed, 2);

      // verify all nodes at level 2 in the tree
      verify("BLUE", AUTHORIZATION, 0, parsed, 0, 0);
      verify("RED|PINK|YELLOW", OR, 3, parsed, 0, 1);
      verify("YELLOW|\"GREEN/GREY\"", OR, 2, parsed, 1, 0);
      verify("RED|BLUE", OR, 2, parsed, 1, 1);

      // verify all nodes at level 3 in the tree
      verify("RED", AUTHORIZATION, 0, parsed, 0, 1, 0);
      verify("PINK", AUTHORIZATION, 0, parsed, 0, 1, 1);
      verify("YELLOW", AUTHORIZATION, 0, parsed, 0, 1, 2);
      verify("YELLOW", AUTHORIZATION, 0, parsed, 1, 0, 0);
      verify("\"GREEN/GREY\"", AUTHORIZATION, 0, parsed, 1, 0, 1);
      verify("RED", AUTHORIZATION, 0, parsed, 1, 1, 0);
      verify("BLUE", AUTHORIZATION, 0, parsed, 1, 1, 1);
    }
  }

  @Test
  public void testEmpty() {
    var parsed = AccessExpression.parse("");
    verify("", EMPTY, 0, parsed);
    parsed = AccessExpression.parse(new byte[0]);
    verify("", EMPTY, 0, parsed);
  }

  @Test
  public void testParseTwice() {
    for (var expression : List.of(AccessExpression.of("A&B"),
        AccessExpression.of("A&B".getBytes(UTF_8)))) {
      var parsed = expression.parse();
      assertNotSame(expression, parsed);
      assertEquals(expression.getExpression(), parsed.getExpression());
      assertSame(parsed, expression.parse());
      assertSame(parsed, expression.parse());
    }
  }

  /**
   * Traverses a path in the parse tree an verifies the node at the end of the path.
   */
  private void verify(String expectedExpression, ParsedAccessExpression.ExpressionType expectedType,
      int expectedChildren, ParsedAccessExpression parsed, int... path) {
    for (int childIndex : path) {
      parsed = parsed.getChildren().get(childIndex);
    }

    assertEquals(expectedExpression, parsed.getExpression());
    assertEquals(expectedType, parsed.getType());
    assertEquals(expectedChildren, parsed.getChildren().size());
    assertSame(parsed, parsed.parse());
    // check list of children is immutable
    var fp = parsed;
    assertThrows(UnsupportedOperationException.class, () -> fp.getChildren().clear());
  }
}
