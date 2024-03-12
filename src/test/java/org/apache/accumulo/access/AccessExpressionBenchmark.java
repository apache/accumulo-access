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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Benchmarks Access Expressions using JMH. To run, use the following commands.
 *
 * <p>
 * <blockquote>
 *
 * <pre>
 * mvn clean package
 * mvn exec:exec -Dexec.executable="java" -Dexec.classpathScope=test -Dexec.args="-classpath %classpath org.apache.accumulo.access.AccessExpressionBenchmark"
 * </pre>
 *
 * </blockquote>
 */
public class AccessExpressionBenchmark {

  public static class EvaluatorTests {
    AccessEvaluator evaluator;

    List<byte[]> expressions;
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    private ArrayList<byte[]> allTestExpressions;

    private ArrayList<String> allTestExpressionsStr;

    private ArrayList<EvaluatorTests> evaluatorTests;

    @SuppressFBWarnings(value = {"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"},
        justification = "Field is written by Gson")
    @Setup
    public void loadData() throws IOException {
      List<AccessEvaluatorTest.TestDataSet> testData = AccessEvaluatorTest.readTestData();
      allTestExpressions = new ArrayList<>();
      allTestExpressionsStr = new ArrayList<>();
      evaluatorTests = new ArrayList<>();

      for (var testDataSet : testData) {
        EvaluatorTests et = new EvaluatorTests();
        et.expressions = new ArrayList<>();

        if (testDataSet.auths.length == 1) {
          et.evaluator = AccessEvaluator.of(Authorizations.of(Set.of(testDataSet.auths[0])));
        } else {
          var authSets = Stream.of(testDataSet.auths).map(a -> Authorizations.of(Set.of(a)))
              .collect(Collectors.toList());
          et.evaluator = AccessEvaluator.of(authSets);
        }

        for (var tests : testDataSet.tests) {
          if (tests.expectedResult != AccessEvaluatorTest.ExpectedResult.ERROR) {
            for (var exp : tests.expressions) {
              allTestExpressionsStr.add(exp);
              byte[] byteExp = exp.getBytes(UTF_8);
              allTestExpressions.add(byteExp);
              et.expressions.add(byteExp);
            }
          }
        }

        evaluatorTests.add(et);
      }
    }

    List<byte[]> getBytesExpressions() {
      return allTestExpressions;
    }

    List<String> getStringExpressions() {
      return allTestExpressionsStr;
    }

    List<EvaluatorTests> getEvaluatorTests() {
      return evaluatorTests;
    }

  }

  /**
   * Measures the time it takes to parse an expression stored in byte[] and produce a parse tree.
   */
  @Benchmark
  public void measureBytesValidation(BenchmarkState state, Blackhole blackhole) {
    for (byte[] accessExpression : state.getBytesExpressions()) {
      AccessExpression.validate(accessExpression);
    }
  }

  /**
   * Measures the time it takes to parse an expression stored in a String and produce a parse tree.
   */
  @Benchmark
  public void measureStringValidation(BenchmarkState state, Blackhole blackhole) {
    for (String accessExpression : state.getStringExpressions()) {
      AccessExpression.validate(accessExpression);
    }
  }

  /**
   * Measures the time it takes to parse and evaluate an expression. This has to create the parse
   * tree an operate on it.
   */
  @Benchmark
  public void measureEvaluation(BenchmarkState state, Blackhole blackhole) {
    for (EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
      for (byte[] expression : evaluatorTests.expressions) {
        blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
      }
    }
  }

  public static void main(String[] args) throws RunnerException, IOException {

    var state = new BenchmarkState();
    state.loadData();

    int numExpressions = state.getBytesExpressions().size();

    System.out.println("Number of Expressions: " + numExpressions);

    Options opt = new OptionsBuilder().include(AccessExpressionBenchmark.class.getSimpleName())
        .mode(Mode.Throughput).operationsPerInvocation(numExpressions)
        .timeUnit(TimeUnit.MICROSECONDS).warmupTime(TimeValue.seconds(5)).warmupIterations(3)
        .measurementIterations(4).forks(3).build();
    new Runner(opt).run();
  }
}
