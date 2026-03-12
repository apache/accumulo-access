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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.accumulo.access.Authorizations;

public class AuthorizationsImpl implements Authorizations {

  private static final long serialVersionUID = 1L;

  static final Authorizations EMPTY = new AuthorizationsImpl(Set.of());

  private transient Set<String> authorizations;

  public AuthorizationsImpl() {}

  AuthorizationsImpl(Set<String> authorizations) {
    this.authorizations = Set.copyOf(authorizations);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AuthorizationsImpl oa) {
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
    return authorizations;
  }

  @Override
  public Iterator<String> iterator() {
    return authorizations.iterator();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.write(authorizations.size());
    for (String a : authorizations) {
      out.writeUTF(a);
    }
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int size = in.readInt();
    if (size == 0) {
      this.authorizations = Set.of();
    } else {
      Set<String> s = new HashSet<>(size);
      for (int i = 0; i < size; i++) {
        s.add(in.readUTF());
      }
      this.authorizations = Set.copyOf(s);
    }
  }
}
