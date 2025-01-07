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
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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
      var actual = AccessExpression.of(expression).getAuthorizations().asSet().stream().sorted()
          .collect(Collectors.joining(","));
      assertEquals(expected, actual);
      actual = AccessExpression.of(expression.getBytes(UTF_8)).getAuthorizations().asSet().stream()
          .sorted().collect(Collectors.joining(","));
      assertEquals(expected, actual);
    }

  }

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
      assertEquals(expected, AccessExpression.of(expression, true).getExpression());
      assertEquals(expected, AccessExpression.of(expression.getBytes(UTF_8), true).getExpression());
      assertEquals(expected, AccessExpression
          .of(AccessExpression.of(expression, true).getExpression(), true).getExpression());

      // when not normalizing should see the original expression
      assertEquals(expression, AccessExpression.of(expression).getExpression());
      assertEquals(expression, AccessExpression.of(expression, false).getExpression());
      assertEquals(expression, AccessExpression.of(expression.getBytes(UTF_8)).getExpression());
      assertEquals(expression,
          AccessExpression.of(expression.getBytes(UTF_8), false).getExpression());
    }
  }

  void checkError(String expression, String expected, int index) {
    checkError(() -> AccessExpression.validate(expression), expected, index);
    checkError(() -> AccessExpression.validate(expression.getBytes(UTF_8)), expected, index);
    checkError(() -> AccessExpression.of(expression), expected, index);
    checkError(() -> AccessExpression.of(expression, true), expected, index);
    checkError(() -> AccessExpression.of(expression, false), expected, index);
    checkError(() -> AccessExpression.of(expression.getBytes(UTF_8)), expected, index);
    checkError(() -> AccessExpression.of(expression.getBytes(UTF_8), true), expected, index);
    checkError(() -> AccessExpression.of(expression.getBytes(UTF_8), false), expected, index);
  }

  void checkError(Executable executable, String expected, int index) {
    var exception = assertThrows(InvalidAccessExpressionException.class, executable);
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

  @Test
  public void testEqualsHashcode() {
    var ae1 = AccessExpression.of("A&B");
    var ae2 = AccessExpression.of("A&C");
    var ae3 = AccessExpression.of("B&A", true);

    assertEquals(ae1, ae3);
    assertNotEquals(ae1, ae2);
    assertNotEquals(ae3, ae2);

    assertEquals(ae1.hashCode(), ae3.hashCode());
    assertNotEquals(ae1.hashCode(), ae2.hashCode());
  }

  @Test
  public void testSpecificationDocumentation() throws IOException, URISyntaxException {
    // verify AccessExpression.abnf matches what is documented in SPECIFICATION.md

    // read the abnf spec, ignoring the header
    List<String> specLinesFromAbnfFile;
    try (
        var abnfFileStream =
            AccessExpression.class.getResourceAsStream("specification/AccessExpression.abnf");
        var inputStreamReader = new InputStreamReader(abnfFileStream, UTF_8);
        var bufferedReader = new BufferedReader(inputStreamReader)) {

      Predicate<String> abnfComment = line -> line.startsWith(";");
      Predicate<String> beforeFirstLine = abnfComment.or(String::isBlank);
      specLinesFromAbnfFile = bufferedReader.lines().dropWhile(beforeFirstLine).collect(toList());
    }

    // grab from the markdown, but make sure to skip the markdown triple ticks
    List<String> specLinesFromMarkdownFile = Files.readAllLines(Path.of("SPECIFICATION.md"))
        .stream().dropWhile(line -> !line.startsWith("```ABNF")).skip(1)
        .takeWhile(line -> !line.startsWith("```")).collect(toList());

    assertFalse(specLinesFromAbnfFile.isEmpty()); // make sure we didn't just compare nothing
    assertEquals(specLinesFromAbnfFile, specLinesFromMarkdownFile);
  }

  @Test
  public void testEmpty() {
    // do not expect empty expression to fail validation
    AccessExpression.validate(new byte[0]);
    AccessExpression.validate("");
  }
}
