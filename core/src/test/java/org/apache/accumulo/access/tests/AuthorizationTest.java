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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.apache.accumulo.access.impl.Tokenizer;
import org.junit.jupiter.api.Test;

public class AuthorizationTest {

  @Test
  public void testEquality() {
    var access = Access.builder().build();
    Authorizations auths1 = access.newAuthorizations(Set.of("A", "B", "C"));
    Authorizations auths2 = access.newAuthorizations(Set.of("A", "B", "C"));

    assertEquals(auths1, auths2);
    assertEquals(auths1.hashCode(), auths2.hashCode());

    Authorizations auths3 = access.newAuthorizations(Set.of("D", "E", "F"));

    assertNotEquals(auths1, auths3);
    assertNotEquals(auths1.hashCode(), auths3.hashCode());
  }

  @Test
  public void testEmpty() {
    var access = Access.builder().build();
    // check if new object is allocated
    assertSame(access.newAuthorizations(Set.of()), access.newAuthorizations(Set.of()));
    assertEquals(Set.of(), access.newAuthorizations(Set.of()).asSet());
    assertSame(Set.of(), access.newAuthorizations(Set.of()).asSet());
  }

  @Test
  public void testAuthorizationValidation() {

    // create an instance of accumulo access that expects all auths to start with a lower case
    // letter followed by one or more lower case letters or digits.
    var accumuloAccess = Access.builder().authorizationValidator((auth, quoting) -> {
      if (auth.length() < 2) {
        return false;
      }

      char c = auth.charAt(0);
      if (!Character.isLowerCase(c) || !Character.isLetter(c)) {
        return false;
      }

      for (int i = 1; i < auth.length(); i++) {
        c = auth.charAt(i);
        boolean valid = Character.isDigit(c) || (Character.isLetter(c) && Character.isLowerCase(c));
        if (!valid) {
          return false;
        }
      }

      return true;
    }).build();

    runTest("ac", "a9", "dc", "9a", accumuloAccess);
  }

  @Test
  public void testNonUnicode() {
    // test with a character that is not unicode
    char c = '\u0378';
    assertFalse(Character.isDefined(c));
    assertFalse(Character.isISOControl(c));
    var badAuth = new String(new char[] {'a', c});

    var access = Access.builder().build();
    runTest("ac", "a9", "dc", badAuth, access);
  }

  @Test
  public void testControlCharacters() {
    char c = '\u000c';
    assertTrue(Character.isDefined(c));
    assertTrue(Character.isISOControl(c));

    var access = Access.builder().build();
    var badAuth = new String(new char[] {'a', c});
    runTest("ac", "a9", "dc", badAuth, access);
  }

  @Test
  public void testReplacementCharacter() {
    char c = '\uFFFD';
    assertEquals(c + "", UTF_8.newDecoder().replacement());

    var access = Access.builder().build();
    var badAuth = new String(new char[] {'a', c});
    runTest("ac", "a9", "dc", badAuth, access);
  }

  @Test
  public void testAuthorizationCharacters() {
    for (char c = 0; c < Character.MAX_VALUE; c++) {
      boolean valid = (c >= 'a' && c <= 'z') | (c >= 'A' && c <= 'Z') | (c >= '0' && c <= '9')
          | c == '_' | c == '-' | c == ':' | c == '.' | c == '/';
      // This code had a bug where it was only considering the lower 8 bits of the 16 bits in the
      // char and that caused an obscure and easy to miss problem. So this test was written to cover
      // all 64k chars.
      assertEquals(valid, Tokenizer.isValidAuthChar(c));
    }
  }

  @Test
  public void testUnescaped() {
    // This test ensures that auth passed to the authorization validator are unescaped, even if they
    // are escaped in the expression
    var accumuloAccess = Access.builder().authorizationValidator((auth, quoting) -> {
      for (int i = 0; i < auth.length(); i++) {
        assertNotEquals('\\', auth.charAt(i));
      }
      return true;
    }).build();

    var quoted = accumuloAccess.quote("ABC\"D");
    assertEquals('"' + "ABC\\\"D" + '"', quoted);
    assertEquals("ABC\"D", accumuloAccess.unquote(quoted));

    var auths1 = accumuloAccess.newAuthorizations(Set.of("ABC\"D", "DEF"));
    var auths2 = accumuloAccess.newAuthorizations(Set.of("ABC\"D", "XYZ"));
    var evaluator = accumuloAccess.newEvaluator(auths1);

    assertTrue(evaluator.canAccess(quoted + "|XYZ"));
    assertTrue(evaluator.canAccess("(XYZ&RST)|" + quoted));

    assertTrue(evaluator.canAccess(accumuloAccess.newExpression("(XYZ&RST)|" + quoted)));
    assertTrue(evaluator.canAccess(accumuloAccess.newParsedExpression("(XYZ&RST)|" + quoted)));
    assertTrue(evaluator.canAccess(accumuloAccess.newExpression("(XYZ&RST)|" + quoted).parse()));

    var multiEvaluator = accumuloAccess.newEvaluator(List.of(auths1, auths2));
    assertTrue(multiEvaluator.canAccess(quoted + "|XYZ"));
    assertFalse(multiEvaluator.canAccess(quoted + "&DEF"));
    assertTrue(evaluator.canAccess(quoted + "&DEF"));

  }

