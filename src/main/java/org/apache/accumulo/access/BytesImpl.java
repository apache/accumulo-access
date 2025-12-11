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
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.checkIndex;

import java.util.Arrays;

//this class is intentionally package private and should never be made public
final class BytesImpl implements Bytes {

  private static final long serialVersionUID = 1L;
  private byte[] data;
  private int offset;
  private int length;

  /**
   * Creates a new sequence. The given byte array is used directly as the backing array, so later
   * changes made to the array reflect into the new sequence.
   *
   * @param data byte data
   */
  BytesImpl(byte[] data) {
    set(data, 0, data.length);
  }

  public byte[] get() {
    return Arrays.copyOfRange(data, offset, length);
  }

  public byte byteAt(int i) {
    return data[offset + checkIndex(i, length)];
  }

  public int length() {
    return length;
  }

  @Override
  public int compareTo(Bytes obs) {
    if (obs instanceof BytesImpl) {
      BytesImpl bw = (BytesImpl) obs;
      return Arrays.compare(data, offset, offset + length(), bw.data, bw.offset,
          bw.offset + bw.length());
    } else {
      return Bytes.super.compareTo(obs);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BytesImpl) {
      BytesImpl obs = (BytesImpl) o;

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
   * a copy of the input buffer. This method is used internally as an optimization so that different
   * instances of BytesImpl can reference the same underlying byte[] at different offsets.
   */
  void set(byte[] data, int offset, int length) {
    checkFromIndexSize(offset, length, data.length);
    this.data = data;
    this.offset = offset;
    this.length = length;
  }

}
