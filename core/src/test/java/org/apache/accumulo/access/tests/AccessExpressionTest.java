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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.ParsedAccessExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class AccessExpressionTest {

  @Test
  public void testGetAuthorizations() {
    var access = Access.builder().build();
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
    testData.add(List.of("\"A#B\"&\"A\\\\B\"", "A#B,A\\B"));

    for (var testCase : testData) {
      assertEquals(2, testCase.size());
      var expression = testCase.get(0);
      var expected = testCase.get(1);
      HashSet<String> found = new HashSet<>();
      access.findAuthorizations(expression, found::add);
      var actual = found.stream().sorted().collect(Collectors.joining(","));
      assertEquals(expected, actual);
      found.clear();
    }

  }

  void checkError(String expression, String expected, int index) {
    var access = Access.builder().build();
    checkError(() -> access.validateExpression(expression), expected, index);
    checkError(() -> access.newExpression(expression), expected, index);
    checkError(() -> access.newParsedExpression(expression), expected, index);
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
    var access = Access.builder().build();
    var ae1 = access.newExpression("A&B");
    var ae2 = access.newExpression("A&C");
    var ae3 = access.newExpression("A&B");
    var ae4 = access.newParsedExpression("A&B");

    assertEquals(ae1, ae3);
    assertEquals(ae1, ae4);
    assertNotEquals(ae1, ae2);
    assertNotEquals(ae3, ae2);
    assertNotEquals(ae2, ae4);

    assertEquals("A&B", ae1.toString());
    assertEquals("A&B", ae4.toString());
    assertEquals("A&B", ae1.getExpression());
    assertEquals("A&B", ae4.getExpression());

    assertEquals(ae1.hashCode(), ae3.hashCode());
    assertEquals(ae1.hashCode(), ae4.hashCode());
    assertNotEquals(ae1.hashCode(), ae2.hashCode());
    assertNotEquals(ae2.hashCode(), ae4.hashCode());
  }

  @Disabled
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

    List<String> specLinesFromMarkdownFile;
    try (var specFile = AccessExpression.class.getResourceAsStream("/SPECIFICATION.md");
        var specFileReader = new InputStreamReader(specFile, UTF_8);
        var specFileBufferedReader = new BufferedReader(specFileReader)) {
      specLinesFromMarkdownFile =
          specFileBufferedReader.lines().dropWhile(line -> !line.startsWith("```ABNF")).skip(1)
              .takeWhile(line -> !line.startsWith("```")).collect(toList());
    }

    assertFalse(specLinesFromAbnfFile.isEmpty()); // make sure we didn't just compare nothing
    assertEquals(specLinesFromAbnfFile, specLinesFromMarkdownFile);
  }

  @Test
  public void testEmpty() {
    var access = Access.builder().build();
    // do not expect empty expression to fail validation
    access.validateExpression("");
    assertEquals("", access.newExpression("").getExpression());

    var parsed = access.newParsedExpression("");
    assertEquals("", parsed.getExpression());
    assertTrue(parsed.getChildren().isEmpty());
    assertEquals(ParsedAccessExpression.ExpressionType.EMPTY, parsed.getType());
  }

  @Test
  public void testImmutable() {
    var access = Access.builder().build();
    var exp = "A&B&(C|D)";
    var exp2 = access.newParsedExpression(exp);

    assertEquals("A&B&(C|D)", exp2.getExpression());

    assertEquals("A", exp2.getChildren().get(0).getExpression());
    assertEquals("B", exp2.getChildren().get(1).getExpression());
    assertEquals("C|D", exp2.getChildren().get(2).getExpression());
    assertEquals("C", exp2.getChildren().get(2).getChildren().get(0).getExpression());
    assertEquals("D", exp2.getChildren().get(2).getChildren().get(1).getExpression());

    // check that children list in parse tree is immutable.
    assertThrows(UnsupportedOperationException.class, () -> exp2.getChildren().remove(0));
    assertThrows(UnsupportedOperationException.class,
        () -> exp2.getChildren().get(2).getChildren().remove(0));
  }

  @Test
  public void testNull() {
    var access = Access.builder().build();
    assertThrows(NullPointerException.class, () -> access.newParsedExpression(null));
    assertThrows(NullPointerException.class, () -> access.validateExpression(null));
    assertThrows(NullPointerException.class, () -> access.newExpression(null));
    assertThrows(NullPointerException.class, () -> access.findAuthorizations(null, auth -> {}));
    assertThrows(NullPointerException.class, () -> access.findAuthorizations("A&B", null));
    assertThrows(NullPointerException.class, () -> access.quote(null));
    assertThrows(NullPointerException.class, () -> access.unquote(null));
  }
}
