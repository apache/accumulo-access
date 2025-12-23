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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.accumulo.access.AccumuloAccess;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAuthorizationException;
import org.junit.jupiter.api.Test;

public class AuthorizationTest {

  @Test
  public void testEquality() {
    var accumuloAccess = AccumuloAccess.builder().build();
    Authorizations auths1 = accumuloAccess.newAuthorizations(Set.of("A", "B", "C"));
    Authorizations auths2 = accumuloAccess.newAuthorizations(Set.of("A", "B", "C"));

    assertEquals(auths1, auths2);
    assertEquals(auths1.hashCode(), auths2.hashCode());

    Authorizations auths3 = accumuloAccess.newAuthorizations(Set.of("D", "E", "F"));

    assertNotEquals(auths1, auths3);
    assertNotEquals(auths1.hashCode(), auths3.hashCode());
  }

  @Test
  public void testEmpty() {
    var accumuloAccess = AccumuloAccess.builder().build();
    // check if new object is allocated
    assertSame(accumuloAccess.newAuthorizations(), accumuloAccess.newAuthorizations());
    // check if optimization is working
    assertSame(accumuloAccess.newAuthorizations(), accumuloAccess.newAuthorizations(Set.of()));
    assertEquals(Set.of(), accumuloAccess.newAuthorizations().asSet());
    assertSame(Set.of(), accumuloAccess.newAuthorizations().asSet());
  }

  @Test
  public void testAuthorizationValidation() {

    // create an instance of accumulo access that expects all auths to start with a lower case
    // letter followed by one or more lower case letters or digits.
    var accumuloAccess = AccumuloAccess.builder().authorizationValidator(auth -> {
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
    var badAuth = new String(new char[] {'a', c});

    var accumuloAccess = AccumuloAccess.builder().build();
    runTest("ac", "a9", "dc", badAuth, accumuloAccess);
  }

  private static void runTest(String goodAuth1, String goodAuth2, String goodAuth3, String badAuth,
      AccumuloAccess accumuloAccess) {
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
      var exp = a1 + "|(" + a2 + "&" + a3 + ")|" + a4;
      var exception =
          assertThrows(InvalidAuthorizationException.class, () -> accumuloAccess.validate(exp));
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
    }

    var exception =
        assertThrows(InvalidAuthorizationException.class, () -> accumuloAccess.quote(badAuth));
    assertTrue(exception.getMessage().contains(badAuth));

    exception = assertThrows(InvalidAuthorizationException.class,
        () -> accumuloAccess.unquote('"' + badAuth + '"'));
    assertTrue(exception.getMessage().contains(badAuth));
  }
}
