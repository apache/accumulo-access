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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * An immutable collection of authorization strings.
 *
 * <p>
 * Instances of this class are thread-safe.
 *
 * <p>
 * Note: The underlying implementation uses UTF-8 when converting between bytes and Strings.
 *
 * @since 1.0.0
 */
public final class Authorizations {
  private final Set<String> authorizations;

  private Authorizations(Set<String> authorizations) {
    this.authorizations = Set.copyOf(authorizations);
  }

  /**
   * Returns the set of authorization strings in this Authorization object
   *
   * @return set of authorization strings
   */
  public Set<String> asSet() {
    return authorizations;
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
   * Creates an Authorizations object from the list of input authorization strings. Duplicate values
   * are removed from the input list
   *
   * @param failOnDuplicates throw an exception if there are duplicates in the input
   * @param authorizations list of authorization strings
   * @return Authorizations object
   * @throws IllegalArgumentException if failOnDuplicates is true and duplicates in the input
   */
  public static Authorizations of(boolean failOnDuplicates, String... authorizations) {
    return failOnDuplicates ? new Authorizations(Set.of(authorizations))
        : of(false, Arrays.asList(authorizations));
  }

  /**
   * Creates an Authorizations object from the collection of input authorization strings. Duplicate
   * values are removed from the input list
   *
   * @param failOnDuplicates throw an exception if there are duplicates in the input
   * @param authorizations list of authorization strings
   * @return Authorizations object
   * @throws IllegalArgumentException if failOnDuplicates is true and duplicates in the input
   */
  public static Authorizations of(boolean failOnDuplicates, Collection<String> authorizations) {
    Set<String> auths = Set.copyOf(authorizations);
    if (failOnDuplicates) {
      if (authorizations.size() != auths.size()) {
        throw new IllegalArgumentException("duplicate element found in input");
      }
    }
    return new Authorizations(auths);
  }

}
