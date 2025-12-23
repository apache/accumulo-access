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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccumuloAccess;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.impl.AccessEvaluatorImpl;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = {"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"},
    justification = "Field is written by Gson")
public class AccessEvaluatorTest {

  enum ExpectedResult {
    ACCESSIBLE, INACCESSIBLE, ERROR
  }

  public static class TestExpressions {
    ExpectedResult expectedResult;
    String[] expressions;
  }

  public static class TestDataSet {
    String description;
    String[][] auths;
    List<TestExpressions> tests;
  }

  static List<TestDataSet> readTestData() throws IOException {
    try (var input =
        AccessEvaluatorTest.class.getClassLoader().getResourceAsStream("testdata.json")) {
      if (input == null) {
        throw new IllegalStateException("could not find resource : testdata.json");
      }
      var json = new String(input.readAllBytes(), UTF_8);

      Type listType = new TypeToken<ArrayList<TestDataSet>>() {}.getType();
      return new Gson().fromJson(json, listType);
    }
  }

  @Test
  public void runTestCases() throws IOException {
    List<TestDataSet> testData = readTestData();

    assertFalse(testData.isEmpty());

    var accumuloAccess = AccumuloAccess.builder().build();

    for (var testSet : testData) {
      System.out.println("runTestCases for " + testSet.description);
      AccessEvaluator evaluator;
      assertTrue(testSet.auths.length >= 1);
      if (testSet.auths.length == 1) {
        evaluator =
            accumuloAccess.newEvaluator(accumuloAccess.newAuthorizations(Set.of(testSet.auths[0])));
        runTestCases(accumuloAccess, testSet, evaluator);

        Set<String> auths = Stream.of(testSet.auths[0]).collect(Collectors.toSet());
        evaluator = accumuloAccess.newEvaluator(auths::contains);
        runTestCases(accumuloAccess, testSet, evaluator);
      } else {
        var authSets = Stream.of(testSet.auths)
            .map(a -> accumuloAccess.newAuthorizations(Set.of(a))).collect(Collectors.toList());
        evaluator = accumuloAccess.newEvaluator(authSets);
        runTestCases(accumuloAccess, testSet, evaluator);
      }
    }
  }

  private static void runTestCases(AccumuloAccess accumuloAccess, TestDataSet testSet,
      AccessEvaluator evaluator) {

    assertFalse(testSet.tests.isEmpty());

    for (var tests : testSet.tests) {

      assertTrue(tests.expressions.length > 0);

      for (var expression : tests.expressions) {

        // Call various APIs with well-formed access expressions to ensure they do not throw an
        // exception
        if (tests.expectedResult == ExpectedResult.ACCESSIBLE
            || tests.expectedResult == ExpectedResult.INACCESSIBLE) {
          accumuloAccess.validate(expression);
          assertEquals(expression, accumuloAccess.newExpression(expression).getExpression());
          // parsing an expression will strip unneeded outer parens
          assertTrue(
              expression.contains(accumuloAccess.newParsedExpression(expression).getExpression()));
          accumuloAccess.findAuthorizations(expression, auth -> {});
        }

        switch (tests.expectedResult) {
          case ACCESSIBLE:
            assertTrue(evaluator.canAccess(expression), expression);
            assertTrue(evaluator.canAccess(accumuloAccess.newExpression(expression)), expression);
            assertTrue(evaluator.canAccess(accumuloAccess.newParsedExpression(expression)),
                expression);
            assertTrue(
                evaluator.canAccess(accumuloAccess.newParsedExpression(expression).getExpression()),
                expression);
            break;
          case INACCESSIBLE:
            assertFalse(evaluator.canAccess(expression), expression);
            assertFalse(evaluator.canAccess(accumuloAccess.newExpression(expression)), expression);
            assertFalse(evaluator.canAccess(accumuloAccess.newParsedExpression(expression)),
                expression);
            assertFalse(
                evaluator.canAccess(accumuloAccess.newParsedExpression(expression).getExpression()),
                expression);
            break;
          case ERROR:
            assertThrows(InvalidAccessExpressionException.class,
                () -> evaluator.canAccess(expression), expression);
            assertThrows(InvalidAccessExpressionException.class,
                () -> accumuloAccess.validate(expression), expression);
            assertThrows(InvalidAccessExpressionException.class,
                () -> accumuloAccess.newExpression(expression), expression);
            assertThrows(InvalidAccessExpressionException.class,
                () -> accumuloAccess.newParsedExpression(expression), expression);
            break;
          default:
            throw new IllegalArgumentException();
        }
      }
    }
  }

