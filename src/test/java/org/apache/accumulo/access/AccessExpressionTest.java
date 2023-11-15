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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class AccessExpressionTest {

  @Test
  public void testGetAuthorizations() {
    // Test data pairs where the first entry of each pair is an expression to normalize and second
    // is the expected authorization in the expression
    var testData = new ArrayList<List<String>>();

    testData.add(List.of("", ""));
    testData.add(List.of("a", "a"));
    testData.add(List.of("(a)", "a"));
    testData.add(List.of("Z|M|A", "A,M,Z"));
    testData.add(List.of("Z&M&A", "A,M,Z"));
    testData.add(List.of("(Y|B|Y)&(Z|A|Z)", "A,B,Y,Z"));
    testData.add(List.of("(Y&B&Y)|(Z&A&Z)", "A,B,Y,Z"));
    testData.add(List.of("(A1|B1)&((A1|B2)&(B2|C1))", "A1,B1,B2,C1"));

    for (var testCase : testData) {
      assertEquals(2, testCase.size());
      var expression = testCase.get(0);
      var expected = testCase.get(1);
      var actual = AccessExpression.getAuthorizations(expression).asSet().stream().sorted()
          .collect(Collectors.joining(","));
      assertEquals(expected, actual);
      actual = AccessExpression.getAuthorizations(expression.getBytes(UTF_8)).asSet().stream()
          .sorted().collect(Collectors.joining(","));
      assertEquals(expected, actual);
    }
  }

  void checkError(String expression, String expected, int index) {
    var exception = assertThrows(IllegalAccessExpressionException.class,
        () -> AccessExpression.validate(expression));
    assertTrue(exception.getMessage().contains(expected));
    assertEquals(index, exception.getIndex());

    exception = assertThrows(IllegalAccessExpressionException.class,
        () -> AccessExpression.validate(expression.getBytes(UTF_8)));
    assertTrue(exception.getMessage().contains(expected));
    assertEquals(index, exception.getIndex());

    exception = assertThrows(IllegalAccessExpressionException.class,
        () -> AccessExpression.getAuthorizations(expression));
    assertTrue(exception.getMessage().contains(expected));
    assertEquals(index, exception.getIndex());

    exception = assertThrows(IllegalAccessExpressionException.class,
        () -> AccessExpression.getAuthorizations(expression.getBytes(UTF_8)));
    assertTrue(exception.getMessage().contains(expected));
    assertEquals(index, exception.getIndex());
  }

  @Test
  public void testErrorMessages() {
    checkError("a|b&c", "Cannot mix '|' and '&'", 3);
    checkError("a&b|c", "Cannot mix '|' and '&'", 3);

    checkError("(a|b", "Expected ')' instead saw end of input", 4);
    checkError("((a|b)", "Expected ')' instead saw end of input", 6);
    checkError("((a|b)(c)", "Expected ')' instead saw '('", 6);
    checkError("((a|b)~", "Expected ')' instead saw '~'", 6);
    checkError("((a|b)a", "Expected ')' instead saw 'a'", 6);

    checkError("#", "Expected a '(' character or an authorization token instead saw '#'", 0);
    checkError("()", "Expected a '(' character or an authorization token instead saw ')'", 1);
    checkError("a&", "Expected a '(' character or an authorization token instead saw end of input",
        2);
    checkError("a&b&",
        "Expected a '(' character or an authorization token instead saw end of input", 4);
    checkError("a|", "Expected a '(' character or an authorization token instead saw end of input",
        2);
    checkError("a|b|",
        "Expected a '(' character or an authorization token instead saw end of input", 4);

    checkError("a#", "Unexpected character '#'", 1);
    checkError("a&b#", "Unexpected character '#'", 3);
    checkError("a|b#", "Unexpected character '#'", 3);
    checkError("(a|b)(c)", "Unexpected character '('", 5);

    checkError("\"\"", "Empty authorization token in quotes", 0);
    checkError("A&\"\"", "Empty authorization token in quotes", 2);
    checkError("(A|\"\")", "Empty authorization token in quotes", 3);

    checkError("\"\\9\"", "Invalid escaping within quotes", 1);
    checkError("ERR&\"\\9\"", "Invalid escaping within quotes", 5);
  }
}
