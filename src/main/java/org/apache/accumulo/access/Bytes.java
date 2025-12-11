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
import java.util.Arrays;

/**
 * Wrapper object for a byte[] that allows for using as a key in a Map. Implementations MUST
 * override {@link Object#equals(Object)} and {@link Object#hashCode()}. Implementations may
 * override {@link Bytes#compareTo(Object)}.
 *
 * @since 1.0.0
 */
public interface Bytes extends Comparable<Bytes>, Serializable {

  /**
   * Returns a copy the underlying byte[] to ensure immutability
   *
   * @return copy of underlying byte[]
   */
  byte[] get();

  /**
   * Returns byte at the offset in the underlying byte[]
   *
   * @param i offset
   * @return byte at offset
   */
  byte byteAt(int i);

  /**
   * Returns length of underlying byte[]
   *
   * @return length of byte[]
   */
  int length();

  /**
   * @see Comparable#compareTo(Object)
   */
  @Override
  default int compareTo(Bytes other) {
    return Arrays.compare(get(), other.get());
  }

  /**
   * Create a new Bytes object using the supplied byte[]
   *
   * @param bytes byte[] used as the underlying array
   * @return Bytes object
   */
  public static Bytes of(byte[] bytes) {
    return new BytesImpl(bytes);
  }

}
