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
import java.util.Iterator;
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
public final record Authorizations(Set<String> authorizations)
    implements Iterable<String>, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Authorizations EMPTY = new Authorizations(Set.of());

  public Authorizations(Set<String> authorizations) {
    this.authorizations = Set.copyOf(authorizations);
  }

  @Override
  public Iterator<String> iterator() {
    return authorizations.iterator();
  }

  public static Authorizations empty() {
    return EMPTY;
  }

}
