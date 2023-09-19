package org.apache.accumulo.access.grammar.antlr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.IllegalAccessExpressionException;
import org.apache.accumulo.access.grammars.AccessExpressionLexer;
import org.apache.accumulo.access.grammars.AccessExpressionParser;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_tokenContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_operatorContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_operatorContext;

public class AccessExpressionEvaluator implements AccessEvaluator {
  
  private static class AccessExpressionErrorListener extends AccessExpressionLexer {

    private int errors = 0;
    private final CharStream input;
    
    public AccessExpressionErrorListener(CharStream input) {
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
  
  private class Entity {
    private Set<String> authorizations;

    @Override
    public String toString() {
      return "Entity [authorizations=" + authorizations + "]";
    }
    
  }
  
  private final List<Entity> entities;
  
  public AccessExpressionEvaluator(List<Authorizations> authSets) {
    entities = new ArrayList<>(authSets.size());

    for (Authorizations a : authSets) {
      Set<String> entityAuths = a.asSet();
      Entity e = new Entity();
      entities.add(e);
      e.authorizations = new HashSet<>(entityAuths.size() * 2);
      a.asSet().stream().forEach(auth -> {
        e.authorizations.add(auth);
        e.authorizations.add(new String(AccessEvaluator.escape(auth.getBytes(UTF_8), true), UTF_8));
      });
    }
//    System.out.println("AUTHS: " + entities);
  }
  
  public boolean canAccess(byte[] accessExpression) throws IllegalAccessExpressionException {
    return canAccess(AccessExpression.of(accessExpression));
  }
  
  public boolean canAccess(AccessExpression accessExpression) {
    return canAccess(accessExpression.getExpression());    
  }
  
  public boolean canAccess(String accessExpression) {
    if ("".equals(accessExpression)) {
      return true;
    }
    Access_expressionContext root = parseAccessExpression(accessExpression);
    for (Entity e : entities) {
      if (!evaluate(e, root)) {
        return false;
      }
    }
    return true;
  }
  
  private Boolean evaluate(Entity e, ParseTree node) {
    Boolean retval;
    if (node instanceof Access_tokenContext) {
      retval = e.authorizations.contains(node.getText());
    } else if (node instanceof TerminalNode || node instanceof And_operatorContext | node instanceof Or_operatorContext) {
      retval = null;
    } else {
      int childCount = node.getChildCount();
      int trueCount = 0;
      int falseCount = 0;
      for (int i = 0; i < childCount; i++) {
        Boolean childResult = evaluate(e, node.getChild(i));
        if (childResult == null) {
          continue;
        } else {
          if (childResult == Boolean.TRUE) {
            trueCount++;
          } else {
            falseCount++;
          }
        }
      }
      if (childCount == 1) {
        retval = falseCount == 0;
      } else {
        if (node instanceof Or_expressionContext) {
          retval = trueCount > 0;      
        } else if (node instanceof And_expressionContext) {
          retval = trueCount > 0 && falseCount == 0;
        } else {
          retval = trueCount > 0 && falseCount == 0;
        }
      }
//      System.out.println("child results-> true=" + trueCount +", false=" + falseCount);
    }
//    System.out.println("node: " + node.getClass().getSimpleName()+ ", value: " + node.getText() + ", retval: " + retval);
    return retval;
  }
  
  private Access_expressionContext parseAccessExpression(String accessExpression) throws IllegalAccessExpressionException {
    CodePointCharStream expression = CharStreams.fromString(accessExpression);
    AccessExpressionErrorListener lexer = new AccessExpressionErrorListener(expression);
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
