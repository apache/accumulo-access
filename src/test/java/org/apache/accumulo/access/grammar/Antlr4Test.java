package org.apache.accumulo.access.grammar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.accumulo.access.grammars.AbnfLexer;
import org.apache.accumulo.access.grammars.AbnfParser;
import org.apache.accumulo.access.grammars.AccessExpressionLexer;
import org.apache.accumulo.access.grammars.AccessExpressionParser;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class Antlr4Test {
  
  @Test
  public void testAbnfSpecificationParses() throws Exception {
    
    // This test checks that the Access Expression ABNF specification
    // text conforms to the ABNF grammar
    
    InputStream is = Antlr4Test.class.getResourceAsStream("/specification.abnf");
    assertNotNull(is);
    
    final AtomicLong errors = new AtomicLong(0);

    AbnfLexer lexer = new AbnfLexer(CharStreams.fromStream(is)) {

      @Override
      public void recover(LexerNoViableAltException e) {
        super.recover(e);
        errors.incrementAndGet();
      }

      @Override
      public void recover(RecognitionException re) {
        super.recover(re);
        errors.incrementAndGet();
      }
      
    };
    
    AbnfParser parser = new AbnfParser(new CommonTokenStream(lexer));

    parser.rulelist();
    assertEquals(0, errors.get());
    
  }
  
  @Test
  public void testExpressions() throws Exception {
    test("A");
    test("A&B");
    test("A|C");
    assertThrows(AssertionFailedError.class, () -> test("A|"));
    test("(A&B)");
    test("(A|B)");
  }
  
  private void test(String input) throws Exception {
    CodePointCharStream expression = CharStreams.fromString(input);
    final AtomicLong errors = new AtomicLong(0);
    AccessExpressionLexer lexer = new AccessExpressionLexer(expression) {

      @Override
      public void recover(LexerNoViableAltException e) {
        super.recover(e);
        errors.incrementAndGet();
      }

      @Override
      public void recover(RecognitionException re) {
        super.recover(re);
        errors.incrementAndGet();
      }
      
    };
    AccessExpressionParser parser = new AccessExpressionParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new ConsoleErrorListener() {

      @Override
      public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line,
          int charPositionInLine, String msg, RecognitionException e) {
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
        errors.incrementAndGet();
      }
      
    });
    parser.access_expression();
    assertEquals(0, errors.get());
    assertEquals(0, parser.getNumberOfSyntaxErrors());
  }

}
