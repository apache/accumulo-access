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
package org.apache.accumulo.access.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AuthorizationValidator;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.apache.accumulo.access.testdata.TestDataLoader;
import org.apache.accumulo.access.testdata.TestDataLoader.ExpectedResult;
import org.apache.accumulo.access.testdata.TestDataLoader.TestDataSet;
import org.junit.jupiter.api.Test;

class AccessEvaluatorTest {

  @Test
  public void runTestCases() throws Exception {
    List<TestDataSet> testData = TestDataLoader.readTestData();

    assertFalse(testData.isEmpty());

    var access = Access.builder().build();

    for (var testSet : testData) {
      System.out.println("runTestCases for " + testSet.getDescription());
      AccessEvaluator evaluator;
      assertTrue(testSet.getAuths().length >= 1);
      if (testSet.getAuths().length == 1) {
        evaluator = access.newEvaluator(Set.of(testSet.getAuths()[0]));
        runTestCases(access, testSet, evaluator);

        Set<String> auths = Stream.of(testSet.getAuths()[0]).collect(Collectors.toSet());
        evaluator = access.newEvaluator(auths::contains);
        runTestCases(access, testSet, evaluator);
      } else {
        var authSets =
            Stream.of(testSet.getAuths()).map(a -> Set.of(a)).collect(Collectors.toList());
        evaluator = access.newEvaluator(authSets);
        runTestCases(access, testSet, evaluator);
      }
    }
  }

  private static void runTestCases(Access accumuloAccess, TestDataSet testSet,
      AccessEvaluator evaluator) {

    assertFalse(testSet.getTests().isEmpty());

    for (var tests : testSet.getTests()) {

      assertTrue(tests.getExpressions().length > 0);

      for (var expression : tests.getExpressions()) {

        // Call various APIs with well-formed access expressions to ensure they do not throw an
        // exception
        if (tests.getExpectedResult() == ExpectedResult.ACCESSIBLE
            || tests.getExpectedResult() == ExpectedResult.INACCESSIBLE) {
          accumuloAccess.validateExpression(expression);
          assertEquals(expression, accumuloAccess.newExpression(expression).getExpression());
          // parsing an expression will strip unneeded outer parens
          assertTrue(
              expression.contains(accumuloAccess.newParsedExpression(expression).getExpression()));
          accumuloAccess.findAuthorizations(expression, auth -> {});
        }

        switch (tests.getExpectedResult()) {
          case ACCESSIBLE -> {
            assertTrue(evaluator.canAccess(expression), expression);
            assertTrue(evaluator.canAccess(accumuloAccess.newExpression(expression)), expression);
            assertTrue(evaluator.canAccess(accumuloAccess.newParsedExpression(expression)),
                expression);
            assertTrue(
                evaluator.canAccess(accumuloAccess.newParsedExpression(expression).getExpression()),
                expression);
          }
          case INACCESSIBLE -> {
            assertFalse(evaluator.canAccess(expression), expression);
            assertFalse(evaluator.canAccess(accumuloAccess.newExpression(expression)), expression);
            assertFalse(evaluator.canAccess(accumuloAccess.newParsedExpression(expression)),
                expression);
            assertFalse(
                evaluator.canAccess(accumuloAccess.newParsedExpression(expression).getExpression()),
                expression);
          }
          case ERROR -> {
            assertThrows(InvalidAccessExpressionException.class,
                () -> evaluator.canAccess(expression), expression);
            assertThrows(InvalidAccessExpressionException.class,
                () -> accumuloAccess.validateExpression(expression), expression);
            assertThrows(InvalidAccessExpressionException.class,
                () -> accumuloAccess.newExpression(expression), expression);
            assertThrows(InvalidAccessExpressionException.class,
                () -> accumuloAccess.newParsedExpression(expression), expression);
          }
        }
      }
    }
  }

  @Test
  public void testEmptyAuthorizations() {
    var access = Access.builder().build();
    assertThrows(InvalidAuthorizationException.class, () -> access.newEvaluator(Set.of("")));
    assertThrows(InvalidAuthorizationException.class, () -> access.newEvaluator(Set.of("", "A")));
    assertThrows(InvalidAuthorizationException.class, () -> access.newEvaluator(Set.of("A", "")));
    assertThrows(InvalidAuthorizationException.class, () -> access.newEvaluator(Set.of("")));
  }

  @Test
  public void testSpecialChars() {
    var access = Access.builder().build();
    // special chars do not need quoting
    for (String qt : List.of("A_", "_", "A_C", "_C")) {
      assertEquals(qt, access.quote(qt));
      for (char c : new char[] {'/', ':', '-', '.'}) {
        String qt2 = qt.replace('_', c);
        assertEquals(qt2, access.quote(qt2));
      }
    }

    assertEquals("a_b:c/d.e", access.quote("a_b:c/d.e"));
  }

