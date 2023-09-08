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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class AccessExpressionTest {

  @Test
  public void testEmptyExpression(){
    assertEquals("", AccessExpression.of().getExpression());
  }

  @Test
  public void testGetAuthorizations(){
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
      var actual = AccessExpression.of(expression).getAuthorizations().asSet().stream().sorted().collect(Collectors.joining(","));
      assertEquals(expected,actual);
      actual = AccessExpression.of(expression.getBytes(UTF_8)).getAuthorizations().asSet().stream().sorted().collect(Collectors.joining(","));
      assertEquals(expected,actual);
    }

  }


  @Test
  public void testNormalize() {
    // Test data pairs where the first entry of each pair is an expression to normalize and second
    // is the expected normalized value.
    var testData = new ArrayList<List<String>>();

    testData.add(List.of("", ""));
    testData.add(List.of("a", "a"));
    testData.add(List.of("(a)", "a"));
    testData.add(List.of("b|a", "a|b"));
    testData.add(List.of("(b)|a", "a|b"));
    testData.add(List.of("(b)|((a))", "a|b"));
    testData.add(List.of("(b|(a|c))&x", "x&(a|b|c)"));
    testData.add(List.of("(((a)))", "a"));
    testData.add(List.of("Z|M|A", "A|M|Z"));
    testData.add(List.of("Z&M&A", "A&M&Z"));
    testData.add(List.of("(Y&B)|(Z&A)", "(A&Z)|(B&Y)"));
    testData.add(List.of("(Y&B&Y)|(Z&A&Z)", "(A&Z)|(B&Y)"));
    testData.add(List.of("(Y|B)&(Z|A)", "(A|Z)&(B|Y)"));
    testData.add(List.of("(Y|B|Y)&(Z|A|Z)", "(A|Z)&(B|Y)"));
    testData.add(List.of("((Z&B)|(Y&C))&((V&D)|(X&A))", "((A&X)|(D&V))&((B&Z)|(C&Y))"));
    testData.add(List.of("((Z&B&B)|(Y&C&Y))&((V&D)|(X&A))", "((A&X)|(D&V))&((B&Z)|(C&Y))"));
    testData.add(List.of("((Z&B)|(Y&C))&((V&D&D)|(X&A))", "((A&X)|(D&V))&((B&Z)|(C&Y))"));
    testData.add(List.of("((Z|B)&(Y|C))|((V|D)&(X|A))", "((A|X)&(D|V))|((B|Z)&(C|Y))"));
    testData.add(List.of("bz1|bm3|c9|ba4|am", "am|ba4|bm3|bz1|c9"));
    testData.add(List.of("bz1&bm3&c9&ba4&am", "am&ba4&bm3&bz1&c9"));
    testData.add(List.of("((V&D)|(X&A))&A", "A&((A&X)|(D&V))"));
    testData.add(List.of("((V|D)&(X|A))|A", "A|((A|X)&(D|V))"));
    testData.add(List.of("(Z|(X|M))|C|(A|B)", "A|B|C|M|X|Z"));
    testData.add(List.of("(Z&(X&M))&C&(A&B)", "A&B&C&M&X&Z"));
    testData.add(List.of("(Z&(X&(M|L)))&C&(A&B)", "A&B&C&X&Z&(L|M)"));
    testData.add(List.of("(Z|(X|(M&L)))|C|(A|B)", "A|B|C|X|Z|(L&M)"));

    for (var testCase : testData) {
      assertEquals(2, testCase.size());
      var expression = testCase.get(0);
      var expected = testCase.get(1);
      var normalized = AccessExpression.of(expression).normalize();
      assertEquals(expected, normalized);
      assertEquals(expected, AccessExpression.of(expression.getBytes(UTF_8)).normalize());
      assertEquals(expected, AccessExpression.of(normalized).normalize());
    }
  }
}
