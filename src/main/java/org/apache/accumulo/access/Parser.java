package org.apache.accumulo.access;

import java.util.ArrayList;

/**
 * Code for parsing an access expression and creating a parse tree of type {@link AeNode}
 */
final class Parser {
  public static AeNode parseAccessExpression(byte[] expression) {

    Tokenizer tokenizer = new Tokenizer(expression);

    if (!tokenizer.hasNext()) {
      return AeNode.of();
    }

    var node = parseExpression(tokenizer);

    if (tokenizer.hasNext()) {
      // not all input was read, so not a valid expression
      tokenizer.error("Unexpected character '" + (char) tokenizer.peek() + "'");
    }

    return node;
  }

  private static AeNode parseExpression(Tokenizer tokenizer) {

    AeNode first = parseParenExpressionOrAuthorization(tokenizer);

    if (tokenizer.hasNext() && (tokenizer.peek() == '&' || tokenizer.peek() == '|')) {
      var nodes = new ArrayList<AeNode>();
      nodes.add(first);

      var operator = tokenizer.peek();

      do {
        tokenizer.advance();

        nodes.add(parseParenExpressionOrAuthorization(tokenizer));

      } while (tokenizer.hasNext() && tokenizer.peek() == operator);

      if (tokenizer.hasNext() && (tokenizer.peek() == '|' || tokenizer.peek() == '&')) {
        // A case of mixed operators, lets give a clear error message
        tokenizer.error("Cannot mix '|' and '&'");
      }

      return AeNode.of(operator, nodes);
    } else {
      return first;
    }
  }

  private static AeNode parseParenExpressionOrAuthorization(Tokenizer tokenizer) {
    if (!tokenizer.hasNext()) {
      tokenizer
          .error("Expected a '(' character or an authorization token instead saw end of input");
    }

    if (tokenizer.peek() == '(') {
      tokenizer.advance();
      var node = parseExpression(tokenizer);
      tokenizer.next(')');
      return node;
    } else {
      return AeNode.of(tokenizer.nextAuthorization());
    }
  }
}
