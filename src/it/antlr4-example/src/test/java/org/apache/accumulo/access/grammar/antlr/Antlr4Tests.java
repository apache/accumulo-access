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
package org.apache.accumulo.access.grammar.antlr;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;
import org.apache.accumulo.access.antlr.TestDataLoader;
import org.apache.accumulo.access.antlr.TestDataLoader.ExpectedResult;
import org.apache.accumulo.access.antlr.TestDataLoader.TestDataSet;
import org.apache.accumulo.access.antlr.TestDataLoader.TestExpressions;
import org.apache.accumulo.access.grammars.AccessExpressionLexer;
import org.apache.accumulo.access.grammars.AccessExpressionParser;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;
import org.junit.jupiter.api.Test;

public class Antlr4Tests {

  private void testParse(String input) throws Exception {
    CodePointCharStream expression = CharStreams.fromString(input);
    final AtomicLong errors = new AtomicLong(0);
    AccessExpressionLexer lexer = new AccessExpressionLexer(expression) {

      @Override
      public void recover(LexerNoViableAltException e) {
        System.out.println("Error in lexer. Expression: " + expression + ", msg: " + e);
        super.recover(e);
        errors.incrementAndGet();
      }

      @Override
      public void recover(RecognitionException re) {
        System.out.println("Error in lexer. Expression: " + expression + ", msg: " + re);
        super.recover(re);
        errors.incrementAndGet();
      }

    };
    AccessExpressionParser parser = new AccessExpressionParser(new CommonTokenStream(lexer));
    parser.setErrorHandler(new BailErrorStrategy());
    parser.removeErrorListeners();
    parser.addErrorListener(new ConsoleErrorListener() {

      @Override
      public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line,
          int charPositionInLine, String msg, RecognitionException e) {
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
        errors.incrementAndGet();
      }

    });
    try {
      Access_expressionContext ctx = parser.access_expression();
      assertNull(ctx.exception);
    } catch (RuntimeException e1) {
      throw new AssertionError(e1);
    }
    assertEquals(0, errors.get());
    assertEquals(0, parser.getNumberOfSyntaxErrors());
  }

  @Test
  public void testCompareWithAccessExpressionImplParsing() throws Exception {
    // This test checks that the parsing of the AccessExpressions in testdata.json
    // using ANTLR have the same outcome as AccessExpression.of()
    List<TestDataSet> testData = TestDataLoader.readTestData();
    for (TestDataSet testSet : testData) {
      for (TestExpressions test : testSet.tests) {
        ExpectedResult result = test.expectedResult;
        for (String cv : test.expressions) {
          if (result == ExpectedResult.ERROR) {
            assertThrows(InvalidAccessExpressionException.class, () -> AccessExpression.of(cv));
            assertThrows(AssertionError.class, () -> testParse(cv));
          } else {
            AccessExpression.of(cv);
            testParse(cv);
          }
        }
      }
    }
  }

  @Test
  public void testSimpleEvaluation() throws Exception {
    String accessExpression = "(one&two)|(foo&bar)";
    Authorizations auths = new Authorizations(Set.of("four", "three", "one", "two"));
    AccessExpressionAntlrEvaluator eval = new AccessExpressionAntlrEvaluator(List.of(auths));
    assertTrue(eval.canAccess(accessExpression));
  }

  @Test
  public void testSimpleEvaluationFailure() throws Exception {
    String accessExpression = "(A&B&C)";
    Authorizations auths = new Authorizations(Set.of("A", "C"));
    AccessExpressionAntlrEvaluator eval = new AccessExpressionAntlrEvaluator(List.of(auths));
    assertFalse(eval.canAccess(accessExpression));
  }

  @Test
  public void testCompareAntlrEvaluationAgainstAccessEvaluatorImpl() throws Exception {
    // This test checks that the evaluation of the AccessExpressions in testdata.json
    // using ANTLR have the same outcome as AccessEvaluatorImpl
    List<TestDataSet> testData = TestDataLoader.readTestData();
    for (TestDataSet testSet : testData) {

      List<Authorizations> authSets =
          Stream.of(testSet.auths).map(a -> new Authorizations(Set.of(a))).collect(Collectors.toList());
      AccessEvaluator evaluator = AccessEvaluator.of(authSets);
      AccessExpressionAntlrEvaluator antlr = new AccessExpressionAntlrEvaluator(authSets);

      for (TestExpressions test : testSet.tests) {
        for (String expression : test.expressions) {
          switch (test.expectedResult) {
            case ACCESSIBLE:
              assertTrue(evaluator.canAccess(expression), expression);
              assertTrue(evaluator.canAccess(expression.getBytes(UTF_8)), expression);
              assertTrue(evaluator.canAccess(AccessExpression.of(expression)), expression);
              assertTrue(evaluator.canAccess(AccessExpression.of(expression.getBytes(UTF_8))),
                  expression);
              assertEquals(expression,
                  AccessExpression.of(expression.getBytes(UTF_8)).getExpression());
              assertEquals(expression, AccessExpression.of(expression).getExpression());

              assertTrue(antlr.canAccess(expression), expression);
              assertTrue(antlr.canAccess(expression.getBytes(UTF_8)), expression);
              assertTrue(antlr.canAccess(AccessExpression.of(expression)), expression);
              assertTrue(antlr.canAccess(AccessExpression.of(expression.getBytes(UTF_8))),
                  expression);

              break;
            case INACCESSIBLE:
              assertFalse(evaluator.canAccess(expression), expression);
              assertFalse(evaluator.canAccess(expression.getBytes(UTF_8)), expression);
              assertFalse(evaluator.canAccess(AccessExpression.of(expression)), expression);
              assertFalse(evaluator.canAccess(AccessExpression.of(expression.getBytes(UTF_8))),
                  expression);
              assertEquals(expression,
                  AccessExpression.of(expression.getBytes(UTF_8)).getExpression());
              assertEquals(expression, AccessExpression.of(expression).getExpression());

              assertFalse(antlr.canAccess(expression), expression);
              assertFalse(antlr.canAccess(expression.getBytes(UTF_8)), expression);
              assertFalse(antlr.canAccess(AccessExpression.of(expression)), expression);
              assertFalse(antlr.canAccess(AccessExpression.of(expression.getBytes(UTF_8))),
                  expression);

              break;
            case ERROR:
              assertThrows(InvalidAccessExpressionException.class,
                  () -> evaluator.canAccess(expression), expression);
              assertThrows(InvalidAccessExpressionException.class,
                  () -> evaluator.canAccess(expression.getBytes(UTF_8)), expression);
              assertThrows(InvalidAccessExpressionException.class,
                  () -> evaluator.canAccess(AccessExpression.of(expression)), expression);
              assertThrows(InvalidAccessExpressionException.class,
                  () -> evaluator.canAccess(AccessExpression.of(expression.getBytes(UTF_8))),
                  expression);

              assertThrows(InvalidAccessExpressionException.class,
                  () -> antlr.canAccess(expression), expression);
              assertThrows(InvalidAccessExpressionException.class,
                  () -> antlr.canAccess(expression.getBytes(UTF_8)), expression);
              assertThrows(InvalidAccessExpressionException.class,
                  () -> antlr.canAccess(AccessExpression.of(expression)), expression);
              assertThrows(InvalidAccessExpressionException.class,
                  () -> antlr.canAccess(AccessExpression.of(expression.getBytes(UTF_8))),
                  expression);
              break;
            default:
              throw new IllegalArgumentException();
          }
        }
      }
    }

  }
}
