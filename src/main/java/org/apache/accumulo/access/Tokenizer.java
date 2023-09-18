package org.apache.accumulo.access;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Tokenizer {

    private static final boolean[] validAuthChars = new boolean[256];

    static {
        for (int i = 0; i < 256; i++) {
            validAuthChars[i] = false;
        }

        for (int i = 'a'; i <= 'z'; i++) {
            validAuthChars[i] = true;
        }

        for (int i = 'A'; i <= 'Z'; i++) {
            validAuthChars[i] = true;
        }

        for (int i = '0'; i <= '9'; i++) {
            validAuthChars[i] = true;
        }

        validAuthChars['_'] = true;
        validAuthChars['-'] = true;
        validAuthChars[':'] = true;
        validAuthChars['.'] = true;
        validAuthChars['/'] = true;
    }

    static boolean isValidAuthChar(byte b) {
        return validAuthChars[0xff & b];
    }

    private byte[] expression;
    private int index;

    private AuthorizationToken authorizationToken = new AuthorizationToken();

    static class AuthorizationToken {
        byte[] data;
        int start;
        int len;
    }


    Tokenizer(byte[] expression) {
        this.expression = expression;
        authorizationToken.data = expression;
    }


    boolean hasNext() {
        return index < expression.length;
    }

    public void advance() {
        index++;
    }

    public void next(char expected) {
        if(!hasNext()) {
            error("expected "+expected+" but expression is at end");
        }

        if(expression[index] != expected) {
            error("expected "+expected+" but saw "+(char)(expression[index]));
        }
        index++;
    }

    public void error(String msg) {
        error(msg, index);
    }

    public void error(String msg, int idx) {
        throw new IllegalAccessExpressionException(msg, new String(expression, UTF_8), idx);
    }

    byte peek(){
        return expression[index];
    }

    AuthorizationToken nextAuthorization(){
            if(expression[index] == '"') {
                int start = ++index;

                while (index < expression.length && expression[index] != '"') {
                    if (expression[index] == '\\') {
                        index++;
                        if (index == expression.length
                                || (expression[index] != '\\' && expression[index] != '"')) {
                            error("invalid escaping within quotes", index -1);
                        }
                    }
                    index++;
                }

                if (index == expression.length) {
                    error("unclosed quote", start - 1);
                }

                if (start == index) {
                    error("empty authorization token in quotes", start - 1);
                }

                authorizationToken.start = start;
                authorizationToken.len = index - start;

                index++;

                return authorizationToken;

            } else if (isValidAuthChar(expression[index])) {
                int start = index;
                while (index < expression.length && isValidAuthChar(expression[index])) {
                    index++;
                }
                authorizationToken.start = start;
                authorizationToken.len = index - start;
                return authorizationToken;
            } else {
                error("Expected an authorization");
                return null;
            }
    }

}
