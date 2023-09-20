package org.apache.accumulo.access.grammar.antlr;

import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.accumulo.access.IllegalAccessExpressionException;
import org.apache.accumulo.access.grammars.AccessExpressionLexer;
import org.apache.accumulo.access.grammars.AccessExpressionParser;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;

public class AccessExpressionAntlrParser {
  
  private static class AccessExpressionLexerWithErrors extends AccessExpressionLexer {

    private int errors = 0;
    private final CharStream input;
    
    public AccessExpressionLexerWithErrors(CharStream input) {
      super(input);
      this.input = input;
    }

    @Override
    public void recover(LexerNoViableAltException e) {
      System.out.println("Error in lexer. Expression: " + input +", msg: " + e);
      super.recover(e);
      errors++;
    }

    @Override
    public void recover(RecognitionException re) {
      System.out.println("Error in lexer. Expression: " + input +", msg: " + re);
      super.recover(re);
      errors++;
    }
    
    public int getErrorCount() {
      return errors;
    }
    
  }

  private static class ParserErrorListener extends ConsoleErrorListener {

    int errors = 0;
    
    @Override
    public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line,
        int charPositionInLine, String msg, RecognitionException e) {
      super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
      errors++;
    }
    
    public int getErrorCount() {
      return errors;
    }    
    
  }
  
  public static Access_expressionContext parseAccessExpression(byte[] accessExpression) throws IllegalAccessExpressionException {
    return parseAccessExpression(new String(accessExpression, StandardCharsets.UTF_8));
  }
  
  public static Access_expressionContext parseAccessExpression(String accessExpression) throws IllegalAccessExpressionException {
    CodePointCharStream input = CharStreams.fromString(accessExpression);
    AccessExpressionLexerWithErrors lexer = new AccessExpressionLexerWithErrors(input);
    AccessExpressionParser parser = new AccessExpressionParser(new CommonTokenStream(lexer));
    parser.setErrorHandler(new BailErrorStrategy());
    parser.removeErrorListeners();
    ParserErrorListener errorListener = new ParserErrorListener();
    parser.addErrorListener(errorListener);
    try {
      int errors = 0;
      Access_expressionContext ctx = parser.access_expression();
      errors = lexer.getErrorCount();
      errors += errorListener.getErrorCount();
      if (errors > 0 || parser.getNumberOfSyntaxErrors() > 0 || ctx.exception != null) {
        throw new IllegalAccessExpressionException("Parse error", "", 0);
      }
      return ctx;
    } catch (RuntimeException e1) {
      throw new IllegalAccessExpressionException(e1.getMessage(), "", 0);
    }
  }

}
