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

/**
 * An exception that is thrown when an authorization is not valid.
 *
 * @since 1.0.0
 */
public class InvalidAuthorizationException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String auth;
  private final String reason;

  public static InvalidAuthorizationException emptyString() {
    return new InvalidAuthorizationException("", "empty string");
  }

  public static InvalidAuthorizationException unablancedQuotes(CharSequence auth) {
    return new InvalidAuthorizationException(auth, "unbalanced quotes");
  }

  public static InvalidAuthorizationException invalidChars(CharSequence auth) {
    return new InvalidAuthorizationException(auth, "invalid characters");
  }

  public static InvalidAuthorizationException illegalEscape(CharSequence auth) {
    return new InvalidAuthorizationException(auth, "invalid escape sequence");
  }

  public static InvalidAuthorizationException unescapedQuote(CharSequence auth) {
    return new InvalidAuthorizationException(auth, "unescaped quote");
  }

  private InvalidAuthorizationException(CharSequence auth, String reason) {
    this.auth = auth.toString();
    this.reason = reason;
  }

  @Override
  public String getMessage() {
    return "authorization : '" + auth + "' (" + reason + ")";
  }

}
