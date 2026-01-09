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

/**
 * This class exists to avoid repeat conversions from byte to char as well as to provide helper
 * methods for comparing them.
 */
final class CharUtils {
  static final char QUOTE = '"';
  static final char BACKSLASH = '\\';
  static final char AND_OPERATOR = '&';
  static final char OR_OPERATOR = '|';

  private CharUtils() {
    // private constructor to prevent instantiation
  }

  static boolean isQuoteSymbol(char b) {
    return b == QUOTE;
  }

  static boolean isBackslashSymbol(char b) {
    return b == BACKSLASH;
  }

  static boolean isQuoteOrSlash(char b) {
    return isQuoteSymbol(b) || isBackslashSymbol(b);
  }

  static boolean isAndOperator(char b) {
    return b == AND_OPERATOR;
  }

  static boolean isOrOperator(char b) {
    return b == OR_OPERATOR;
  }

  static boolean isAndOrOperator(char b) {
    return isAndOperator(b) || isOrOperator(b);
  }
}
