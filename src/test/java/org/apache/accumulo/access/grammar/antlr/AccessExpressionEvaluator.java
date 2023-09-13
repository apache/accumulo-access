package org.apache.accumulo.access.grammar.antlr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicLong;

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
import org.apache.accumulo.access.IllegalAccessExpressionException;
import org.apache.accumulo.access.grammars.AccessExpressionLexer;
import org.apache.accumulo.access.grammars.AccessExpressionParser;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;

public class AccessExpressionEvaluator implements AccessEvaluator {

  private final AccessExpressionEvaluatorVisitor visitor;
  
  public AccessExpressionEvaluator(Authorizations auths) {
    visitor = new AccessExpressionEvaluatorVisitor(auths.asSet());
  }
  
  public boolean canAccess(byte[] accessExpression) throws IllegalAccessExpressionException {
    return canAccess(AccessExpression.of(accessExpression));
  }
  
  public boolean canAccess(AccessExpression accessExpression) {
    return canAccess(accessExpression.getExpression());    
  }
  
  public boolean canAccess(String accessExpression) {
    Access_expressionContext root = parseAccessExpression(accessExpression);
    return visitor.visitAccess_expression(root);
  }
  
  private Access_expressionContext parseAccessExpression(String accessExpression) {
    CodePointCharStream expression = CharStreams.fromString(accessExpression);
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
      return ctx;
    } catch (RuntimeException e1) {
      throw new AssertionError(e1);
    } finally {
      assertEquals(0, errors.get());
      assertEquals(0, parser.getNumberOfSyntaxErrors());
    }
  }
  
}
