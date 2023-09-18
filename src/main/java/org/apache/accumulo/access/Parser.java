package org.apache.accumulo.access;

import java.util.ArrayList;

/**
 * Code for parsing an access expression and creating a parse tree of type {@link AeNode}
 */
final class Parser {
    public static AeNode parseAccessExpression(byte[] expression) {

        Tokenizer tokenizer = new Tokenizer(expression);

        if(!tokenizer.hasNext()) {
            return AeNode.of();
        }

        var node = parseExpression(tokenizer);

        if(tokenizer.hasNext()){
            //not all input was read, so not a valid expression
            tokenizer.error("Unconsumed token "+(char)tokenizer.peek());
        }

        return node;
    }

    private static AeNode parseExpression(Tokenizer tokenizer) {
        if(!tokenizer.hasNext()) {
            tokenizer.error("illegal empty expression ");
        }

        AeNode first;

        if(tokenizer.peek() == '(') {
            first = parseParenExpression(tokenizer);
        } else {
            first = AeNode.of(tokenizer.nextAuthorization());
        }

        if(tokenizer.hasNext() && (tokenizer.peek() == '&' || tokenizer.peek() == '|')) {
            var nodes = new ArrayList<AeNode>();
            nodes.add(first);

            var operator = tokenizer.peek();

            do {
                tokenizer.advance();

                if(!tokenizer.hasNext()) {
                    tokenizer.error("nothing following a "+(char)operator+" operator ");
                }

                if(tokenizer.peek() == '(') {
                    nodes.add(parseParenExpression(tokenizer));
                } else {
                    nodes.add(AeNode.of(tokenizer.nextAuthorization()));
                }
            } while(tokenizer.hasNext() && tokenizer.peek() == operator);

            if(tokenizer.hasNext() && (tokenizer.peek() == '|' || tokenizer.peek() == '&')) {
                // A case of mixed operators, lets give a clear error message
                tokenizer.error("cannot mix | and &");
            }


            return AeNode.of(operator, nodes);
        } else {
            return first;
        }
    }

    private static AeNode parseParenExpression(Tokenizer tokenizer) {
        tokenizer.advance();
        var node = parseExpression(tokenizer);
        tokenizer.next( ')');
        return node;
    }
}
