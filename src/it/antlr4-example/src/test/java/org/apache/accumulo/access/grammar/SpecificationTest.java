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
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.grammar.antlr.Antlr4Tests;
import org.apache.accumulo.access.grammars.AbnfLexer;
import org.apache.accumulo.access.grammars.AbnfParser;
import org.junit.jupiter.api.Test;

// This test uses the ANTLR ABNF grammar to parse the
// AccessExpression ANBF specification to validate that
// it is proper ANBF.
public class SpecificationTest {

  @Test
  public void testAbnfSpecificationParses() throws Exception {

    var is = AccessExpression.class.getResourceAsStream("specification/AccessExpression.abnf");
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
