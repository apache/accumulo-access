package org.apache.accumulo.access.grammar.antlr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.IllegalAccessExpressionException;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_tokenContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.And_operatorContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_expressionContext;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Or_operatorContext;

public class AccessExpressionAntlrEvaluator implements AccessEvaluator {

  private class Entity {

    private Set<String> authorizations;

    @Override
    public String toString() {
      return "Entity [authorizations=" + authorizations + "]";
    }

  }

  private final List<Entity> entities;

  public AccessExpressionAntlrEvaluator(List<Authorizations> authSets) {
    entities = new ArrayList<>(authSets.size());

    for (Authorizations a : authSets) {
      Set<String> entityAuths = a.asSet();
      Entity e = new Entity();
      entities.add(e);
      e.authorizations = new HashSet<>(entityAuths.size() * 2);
      a.asSet().stream().forEach(auth -> {
        e.authorizations.add(auth);
        String quoted = AccessExpression.quote(auth);
        if (!quoted.startsWith("\"")) {
          quoted = '"' + quoted + '"';
        }
        e.authorizations.add(quoted);
      });
    }
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
    return canAccess(AccessExpressionAntlrParser.parseAccessExpression(accessExpression));
  }

  public boolean canAccess(Access_expressionContext parsedExpression) {
    for (Entity e : entities) {
      if (!evaluate(e, parsedExpression)) {
        return false;
      }
    }
    return true;
  }

  private Boolean evaluate(Entity e, ParseTree node) {
    Boolean retval;
    if (node instanceof Access_tokenContext) {
      retval = e.authorizations.contains(node.getText());
    } else if (node instanceof TerminalNode
        || node instanceof And_operatorContext | node instanceof Or_operatorContext) {
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
    }
    return retval;
  }

}
