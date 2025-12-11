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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An immutable collection of authorization strings.
 *
 * <p>
 * Instances of this class are thread-safe.
 *
 * <p>
 * Note: The underlying implementation uses byte arrays. Any methods that accept or return a String
 * explicitly uses UTF-8 for the conversion. If you are using non-UTF-8 characters, then use the
 * methods that use byte[].
 *
 * @since 1.0.0
 */
public final class Authorizations implements Iterable<Bytes>, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Authorizations EMPTY = new Authorizations(Set.of());

  private final Set<Bytes> authorizations;

  private Authorizations(final Set<Bytes> authorizations) {
    this.authorizations = authorizations;
  }

  /**
   * Returns the set of authorization strings in this Authorization object
   *
   * @return immutable set of authorization strings
   */
  public Set<Bytes> asSet() {
    return Set.copyOf(authorizations);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Authorizations) {
      var oa = (Authorizations) o;
      return authorizations.equals(oa.authorizations);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return authorizations.hashCode();
  }

  @Override
  public String toString() {
    return authorizations.toString();
  }

  /**
   * @return a pre-allocated empty Authorizations object
   */
  public static Authorizations of() {
    return EMPTY;
  }

  /**
   * Creates an Authorizations object from the set of input authorization byte[].
   *
   * @param authorizations collection of authorization byte[]
   * @return Authorizations object
   */
  public static Authorizations fromBytes(final Collection<byte[]> authorizations) {
    if (authorizations.isEmpty()) {
      return EMPTY;
    } else {
      final Set<Bytes> authBytes = new HashSet<>(authorizations.size());
      for (final byte[] auth : authorizations) {
        if (auth.length == 0) {
          throw new IllegalArgumentException("Empty authorization");
        }
        authBytes.add(new BytesImpl(AccessEvaluatorImpl.escape(auth, false)));
      }
      return new Authorizations(authBytes);
    }
  }

  /**
   * Creates an Authorizations object from the set of input authorization strings.
   *
   * @param authorizations set of authorization strings
   * @return Authorizations object
   */
  public static Authorizations of(final Set<String> authorizations) {
    if (authorizations.isEmpty()) {
      return EMPTY;
    } else {
      final Set<Bytes> authBytes = new HashSet<>(authorizations.size());
      for (final String auth : authorizations) {
        if (auth.length() == 0) {
          throw new IllegalArgumentException("Empty authorization");
        }
        authBytes
            .add(new BytesImpl(AccessEvaluatorImpl.escape(StringUtils.toByteArray(auth), false)));
      }
      return new Authorizations(authBytes);
    }
  }

  @Override
  public Iterator<Bytes> iterator() {
    return authorizations.iterator();
  }
}
