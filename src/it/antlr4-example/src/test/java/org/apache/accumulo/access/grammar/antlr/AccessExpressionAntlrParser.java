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
import org.apache.accumulo.access.InvalidAccessExpressionException;
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
      System.out.println("Error in lexer. Expression: " + input + ", msg: " + e);
      super.recover(e);
      errors++;
    }

    @Override
    public void recover(RecognitionException re) {
      System.out.println("Error in lexer. Expression: " + input + ", msg: " + re);
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

  public static Access_expressionContext parseAccessExpression(byte[] accessExpression)
      throws InvalidAccessExpressionException {
    return parseAccessExpression(new String(accessExpression, StandardCharsets.UTF_8));
  }

  public static Access_expressionContext parseAccessExpression(String accessExpression)
      throws InvalidAccessExpressionException {
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
        throw new InvalidAccessExpressionException("Parse error", "", 0);
      }
      return ctx;
    } catch (RuntimeException e1) {
      throw new InvalidAccessExpressionException(e1.getMessage(), "", 0);
    }
  }

}