  @Test
  public void testMultiCharCodepoint() {
    // Some unicode code points span two UTF-16 chars, this test ensures its possible to handle
    // those.

    String doubleChar = "";
    // find any valid code point that takes two chars
    for (int i = 1 << 16; i < Integer.MAX_VALUE; i++) {
      if (Character.isDefined(i)) {
        var dc = Character.toChars(i);
        if (dc.length == 2) {
          doubleChar = new String(dc);
          break;
        }
      }
    }

    assertEquals(2, doubleChar.length());

    // create an auth that uses doubleChar
    var auth1 = "a" + doubleChar;
    var auth2 = "abc";

    var access = Access.builder().build();

    var exp1 = access.quote(auth1) + "&" + auth2;
    var exp2 = access.quote(auth1) + "|" + auth2;

    assertEquals('"' + auth1 + '"' + "&" + auth2, exp1);
    assertEquals('"' + auth1 + '"' + "|" + auth2, exp2);

    var auths1 = access.newAuthorizations(Set.of(auth1, auth2));
    var evaluator1 = access.newEvaluator(auths1);
    assertTrue(evaluator1.canAccess(exp1));
    assertTrue(evaluator1.canAccess(exp2));

    var auths2 = access.newAuthorizations(Set.of(auth1));
    var evaluator2 = access.newEvaluator(auths2);
    assertFalse(evaluator2.canAccess(exp1));
    assertTrue(evaluator2.canAccess(exp2));

    var auths3 = access.newAuthorizations(Set.of(auth2));
    var evaluator3 = access.newEvaluator(auths3);
    assertFalse(evaluator3.canAccess(exp1));
    assertTrue(evaluator3.canAccess(exp2));
  }

  private static void runTest(String goodAuth1, String goodAuth2, String goodAuth3, String badAuth,
      Access accumuloAccess) {
    List<String> auths = List.of(goodAuth1, goodAuth2, badAuth, goodAuth3);

    var auths1 = accumuloAccess.newAuthorizations(Set.of(goodAuth1, goodAuth2));
    var auths2 = accumuloAccess.newAuthorizations(Set.of(goodAuth3, goodAuth2));

    var evaluator = accumuloAccess.newEvaluator(auths1);
    var multiEvaluator = accumuloAccess.newEvaluator(List.of(auths1, auths2));
    var evaluator2 = accumuloAccess.newEvaluator(auth -> auth.equals(goodAuth3));

    for (int i = 0; i < 4; i++) {
      var a1 = auths.get(i);
      var a2 = auths.get((i + 1) % 4);
      var a3 = auths.get((i + 2) % 4);
      var a4 = auths.get((i + 3) % 4);

      // create the same expression with the invalid auth in different places
      var exp =
          '"' + a1 + '"' + "|(" + '"' + a2 + '"' + "&" + '"' + a3 + '"' + ")|" + '"' + a4 + '"';
      var exception = assertThrows(InvalidAuthorizationException.class,
          () -> accumuloAccess.validateExpression(exp));
      assertTrue(exception.getMessage().contains(badAuth));

      exception = assertThrows(InvalidAuthorizationException.class,
          () -> accumuloAccess.newExpression(exp));
      assertTrue(exception.getMessage().contains(badAuth));

      exception = assertThrows(InvalidAuthorizationException.class,
          () -> accumuloAccess.newParsedExpression(exp));
      assertTrue(exception.getMessage().contains(badAuth));

      exception = assertThrows(InvalidAuthorizationException.class, () -> evaluator.canAccess(exp));
      assertTrue(exception.getMessage().contains(badAuth));

      exception =
          assertThrows(InvalidAuthorizationException.class, () -> multiEvaluator.canAccess(exp));
      assertTrue(exception.getMessage().contains(badAuth));

      exception =
          assertThrows(InvalidAuthorizationException.class, () -> evaluator2.canAccess(exp));
      assertTrue(exception.getMessage().contains(badAuth));

      exception = assertThrows(InvalidAuthorizationException.class,
          () -> accumuloAccess.findAuthorizations(exp, a -> {}));
      assertTrue(exception.getMessage().contains(badAuth));

      exception = assertThrows(InvalidAuthorizationException.class,
          () -> accumuloAccess.newAuthorizations(Set.of(a1, a2, a3, a4)));
      assertTrue(exception.getMessage().contains(badAuth));

      if (badAuth.chars().anyMatch(c -> !Tokenizer.isValidAuthChar((char) c))) {
        // If the expression is created w/o quoting then the bad auth should just be seen as an
        // invalid character in the expression and it should not even consult the authorization
        // validation code. This should result in a different exception.
        var exp2 = a1 + "|(" + a2 + "&" + a3 + ")|" + a4;
        var exception2 =
            assertThrows(InvalidAccessExpressionException.class, () -> evaluator.canAccess(exp2));
        assertTrue(exception2.getMessage().contains(badAuth));
      } else {
        // The bad auth does not need quoting, but should still see an invalid auth exception
        var exp2 = a1 + "|(" + a2 + "&" + a3 + ")|" + a4;
        var exception2 =
            assertThrows(InvalidAuthorizationException.class, () -> evaluator.canAccess(exp2));
        assertTrue(exception2.getMessage().contains(badAuth));
      }
    }

    var exception =
        assertThrows(InvalidAuthorizationException.class, () -> accumuloAccess.quote(badAuth));
    assertTrue(exception.getMessage().contains(badAuth));

    exception = assertThrows(InvalidAuthorizationException.class,
        () -> accumuloAccess.unquote('"' + badAuth + '"'));
    assertTrue(exception.getMessage().contains(badAuth));
  }
}
