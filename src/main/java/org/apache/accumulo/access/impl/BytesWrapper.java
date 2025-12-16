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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.checkIndex;

import java.util.Arrays;

public final class BytesWrapper implements Comparable<BytesWrapper> {

  private byte[] data;
  private int offset;
  private int length;

  /**
   * Creates a new sequence. The given byte array is used directly as the backing array, so later
   * changes made to the array reflect into the new sequence.
   *
   * @param data byte data
   */
  public BytesWrapper(byte[] data) {
    set(data, 0, data.length);
  }

  public byte byteAt(int i) {
    return data[offset + checkIndex(i, length)];
  }

  public int length() {
    return length;
  }

  @Override
  public int compareTo(BytesWrapper obs) {
    return Arrays.compare(data, offset, offset + length(), obs.data, obs.offset,
        obs.offset + obs.length());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BytesWrapper) {
      BytesWrapper obs = (BytesWrapper) o;

      if (this == o) {
        return true;
      }

      if (length() != obs.length()) {
        return false;
      }

      return compareTo(obs) == 0;
    }

    return false;

  }

  @Override
  public int hashCode() {
    int hash = 1;

    int end = offset + length();
    for (int i = offset; i < end; i++) {
      hash = (31 * hash) + data[i];
    }

    return hash;
  }

  @Override
  public String toString() {
    return new String(data, offset, length, UTF_8);
  }

  /*
   * Wraps the given byte[] and captures the current offset and length. This method does *not* make
   * a copy of the input buffer
   */
  public void set(byte[] data, int offset, int length) {
    checkFromIndexSize(offset, length, data.length);
    this.data = data;
    this.offset = offset;
    this.length = length;
  }

}
