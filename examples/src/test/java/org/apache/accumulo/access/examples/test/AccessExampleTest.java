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
package org.apache.accumulo.access.examples.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.accumulo.access.examples.AccessExample;
import org.junit.jupiter.api.Test;

public class AccessExampleTest {
  @Test
  public void testExampleCode() throws IOException {
    try (final var baos = new ByteArrayOutputStream();
        final var out = new PrintStream(baos, false, UTF_8)) {
      final var example = new AccessExample();
      example.run(out, "RED", "BLUE");
      var output = baos.toString(UTF_8);
      for (var expected : List.of("data3", "data5", "data6", "data9")) {
        assertTrue(output.contains(expected + " : "));
      }
      for (var unexpected : List.of("data1", "data4", "data7", "data8")) {
        assertFalse(output.contains(unexpected));
      }
    }
  }
}
