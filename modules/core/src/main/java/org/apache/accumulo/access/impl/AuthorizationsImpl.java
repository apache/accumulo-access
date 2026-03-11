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
package org.apache.accumulo.access.impl;

import java.io.Serial;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.accumulo.access.Authorizations;

public class AuthorizationsImpl implements Authorizations {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final HashSet<String> EMPTY_SET = new HashSet<>();

  static final Authorizations EMPTY = new AuthorizationsImpl(Set.of());

  private final HashSet<String> authorizations; // type must be serializable

  AuthorizationsImpl(Set<String> authorizations) {
    this.authorizations = authorizations.isEmpty() ? EMPTY_SET : new HashSet<>(authorizations);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AuthorizationsImpl) {
      var oa = (AuthorizationsImpl) o;
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
   * Returns the set of authorization strings in this Authorization object
   *
   * @return immutable set of authorization strings
   */
  @Override
  public Set<String> asSet() {
    if (this.authorizations == EMPTY_SET) {
      return Set.of();
    }
    return Set.copyOf(authorizations);
  }

  @Override
  public Iterator<String> iterator() {
    return authorizations.iterator();
  }
}
