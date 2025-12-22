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
package org.apache.accumulo.access.examples.test;

import static org.apache.accumulo.access.examples.ParseExamples.ACCUMULO_ACCESS;
import static org.apache.accumulo.access.examples.ParseExamples.replaceAuthorizations;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.accumulo.access.examples.ParseExamples;
import org.junit.jupiter.api.Test;

// In addition to testing the examples, these test also provide extensive testing of ParsedAccessExpression
public class ParseExamplesTest {

  @Test
  public void testNormalize() {
    // Test data pairs where the first entry of each pair is an expression to normalize and second
    // is the expected normalized value.
    var testData = new ArrayList<List<String>>();

    testData.add(List.of("", ""));
    testData.add(List.of("a", "a"));
    testData.add(List.of("\"a\"", "a"));
    testData.add(List.of("(a)", "a"));
    testData.add(List.of("b|a", "a|b"));
    testData.add(List.of("(b)|a", "a|b"));
    testData.add(List.of("(b)|((a))", "a|b"));
    testData.add(List.of("(b|(a|c))&x", "x&(a|b|c)"));
    testData.add(List.of("(((a)))", "a"));
    testData.add(List.of("b&c&a", "a&b&c"));
    testData.add(List.of("c&b&a", "a&b&c"));
    testData.add(List.of("a&(b&c)", "a&b&c"));
    testData.add(List.of("(a&c)&b", "a&b&c"));
    testData.add(List.of("(d&c&b&a)|(b&c&a&d)", "a&b&c&d"));
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
    testData.add(List.of("(A&(C&B)&C)|((A&C)&(B&C))", "A&B&C"));
    testData.add(List.of("(A|(C|B)|C)&((A|C)|(B|C))", "A|B|C"));
    testData.add(List.of("a|a|a|a", "a"));
    testData.add(List.of("a&a&a&a", "a"));
    testData.add(List.of("(a|a)|(a|a)", "a"));
    testData.add(List.of("(a&a)&(a&a)", "a"));
    testData.add(List.of("X&X&(X|(X&\"X\"))", "X"));
    var auth1 = "\"ABC\"";
    var auth2 = "\"QRS\"";
    var auth3 = "\"X&Z\"";
    testData.add(List.of(
        "(" + auth1 + "&" + auth2 + "&" + auth3 + ")|(" + auth3 + "&" + auth1 + "&" + auth2 + ")",
        "ABC&QRS&\"X&Z\""));

    for (var testCase : testData) {
      assertEquals(2, testCase.size());
      var expression = testCase.get(0);
      var expected = testCase.get(1);

      var actual =
          ParseExamples.normalize(ACCUMULO_ACCESS.newParsedExpression(expression)).expression;
      assertEquals(expected, actual);
    }
  }

  @Test
  public void testReplace() {
    // Test replacement code w/ quoting and escaping.
    var parsed = ACCUMULO_ACCESS.newParsedExpression("((RED&\"ESC\\\\\")|(PINK&BLUE))");
    StringBuilder expressionBuilder = new StringBuilder();
    replaceAuthorizations(parsed, expressionBuilder, Map.of("ESC\\", "NEEDS+QUOTE"));
    assertEquals("(RED&\"NEEDS+QUOTE\")|(PINK&BLUE)", expressionBuilder.toString());

    // Test replacing multiple
    parsed = ACCUMULO_ACCESS.newParsedExpression("((RED&(GREEN|YELLOW))|(PINK&BLUE))");
    expressionBuilder = new StringBuilder();
    replaceAuthorizations(parsed, expressionBuilder, Map.of("RED", "ROUGE", "GREEN", "AQUA"));
    assertEquals("(ROUGE&(AQUA|YELLOW))|(PINK&BLUE)", expressionBuilder.toString());
  }
}
