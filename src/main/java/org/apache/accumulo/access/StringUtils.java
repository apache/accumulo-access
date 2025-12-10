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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for String to byte[] conversion and vice-versa
 */
public class StringUtils {

  public static byte[] toByteArray(String s) {
    return s.getBytes(UTF_8);
  }

  public static String toString(byte[] b) {
    return new String(b, UTF_8);
  }

  public static Set<String> convertArray(byte[]... arrays) {
    final Set<String> result = new HashSet<>(arrays.length);
    for (byte[] b : arrays) {
      result.add(new String(b, UTF_8));
    }
    return result;
  }

  public static Set<byte[]> convertArray(String... strings) {
    final Set<byte[]> result = new HashSet<>(strings.length);
    for (String s : strings) {
      result.add(s.getBytes(UTF_8));
    }
    return result;
  }

  public static Set<String> convertByteArraySet(Set<byte[]> arrays) {
    final Set<String> result = new HashSet<>(arrays.size());
    arrays.forEach(b -> result.add(new String(b, UTF_8)));
    return result;
  }

  public static Set<byte[]> convertStringSet(Set<String> strings) {
    final Set<byte[]> result = new HashSet<>(strings.size());
    strings.forEach(s -> result.add(s.getBytes(UTF_8)));
    return result;
  }

}
