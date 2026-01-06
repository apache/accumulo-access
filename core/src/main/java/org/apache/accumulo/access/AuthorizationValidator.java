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

import java.util.function.BiPredicate;

/**
 * Implementations validate authorizations for Accumulo Access. Creating implementations that are
 * stricter for a given domain can help avoid expressions that contain unexpected and unused
 * authorizations.
 *
 * <p>
 * When an authorization is quoted and/or escaped in access expression that is undone before is
 * passed to this predicate. Conceptually it is like {@link AccumuloAccess#unquote(String)} is
 * called prior to being passed to this predicate. If the authorization was quoted that information
 * is passed along is it may be useful for optimizations.
 *
 * <p>
 * A CharSequence is passed to this predicate for efficiency. It allows having a view into the
 * larger expression at parse time without any memory allocations. It is not safe to keep a
 * reference to the passed in char sequence as it is only stable while the predicate is called. If a
 * reference needs to be kept for some side effect, then call {@code toString()} to allocate a copy.
 * Avoiding calls to {@code toString()} will result in faster parsing.
 * </p>
 *
 * @since 1.0.0
 */
public interface AuthorizationValidator
    extends BiPredicate<CharSequence,AuthorizationValidator.AuthorizationQuoting> {

  /**
   * @since 1.0.0
   */
  enum AuthorizationQuoting {
    /**
     * Denotes that an authorization seen in a valid access expression was quoted. This may mean the
     * expression has extra characters not seen in an unquoted authorization.
     */
    QUOTED,
    /**
     * Denotes that an authorization seen in a valid access expression was unquoted. This means the
     * expression only contains the characters allowed in an unquoted authorization.
     */
    UNQUOTED
  }

  /**
   * This is the default validator for Accumulo Access. It does the following check of characters in
   * an authorization.
   *
   * <pre>
   *     {@code
   *     AuthorizationValidator DEFAULT = (auth, quoting) -> {
   *       if(quoting == AuthorizationQuoting.UNQUOTED) {
   *         return true;
   *       }
   *       for (int i = 0; i < auth.length(); i++) {
   *         var c = auth.charAt(i);
   *         if (!Character.isDefined(auth.charAt(i)) || Character.isISOControl(c) || c == '\uFFFD') {
   *           return false;
   *         }
   *       }
   *       return true;
   *     }
   *     }
   * </pre>
   *
   * <p>
   * The character U+FFFD is the Unicode replacement character and standard java libraries will
   * insert this into strings when it has problem decoding UTF-8. Therefore, seeing this character
   * likely means a java string was derived from corrupt or invalid UTF-8 data. This is why its
   * considered invalid in an authorization by default.
   *
   * @see Character#isDefined(char)
   * @see Character#isISOControl(char)
   * @since 1.0.0
   */
  AuthorizationValidator DEFAULT = (auth, quoting) -> {
    if (quoting == AuthorizationQuoting.UNQUOTED) {
      // If an authorization is in a valid access expression and is unquoted then its already known
      // to only contain a small set of ASCII chars and no further validation is needed.
      return true;
    }

    for (int i = 0; i < auth.length(); i++) {
      var c = auth.charAt(i);
      if (!Character.isDefined(auth.charAt(i)) || Character.isISOControl(c) || c == '\uFFFD') {
        return false;
      }
    }
    return true;
  };
}
