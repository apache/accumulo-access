package org.apache.accumulo.access.grammar.antlr;

import java.util.Set;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.accumulo.access.grammars.AccessExpressionBaseVisitor;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_tokenContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_operatorContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.ExpressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_operatorContext;

public class AccessExpressionEvaluatorVisitor extends AccessExpressionBaseVisitor<Boolean> {

  private enum Context { NONE, AND, OR };
  
  private final Set<String> auths;
  private Context currentContext = Context.NONE;
  private boolean initialEvaluation = true;
  
  public AccessExpressionEvaluatorVisitor(Set<String> auths) {
    System.out.println("AUTHS: " + auths);
    this.auths = auths;
  }

  @Override
  protected Boolean defaultResult() {
    return Boolean.FALSE;
  }

  @Override
  public Boolean visitAnd_expression(And_expressionContext ctx) {
    Context priorContext = currentContext;
    try {
      currentContext = Context.AND;
      System.out.println("Current: AND, Prior: " + priorContext);
      Boolean result = super.visitAnd_expression(ctx);
      System.out.println("Result: " + result);
      return result;
    } finally {
      currentContext = priorContext;
    }
  }

  @Override
  public Boolean visitOr_expression(Or_expressionContext ctx) {
    Context priorContext = currentContext;
    try {
      currentContext = Context.OR;
      System.out.println("Current: OR, Prior: " + priorContext);
      Boolean result = super.visitOr_expression(ctx);
      System.out.println("Result: " + result);
      return result;
    } finally {
      currentContext = priorContext;
    }
  }

  @Override
  public Boolean visitAccess_token(Access_tokenContext ctx) {
    String text = ctx.getText();
    // return true for the characters in the grammar
    if (text.equals("&") || text.equals("|") || text.equals("(") || text.equals(")") || text.equals("<EOF>")) {
      System.out.println("AccessToken: " + text + ", return null due to grammar character");
      return null;
    }
    boolean inSet =  auths.contains(text);
    System.out.println("AccessToken: " + text + ", in auth set? " + inSet);
    return inSet;
  }

  @Override
  public Boolean visitExpression(ExpressionContext ctx) {
    System.out.println("Expression: returning true");
    super.visitExpression(ctx);
    return true;
  }

  @Override
  public Boolean visitAnd_operator(And_operatorContext ctx) {
    System.out.println("Expression: and operator");
    super.visitAnd_operator(ctx);
    return null;
  }

  @Override
  public Boolean visitOr_operator(Or_operatorContext ctx) {
    System.out.println("Expression: or operator");
    super.visitOr_operator(ctx);
    return null;
  }

  @Override
  public Boolean visitTerminal(TerminalNode node) {
    String text = node.getText();
    // return true for the characters in the grammar
    if (text.equals("&") || text.equals("|") || text.equals("(") || text.equals(")") || text.equals("<EOF>")) {
      System.out.println("terminal: " + text + ", return null due to grammar character");
      return null;
    }
    boolean inSet =  auths.contains(text);
    System.out.println("terminal: " + text + ", in auth set? " + inSet);
    return inSet;
  }

  @Override
  protected Boolean aggregateResult(Boolean aggregate, Boolean nextResult) {
    System.out.println("Aggregation: " + aggregate + ", " + nextResult);
    Boolean result = null;
    if (initialEvaluation) {
      // The defaultResult is false. However, we don't want to AND or OR our
      // first result with this as that will provide an incorrect answer.
      // If this is the first evaluation, then return the first result
      result = nextResult;
      System.out.println("Returning " + result + " due to initial evaluation");
      initialEvaluation = false;
    } else if (nextResult == null) {
      // Null is returned when grammar characters are found in the terminal nodes
      // In this case, return the prior result
      result = aggregate;
    } else if (currentContext == Context.AND) {
      result = aggregate && nextResult;
    } else if (currentContext == Context.OR) {
      result = aggregate || nextResult;
    } else {
      result = nextResult;
    }
    System.out.println("Aggregation Result: " + result);
    return result;
  }

}