  @Test
  public void testEmptyAuthorizations() {
    var accumuloAccess = AccumuloAccess.builder().build();
    // TODO what part of the code throwing the exception?
    assertThrows(IllegalArgumentException.class,
        () -> accumuloAccess.newEvaluator(accumuloAccess.newAuthorizations(Set.of(""))));
    assertThrows(IllegalArgumentException.class,
        () -> accumuloAccess.newEvaluator(accumuloAccess.newAuthorizations(Set.of("", "A"))));
    assertThrows(IllegalArgumentException.class,
        () -> accumuloAccess.newEvaluator(accumuloAccess.newAuthorizations(Set.of("A", ""))));
    assertThrows(IllegalArgumentException.class,
        () -> accumuloAccess.newEvaluator(accumuloAccess.newAuthorizations(Set.of(""))));
  }

  @Test
  public void testSpecialChars() {
    var accumuloAccess = AccumuloAccess.builder().build();
    // special chars do not need quoting
    for (String qt : List.of("A_", "_", "A_C", "_C")) {
      assertEquals(qt, accumuloAccess.quote(qt));
      for (char c : new char[] {'/', ':', '-', '.'}) {
        String qt2 = qt.replace('_', c);
        assertEquals(qt2, accumuloAccess.quote(qt2));
      }
    }

    assertEquals("a_b:c/d.e", accumuloAccess.quote("a_b:c/d.e"));
  }

  @Test
  public void testQuote() {
    var accumuloAccess = AccumuloAccess.builder().build();
    assertEquals("\"A#C\"", accumuloAccess.quote("A#C"));
    assertEquals("A#C", accumuloAccess.unquote(accumuloAccess.quote("A#C")));
    assertEquals("\"A\\\"C\"", accumuloAccess.quote("A\"C"));
    assertEquals("A\"C", accumuloAccess.unquote(accumuloAccess.quote("A\"C")));
    assertEquals("\"A\\\"\\\\C\"", accumuloAccess.quote("A\"\\C"));
    assertEquals("A\"\\C", accumuloAccess.unquote(accumuloAccess.quote("A\"\\C")));
    assertEquals("ACS", accumuloAccess.quote("ACS"));
    assertEquals("ACS", accumuloAccess.unquote(accumuloAccess.quote("ACS")));
    assertEquals("\"九\"", accumuloAccess.quote("九"));
    assertEquals("九", accumuloAccess.unquote(accumuloAccess.quote("九")));
    assertEquals("\"五十\"", accumuloAccess.quote("五十"));
    assertEquals("五十", accumuloAccess.unquote(accumuloAccess.quote("五十")));
  }

  private static String unescape(String s) {
    return AccessEvaluatorImpl.unescape(s).toString();
  }

  @Test
  public void testUnescape() {
    assertEquals("a\"b", unescape("a\\\"b"));
    assertEquals("a\\b", unescape("a\\\\b"));
    assertEquals("a\\\"b", unescape("a\\\\\\\"b"));
    assertEquals("\\\"", unescape("\\\\\\\""));
    assertEquals("a\\b\\c\\d", unescape("a\\\\b\\\\c\\\\d"));

    final String message = "Expected failure to unescape invalid escape sequence";
    final var invalidEscapeSeqList = List.of("a\\b", "a\\b\\c", "a\"b\\");

    invalidEscapeSeqList
        .forEach(seq -> assertThrows(IllegalArgumentException.class, () -> unescape(seq), message));
  }

  // TODO need to copy all test from Accumulo
}
