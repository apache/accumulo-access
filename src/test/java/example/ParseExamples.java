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
package example;

import static org.apache.accumulo.access.AccessExpression.quote;
import static org.apache.accumulo.access.AccessExpression.unquote;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AND;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AUTHORIZATION;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.ByteUtils;
import org.apache.accumulo.access.BytesWrapper;
import org.apache.accumulo.access.ParsedAccessExpression;
import org.apache.accumulo.access.ParsedAccessExpression.ExpressionType;
import org.apache.accumulo.access.StringUtils;

/**
 * Examples of using the parse tree in {@link ParsedAccessExpression} to inspect and transform
 * access expressions.
 */
public class ParseExamples {

  /**
   * This example will replace authorizations in an access expression.
   */
  public static void replaceAuthorizations(ParsedAccessExpression parsed,
      ByteBuffer expressionBuilder, Map<BytesWrapper,byte[]> replacements) {
    if (parsed.getType() == AUTHORIZATION) {
      // If the term is quoted in the expression, the quotes will be preserved. Calling unquote()
      // will only unescape and unquote if the string is quoted, otherwise it returns the string as
      // is.
      byte[] auth = unquote(parsed.getExpression());
      // Must quote any authorization that needs it. Calling quote() will only quote and escape if
      // needed, otherwise it returns the string as is.
      expressionBuilder.put(quote(replacements.getOrDefault(new BytesWrapper(auth), auth)));
    } else {
      byte operator = parsed.getType() == AND ? ByteUtils.AND_OPERATOR : ByteUtils.OR_OPERATOR;
      byte sep = '\0';

      for (var childExpression : parsed.getChildren()) {
        if (sep != '\0') {
          expressionBuilder.put(sep);
        }
        if (childExpression.getType() == AUTHORIZATION) {
          replaceAuthorizations(childExpression, expressionBuilder, replacements);
        } else {
          expressionBuilder.put(ByteUtils.OPEN_PAREN);
          replaceAuthorizations(childExpression, expressionBuilder, replacements);
          expressionBuilder.put(ByteUtils.CLOSE_PAREN);
        }
        sep = operator;
      }
    }
  }

  /**
   * As part of normalizing access expression this class is used to sort and dedupe sub-expressions
   * in a tree set.
   */
  public static class NormalizedExpression implements Comparable<NormalizedExpression> {
    public final byte[] expression;
    public final ExpressionType type;

    NormalizedExpression(byte[] expression, ExpressionType type) {
      this.expression = expression;
      this.type = type;
    }

    // determines the sort order of different kinds of subexpressions.
    private static int typeOrder(ExpressionType type) {
      switch (type) {
        case AUTHORIZATION:
          return 1;
        case OR:
          return 2;
        case AND:
          return 3;
        default:
          throw new IllegalArgumentException("Unexpected type " + type);
      }
    }

    @Override
    public int compareTo(NormalizedExpression o) {
      // Changing this comparator would significantly change how expressions are normalized.
      int cmp = typeOrder(type) - typeOrder(o.type);
      if (cmp == 0) {
        if (type == AUTHORIZATION) {
          // sort based on the unquoted and unescaped form of the authorization
          cmp = Arrays.compare(unquote(expression), unquote(o.expression));
        } else {
          cmp = Arrays.compare(expression, o.expression);
        }

      }
      return cmp;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof NormalizedExpression) {
        return compareTo((NormalizedExpression) o) == 0;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(expression);
    }
  }

  /**
   * This method helps with the flattening aspect of normalization by recursing down as far as
   * possible the parse tree in the case when the expression type is the same. As long as the type
   * is the same in the sub expression, keep using the same tree set.
   */
  public static void flatten(ExpressionType parentType, ParsedAccessExpression parsed,
      TreeSet<NormalizedExpression> normalizedExpressions) {
    if (parsed.getType() == parentType) {
      for (var child : parsed.getChildren()) {
        flatten(parentType, child, normalizedExpressions);
      }
    } else {
      // The type changed, so start again on the subexpression.
      normalizedExpressions.add(normalize(parsed));
    }
  }

