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

public final class CharsWrapper implements CharSequence {
  private CharSequence wrapped;
  private int offset;
  private int len;

  CharsWrapper(CharSequence wrapped) {
    this.wrapped = wrapped;
    this.offset = 0;
    this.len = wrapped.length();
  }

  CharsWrapper(CharSequence wrapped, int offset, int len) {
    // TODO bounds check
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
    return wrapped.charAt(offset + index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    // TODO bounds check
    return new CharsWrapper(wrapped, start + offset, end - start);
  }

  @Override
  public int hashCode() {
    int hash = 1;

    int end = offset + length();
    for (int i = offset; i < end; i++) {
      hash = (31 * hash) + wrapped.charAt(i);
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

      int end = offset + len;
      for (int i1 = offset, i2 = obs.offset; i1 < end; i1++, i2++) {
        if (wrapped.charAt(i1) != obs.wrapped.charAt(i2)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  @Override
  public String toString() {
    char[] chars = new char[len];
    int end = offset + length();
    for (int i = offset; i < end; i++) {
      chars[i - offset] = wrapped.charAt(i);
    }
    return new String(chars);
  }

  public void set(CharSequence data, int start, int len) {
    // TODO bounds check
    this.wrapped = data;
    this.offset = start;
    this.len = len;
  }
}
