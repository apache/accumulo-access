package org.apache.accumulo.access.grammar.antlr;

import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.accumulo.access.grammars.AccessExpressionBaseListener;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_tokenContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_expressionContext;

public class AccessExpressionEvaluatorListener extends AccessExpressionBaseListener {

  private final Set<String> auths;
  private boolean canAccess = false;
  
  public AccessExpressionEvaluatorListener(Set<String> auths) {
    this.auths = auths;
  }
  
  @Override
  public void enterAccess_expression(Access_expressionContext ctx) {
    // TODO Auto-generated method stub
    super.enterAccess_expression(ctx);
  }

  @Override
  public void exitAccess_expression(Access_expressionContext ctx) {
    // TODO Auto-generated method stub
    super.exitAccess_expression(ctx);
  }

  @Override
  public void enterAnd_expression(And_expressionContext ctx) {
    // TODO Auto-generated method stub
    super.enterAnd_expression(ctx);
  }

  @Override
  public void exitAnd_expression(And_expressionContext ctx) {
    // TODO Auto-generated method stub
    super.exitAnd_expression(ctx);
  }

  @Override
  public void enterOr_expression(Or_expressionContext ctx) {
  }

  @Override
  public void exitOr_expression(Or_expressionContext ctx) {
    // TODO Auto-generated method stub
    super.exitOr_expression(ctx);
  }

  @Override
  public void enterAccess_token(Access_tokenContext ctx) {
    for (int i=0; i<ctx.getChildCount(); i++) {
      ParseTree pt = ctx.getChild(i);
    }
  }

  @Override
  public void exitAccess_token(Access_tokenContext ctx) {
    // TODO Auto-generated method stub
    super.exitAccess_token(ctx);
  }

  @Override
  public void visitTerminal(TerminalNode node) {
    node.getText();
  }
  
  

}
