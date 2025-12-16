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
package org.apache.accumulo.access.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import org.apache.accumulo.access.Authorizations;
import org.junit.jupiter.api.Test;

public class AuthorizationTest {

  @Test
  public void testEquality() {
    Authorizations auths1 = new Authorizations(Set.of("A", "B", "C"));
    Authorizations auths2 = new Authorizations(Set.of("A", "B", "C"));

    assertEquals(auths1, auths2);
    assertEquals(auths1.hashCode(), auths2.hashCode());

    Authorizations auths3 = new Authorizations(Set.of("D", "E", "F"));

    assertNotEquals(auths1, auths3);
    assertNotEquals(auths1.hashCode(), auths3.hashCode());
  }

  @Test
  public void testEmpty() {
    // check if new object is allocated
    assertSame(Authorizations.empty(), Authorizations.empty());
    assertEquals(Set.of(), Authorizations.empty().authorizations());
    assertSame(Set.of(), Authorizations.empty().authorizations());
  }
}