  /**
   * <p>
   * For a given access expression this example will deduplicate, sort, flatten, and remove unneeded
   * parentheses or quotes in the expressions. The following list gives examples of what each
   * normalization step does.
   *
   * <ul>
   * <li>As an example of flattening, the expression {@code A&(B&C)} flattens to {@code
   * A&B&C}.</li>
   * <li>As an example of sorting, the expression {@code (Z&Y)|(C&B)} sorts to {@code
   * (B&C)|(Y&Z)}</li>
   * <li>As an example of deduplication, the expression {@code X&Y&X} normalizes to {@code X&Y}</li>
   * <li>As an example of unneeded quotes, the expression {@code "ABC"&"XYZ"} normalizes to
   * {@code ABC&XYZ}</li>
   * <li>As an example of unneeded parentheses, the expression {@code (((ABC)|(XYZ)))} normalizes to
   * {@code ABC|XYZ}</li>
   * </ul>
   *
   * <p>
   * This algorithm attempts to have the same behavior as the one in the Accumulo 2.1
   * ColumnVisibility class. However the implementation is very different.
   * </p>
   */
  public static NormalizedExpression normalize(ParsedAccessExpression parsed) {
    if (parsed.getType() == AUTHORIZATION) {
      // If the authorization is quoted and it does not need to be quoted then the following two
      // lines will remove the unnecessary quoting.
      byte[] unquoted = AccessExpression.unquote(parsed.getExpression());
      byte[] quoted = AccessExpression.quote(unquoted);
      return new NormalizedExpression(quoted, parsed.getType());
    } else {
      // The tree set does the work of sorting and deduplicating sub expressions.
      TreeSet<NormalizedExpression> normalizedChildren = new TreeSet<>();
      for (var child : parsed.getChildren()) {
        flatten(parsed.getType(), child, normalizedChildren);
      }

      if (normalizedChildren.size() == 1) {
        return normalizedChildren.first();
      } else {
        byte operator = parsed.getType() == AND ? ByteUtils.AND_OPERATOR : ByteUtils.OR_OPERATOR;
        byte sep = '\0';

        int length = normalizedChildren.size() * 3;
        for (var child : normalizedChildren) {
          length += child.expression.length;
        }

        final byte[] builder = new byte[length];
        int index = 0;

        for (var child : normalizedChildren) {
          if (sep != '\0') {
            builder[index++] = sep;
          }
          if (child.type == AUTHORIZATION) {
            for (byte b : child.expression) {
              builder[index++] = b;
            }
          } else {
            builder[index++] = ByteUtils.OPEN_PAREN;
            for (byte b : child.expression) {
              builder[index++] = b;
            }
            builder[index++] = ByteUtils.CLOSE_PAREN;
          }
          sep = operator;
        }

        return new NormalizedExpression(Arrays.copyOfRange(builder, 0, index), parsed.getType());
      }
    }
  }

  /**
   * This example recursively prints a parse tree.
   */
  public static void walk(String indent, ParsedAccessExpression parsed) {
    System.out.println(indent + parsed + " type:" + parsed.getType());
    for (var child : parsed.getChildren()) {
      walk(indent + "  ", child);
    }
  }

  public static void main(String[] args) {

    String exp = "((RED&GREEN)|(PINK&BLUE))";
    var parsed = AccessExpression.parse(exp);

    System.out.printf("Operating on %s%n", parsed);

    System.out.printf("%n  Normalized to %s%n", normalize(parsed).expression);
    ByteBuffer expressionBuilder = ByteBuffer.allocateDirect(exp.length() * 2);
    replaceAuthorizations(parsed, expressionBuilder, Map
        .of(new BytesWrapper(StringUtils.toByteArray("GREEN")), StringUtils.toByteArray("GREY")));
    byte[] buf = new byte[expressionBuilder.position()];
    expressionBuilder.get(buf);
    System.out.printf("%n  Replaced GREEN with GREY : %s%n", StringUtils.toString(buf));
    System.out.println("\n  Walking :");
    walk("    ", parsed);
  }
}
