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

import java.util.Arrays;
import java.util.Objects;

public final class CharsWrapper implements CharSequence {
  private char[] wrapped;
  private int offset;
  private int len;

  CharsWrapper(char[] wrapped) {
    this.wrapped = wrapped;
    this.offset = 0;
    this.len = this.wrapped.length;
  }

  private CharsWrapper(char[] wrapped, int offset, int len) {
    this.wrapped = wrapped;
    this.offset = offset;
    this.len = len;
  }

  @Override
  public int length() {
    return len;
  }

  @Override
  public char charAt(int index) {
    Objects.checkIndex(index, len);
    return wrapped[offset + index];
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    Objects.checkFromToIndex(start, end, len);
    return new CharsWrapper(wrapped, start + offset, end - start);
  }

  @Override
  public int hashCode() {
    int hash = 1;

    int end = offset + length();
    for (int i = offset; i < end; i++) {
      hash = (31 * hash) + wrapped[i];
    }

    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof CharsWrapper) {
      CharsWrapper obs = (CharsWrapper) o;

      if (this == o) {
        return true;
      }

      if (length() != obs.length()) {
        return false;
      }

      return Arrays.equals(wrapped, offset, offset + len, obs.wrapped, obs.offset,
          obs.offset + obs.len);
    }

    return false;
  }

  @Override
  public String toString() {
    return new String(wrapped, offset, len);
  }

  public void set(char[] data, int start, int len) {
    this.wrapped = data;
    this.offset = start;
    this.len = len;
  }
}