  @Test
  public void testQuote() {
    var access = Access.builder().build();
    assertEquals("\"A#C\"", access.quote("A#C"));
    assertEquals("A#C", access.unquote(access.quote("A#C")));
    assertEquals("\"A\\\"C\"", access.quote("A\"C"));
    assertEquals("A\"C", access.unquote(access.quote("A\"C")));
    assertEquals("\"A\\\"\\\\C\"", access.quote("A\"\\C"));
    assertEquals("A\"\\C", access.unquote(access.quote("A\"\\C")));
    assertEquals("ACS", access.quote("ACS"));
    assertEquals("ACS", access.unquote(access.quote("ACS")));
    assertEquals("\"九\"", access.quote("九"));
    assertEquals("九", access.unquote(access.quote("九")));
    assertEquals("\"五十\"", access.quote("五十"));
    assertEquals("五十", access.unquote(access.quote("五十")));

    var e = assertThrows(InvalidAuthorizationException.class, () -> access.quote(""));
    assertEquals("authorization : '' (empty string)", e.getMessage());

    testUnquoteError(access, "\"\"\"\"", "unescaped quote");

    for (var illegalInput : List.of("", "\"\"")) {
      testUnquoteError(access, illegalInput, "empty string");
    }

    for (var illegalInput : List.of("\"", "AB\"", "\"AB", "\"A", "B\"")) {
      testUnquoteError(access, illegalInput, "unbalanced quotes");
    }
  }

  private static void testUnquoteError(Access access, String illegalInput,
      String expectedErrorMsg) {
    var e = assertThrows(InvalidAuthorizationException.class, () -> access.unquote(illegalInput),
        illegalInput);
    assertTrue(e.getMessage().contains(expectedErrorMsg));
  }

  private static String unescape(String s) {
    return CharUtils.unescape(s).toString();
  }

  @Test
  public void testUnescape() {
    assertEquals("a\"b", unescape("a\\\"b"));
    assertEquals("a\\b", unescape("a\\\\b"));
    assertEquals("a\\\"b", unescape("a\\\\\\\"b"));
    assertEquals("\\\"", unescape("\\\\\\\""));
    assertEquals("a\\b\\c\\d", unescape("a\\\\b\\\\c\\\\d"));

    List.of("a\\b", "a\\b\\c").forEach(seq -> {
      var e = assertThrows(InvalidAuthorizationException.class, () -> unescape(seq),
          "Expected failure to unescape invalid escape sequence: " + seq);
      assertTrue(e.getMessage().contains("invalid escape"), seq + " -> " + e.getMessage());
    });
    List.of("a\"b\\").forEach(seq -> {
      var e = assertThrows(InvalidAuthorizationException.class, () -> unescape(seq),
          "Expected failure to unescape invalid escape sequence: " + seq);
      assertTrue(e.getMessage().contains("unescaped quote"), seq + " -> " + e.getMessage());
    });
  }

  @Test
  public void testNullAuthValidator() {
    assertThrows(NullPointerException.class,
        () -> Access.builder().authorizationValidator(null).build());
  }

  @Test
  public void testAuthValidation() {
    // This test ensures that unquoted and unescaped auths are passed to the auth validator.
    HashSet<String> seenAuths = new HashSet<>();
    AuthorizationValidator authorizationValidator = (auth, authChars) -> {
      seenAuths.add(auth.toString());
      return AuthorizationValidator.DEFAULT.test(auth, authChars);
    };
    var access = Access.builder().authorizationValidator(authorizationValidator).build();
    var qa1 = access.quote("A");
    var qa2 = access.quote("B/C");
    var qa3 = access.quote("D\\E");

    assertEquals(Set.of("A", "B/C", "D\\E"), seenAuths);
    seenAuths.clear();

    assertEquals("A", access.unquote(qa1));
    assertEquals("B/C", access.unquote(qa2));
    assertEquals("D\\E", access.unquote(qa3));

    assertEquals(Set.of("A", "B/C", "D\\E"), seenAuths);
    seenAuths.clear();

    var eval = access.newEvaluator(Set.of("A"));
    assertFalse(eval.canAccess(qa1 + "&" + qa2 + "&" + qa3));
    assertEquals(Set.of("A", "B/C", "D\\E"), seenAuths);
    seenAuths.clear();

    eval = access.newEvaluator(a -> a.equals("A"));
    assertFalse(eval.canAccess(qa1 + "&" + qa2 + "&" + qa3));
    assertEquals(Set.of("A", "B/C", "D\\E"), seenAuths);
    seenAuths.clear();

  }

  // TODO need to copy all test from Accumulo
}
