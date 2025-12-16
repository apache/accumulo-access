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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.InvalidAccessExpressionException;

public final class MultiAccessEvaluatorImpl implements AccessEvaluator {
  final List<AccessEvaluatorImpl> evaluators;

  public MultiAccessEvaluatorImpl(Collection<Authorizations> authorizationSets) {
    evaluators = new ArrayList<>(authorizationSets.size());
    for (Authorizations authorizations : authorizationSets) {
      evaluators.add(new AccessEvaluatorImpl(authorizations));
    }
  }

  @Override
  public boolean canAccess(String accessExpression) throws InvalidAccessExpressionException {
    return canAccess(accessExpression.getBytes(UTF_8));
  }

  @Override
  public boolean canAccess(byte[] accessExpression) throws InvalidAccessExpressionException {
    for (AccessEvaluatorImpl evaluator : evaluators) {
      if (!evaluator.canAccess(accessExpression)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean canAccess(AccessExpression accessExpression) {
    return canAccess(accessExpression.getExpression());
  }
}
