package org.apache.accumulo.access.grammar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.accumulo.access.grammar.antlr.Antlr4Tests;
import org.apache.accumulo.access.grammars.AbnfLexer;
import org.apache.accumulo.access.grammars.AbnfParser;
import org.junit.jupiter.api.Test;

// This test uses the ANTLR ABNF grammar to parse the
// accumulo-access ANBF specification to validate that
// it is proper ANBF.
public class SpecificationTest {

  @Test
  public void testAbnfSpecificationParses() throws Exception {

    // The test resource specification.abnf is a copy of the ABNF
    // from SPECIFICATION.md
    
    InputStream is = Antlr4Tests.class.getResourceAsStream("/specification.abnf");
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
    parser.removeErrorListeners();
    parser.addErrorListener(new ConsoleErrorListener() {
      @Override
      public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line,
          int charPositionInLine, String msg, RecognitionException e) {
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
        errors.incrementAndGet();
      }
    });

    parser.rulelist();
    assertEquals(0, errors.get());
    
  }

}
