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
package org.apache.accumulo.access;

import static org.apache.accumulo.access.ByteUtils.AND_OPERATOR;
import static org.apache.accumulo.access.ByteUtils.OR_OPERATOR;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Contains the code for an Access Expression represented as a parse tree and all the operations on
 * a parse tree.
 */
abstract class AeNode implements Comparable<AeNode> {

  abstract boolean canAccess(Predicate<BytesWrapper> authorizedPredicate);

  abstract void getAuthorizations(Consumer<String> authConsumer);

  abstract void stringify(StringBuilder builder, boolean addParens);

  abstract AeNode normalize();

  abstract int ordinal();

  @Override
  public boolean equals(Object o) {
    return o instanceof AeNode && compareTo((AeNode) o) == 0;
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(AeNode o) {
    return ordinal() - o.ordinal();
  }

  private static class EmptyNode extends AeNode {
    @Override
    boolean canAccess(Predicate<BytesWrapper> authorizedPredicate) {
      return true;
    }

    @Override
    void getAuthorizations(Consumer<String> authConsumer) {}

    @Override
    void stringify(StringBuilder builder, boolean addParens) {

    }

    @Override
    AeNode normalize() {
      return this;
    }

    @Override
    public int compareTo(AeNode o) {
      return super.compareTo(o);
    }

    @Override
    int ordinal() {
      return 0;
    }
  }

  private static class AuthNode extends AeNode {
    private final BytesWrapper authInExpression;

    AuthNode(Tokenizer.AuthorizationToken auth) {
      authInExpression = new BytesWrapper(auth.data, auth.start, auth.len);
    }

    @Override
    boolean canAccess(Predicate<BytesWrapper> authorizedPredicate) {
      return authorizedPredicate.test(authInExpression);
    }

    @Override
    void getAuthorizations(Consumer<String> authConsumer) {
      authConsumer.accept(AccessEvaluatorImpl.unescape(authInExpression));
    }

    @Override
    void stringify(StringBuilder builder, boolean addParens) {
      boolean needsQuotes = false;
      for (int i = 0; i < authInExpression.length(); i++) {
        if (!Tokenizer.isValidAuthChar(authInExpression.byteAt(i))) {
          needsQuotes = true;
          break;
        }
      }

      if (needsQuotes) {
        builder.append('"');
        builder.append(authInExpression);
        builder.append('"');
      } else {
        builder.append(authInExpression);
      }

    }

    @Override
    AeNode normalize() {
      return this;
    }

    @Override
    public int compareTo(AeNode other) {
      int cmp = super.compareTo(other);
      if (cmp == 0) {
        cmp = authInExpression.compareTo(((AuthNode) other).authInExpression);
      }
      return cmp;
    }

    @Override
    int ordinal() {
      return 1;
    }
  }

  private static abstract class MultiNode extends AeNode {
    protected final List<AeNode> children;

    private MultiNode(List<AeNode> children) {
      this.children = children;
    }

    @Override
    void getAuthorizations(Consumer<String> authConsumer) {
      children.forEach(aeNode -> aeNode.getAuthorizations(authConsumer));
    }

    abstract char operator();

    @Override
    void stringify(StringBuilder builder, boolean addParens) {
      if (addParens) {
        builder.append("(");
      }

      var iter = children.iterator();
      iter.next().stringify(builder, true);
      iter.forEachRemaining(aeNode -> {
        builder.append(operator());
        aeNode.stringify(builder, true);
      });

      if (addParens) {
        builder.append(")");
      }
    }

    @Override
    public int compareTo(AeNode other) {
      int cmp = super.compareTo(other);
      if (cmp == 0) {
        cmp = children.size() - ((MultiNode) other).children.size();

        if (cmp == 0) {
          for (int i = 0; i < children.size(); i++) {
            cmp = children.get(i).compareTo(((MultiNode) other).children.get(i));
            if (cmp != 0) {
              break;
            }
          }
        }
      }
      return cmp;
    }
  }

  private static class AndNode extends MultiNode {

    private AndNode(List<AeNode> children) {
      super(children);
    }

    @Override
    char operator() {
      return AND_OPERATOR;
    }

    @Override
    boolean canAccess(Predicate<BytesWrapper> authorizedPredicate) {
      for (var child : children) {
        if (!child.canAccess(authorizedPredicate)) {
          return false;
        }
      }

      return true;
    }

    void flatten(TreeSet<AeNode> nodes) {
      for (var child : children) {
        if (child instanceof AndNode) {
          ((AndNode) child).flatten(nodes);
        } else {
          nodes.add(child.normalize());
        }
      }
    }

    @Override
    AeNode normalize() {
      var flattened = new TreeSet<AeNode>();
      flatten(flattened);
      if (flattened.size() == 1) {
        return flattened.iterator().next();
      } else {
        return new AndNode(List.copyOf(flattened));
      }
    }

    @Override
    int ordinal() {
      return 3;
    }
  }

  private static class OrNode extends MultiNode {

    private OrNode(List<AeNode> children) {
      super(children);
    }

    @Override
    char operator() {
      return OR_OPERATOR;
    }

    @Override
    boolean canAccess(Predicate<BytesWrapper> authorizedPredicate) {
      for (var child : children) {
        if (child.canAccess(authorizedPredicate)) {
          return true;
        }
      }

      return false;
    }

    void flatten(TreeSet<AeNode> nodes) {
      for (var child : children) {
        if (child instanceof OrNode) {
          ((OrNode) child).flatten(nodes);
        } else {
          nodes.add(child.normalize());
        }
      }
    }

    @Override
    AeNode normalize() {
      var flattened = new TreeSet<AeNode>();
      flatten(flattened);
      if (flattened.size() == 1) {
        return flattened.iterator().next();
      } else {
        return new OrNode(List.copyOf(flattened));
      }
    }

    @Override
    int ordinal() {
      return 2;
    }
  }

  static AeNode of() {
    return new EmptyNode();
  }

  static AeNode of(Tokenizer.AuthorizationToken auth) {
    return new AuthNode(auth);
  }

  static AeNode of(byte operator, List<AeNode> children) {
    switch (operator) {
      case AND_OPERATOR:
        return new AndNode(children);
      case OR_OPERATOR:
        return new OrNode(children);
      default:
        throw new IllegalArgumentException();
    }
  }
}
