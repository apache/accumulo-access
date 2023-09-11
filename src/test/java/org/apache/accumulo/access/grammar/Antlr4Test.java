package org.apache.accumulo.access.grammar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.accumulo.access.AccessEvaluatorTest;
import org.apache.accumulo.access.AccessEvaluatorTest.ExpectedResult;
import org.apache.accumulo.access.AccessEvaluatorTest.TestDataSet;
import org.apache.accumulo.access.AccessEvaluatorTest.TestExpressions;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.IllegalAccessExpressionException;
import org.apache.accumulo.access.grammars.AccessExpressionLexer;
import org.apache.accumulo.access.grammars.AccessExpressionParser;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;
import org.junit.jupiter.api.Test;

public class Antlr4Test {

  @Test
  public void testCompareWithAccessExpressionImplParsing() throws Exception {
    // This test checks that the parsing of the AccessExpressions in testdata.json
    // using ANTLR have the same outcome as AccessExpression.of()
    List<TestDataSet> testData = AccessEvaluatorTest.readTestData();
    for (TestDataSet testSet : testData) {
      for (TestExpressions test : testSet.tests) {
        ExpectedResult result = test.expectedResult;
        for (String cv : test.expressions) {
          System.out.println("Testing: " + cv);
          if (result == ExpectedResult.ERROR) {
            assertThrows(IllegalAccessExpressionException.class, () -> AccessExpression.of(cv));
            assertThrows(AssertionError.class, () -> test(cv));
          } else {
            AccessExpression.of(cv);
            test(cv);
          }
        }
      }
    }
  }
  
  private void test(String input) throws Exception {
    CodePointCharStream expression = CharStreams.fromString(input);
    final AtomicLong errors = new AtomicLong(0);
    AccessExpressionLexer lexer = new AccessExpressionLexer(expression) {

      @Override
      public void recover(LexerNoViableAltException e) {
        System.out.println("Error in lexer. Expression: " + expression +", msg: " + e);
        super.recover(e);
        errors.incrementAndGet();
      }

      @Override
      public void recover(RecognitionException re) {
        System.out.println("Error in lexer. Expression: " + expression +", msg: " + re);
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

}
