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
package org.apache.accumulo.access.benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.testdata.TestDataLoader;
import org.apache.accumulo.access.testdata.TestDataLoader.ExpectedResult;
import org.apache.accumulo.access.testdata.TestDataLoader.TestDataSet;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.VisibilityEvaluator;
import org.apache.accumulo.core.security.VisibilityParseException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.NoBenchmarksException;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(AccessExpressionBenchmark.class);

  public static class VisibilityEvaluatorTests {
    List<VisibilityEvaluator> evaluator;
    List<byte[]> expressions;
    List<ColumnVisibility> columnVisibilities;
  }

  public static class EvaluatorTests {
    AccessEvaluator evaluator;

    List<String> expressions;
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    private Access access;

    private ArrayList<byte[]> allTestExpressions;

    private ArrayList<String> allTestExpressionsStr;

    private ArrayList<EvaluatorTests> evaluatorTests;

    private ArrayList<VisibilityEvaluatorTests> visibilityEvaluatorTests;

    @Setup
    public void loadData() throws Exception {
      access = Access.builder().build();
      List<TestDataSet> testData = TestDataLoader.readTestData();
      allTestExpressions = new ArrayList<>();
      allTestExpressionsStr = new ArrayList<>();
      evaluatorTests = new ArrayList<>();
      visibilityEvaluatorTests = new ArrayList<>();

      for (var testDataSet : testData) {

        // Create old
        VisibilityEvaluatorTests vet = new VisibilityEvaluatorTests();
        vet.expressions = new ArrayList<>();
        vet.columnVisibilities = new ArrayList<>();

        if (testDataSet.getAuths().length == 1) {
          vet.evaluator = List.of(new VisibilityEvaluator(
              new org.apache.accumulo.core.security.Authorizations(testDataSet.getAuths()[0])));
        } else {
          List<VisibilityEvaluator> veList = new ArrayList<>();
          for (String[] auths : testDataSet.getAuths()) {
            veList.add(new VisibilityEvaluator(
                new org.apache.accumulo.core.security.Authorizations(auths)));
          }
          vet.evaluator = veList;
        }

        // Create new
        EvaluatorTests et = new EvaluatorTests();
        et.expressions = new ArrayList<>();

        if (testDataSet.getAuths().length == 1) {
          et.evaluator = access.newEvaluator(Set.of(testDataSet.getAuths()[0]));
        } else {
          var authSets =
              Stream.of(testDataSet.getAuths()).map(a -> Set.of(a)).collect(Collectors.toList());
          et.evaluator = access.newEvaluator(authSets);
        }

        for (var tests : testDataSet.getTests()) {
          if (tests.getExpectedResult() != ExpectedResult.ERROR) {
            for (var exp : tests.getExpressions()) {
              allTestExpressionsStr.add(exp);
              byte[] byteExp = exp.getBytes(UTF_8);
              allTestExpressions.add(byteExp);
              et.expressions.add(exp);
              vet.expressions.add(byteExp);
              vet.columnVisibilities.add(new ColumnVisibility(byteExp));
            }
          }
        }

        evaluatorTests.add(et);
        visibilityEvaluatorTests.add(vet);
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

    List<VisibilityEvaluatorTests> getVisibilityEvaluatorTests() {
      return visibilityEvaluatorTests;
    }

  }

  /**
   * Measures the time it takes to parse an expression stored in byte[] and validate it.
   */
  @Benchmark
  public void measureBytesValidation(BenchmarkState state, Blackhole blackhole) {
    var accumuloAccess = state.access;
    for (byte[] accessExpression : state.getBytesExpressions()) {
      accumuloAccess.validateExpression(new String(accessExpression, UTF_8));
    }
  }

  /**
   * Measures the time it takes to parse an expression stored in a String and validate it.
   */
  @Benchmark
  public void measureStringValidation(BenchmarkState state, Blackhole blackhole) {
    var accumuloAccess = state.access;
    for (String accessExpression : state.getStringExpressions()) {
      accumuloAccess.validateExpression(accessExpression);
    }
  }

  /**
   * Measures the time it takes to parse an expression stored in a String and produce a parse tree.
   *
   */
  @Benchmark
  public void measureCreateParseTree(BenchmarkState state, Blackhole blackhole) {
    var accumuloAccess = state.access;
    for (String accessExpression : state.getStringExpressions()) {
      blackhole.consume(accumuloAccess.newParsedExpression(accessExpression));
    }
  }

  /**
   * Measures the time it takes to evaluate an expression.
   */
  @Benchmark
  public void measureParseAndEvaluation(BenchmarkState state, Blackhole blackhole) {
    for (EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
      for (String expression : evaluatorTests.expressions) {
        blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
      }
    }
  }

  /**
   * Measures the time it takes to evaluate a legacy expression.
   *
   * @throws VisibilityParseException error parsing expression with legacy code
   */
  @Benchmark
  public void measureLegacyEvaluationOnly(BenchmarkState state, Blackhole blackhole)
      throws VisibilityParseException {
    for (VisibilityEvaluatorTests evaluatorTests : state.getVisibilityEvaluatorTests()) {
      for (ColumnVisibility expression : evaluatorTests.columnVisibilities) {
        for (VisibilityEvaluator ve : evaluatorTests.evaluator) {
          blackhole.consume(ve.evaluate(expression));
        }
      }
    }
  }

  /**
   * Measures the time it takes to parse and evaluate a legacy expression. This has to create the
   * parse tree an operate on it.
   *
   * @throws VisibilityParseException error parsing expression with legacy code
   */
  @Benchmark
  public void measureLegacyParseAndEvaluation(BenchmarkState state, Blackhole blackhole)
      throws VisibilityParseException {
    for (VisibilityEvaluatorTests evaluatorTests : state.getVisibilityEvaluatorTests()) {
      for (byte[] expression : evaluatorTests.expressions) {
        for (VisibilityEvaluator ve : evaluatorTests.evaluator) {
          blackhole.consume(ve.evaluate(new ColumnVisibility(expression)));
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {

    var state = new BenchmarkState();
    state.loadData();

    int numExpressions = state.getBytesExpressions().size();
    LOG.info("Number of Expressions: {}", numExpressions);

    var jfr = Boolean.parseBoolean(System.getenv().getOrDefault("ACCESS_BENCHMARK_JFR", "false"));
    var jfrDir = System.getenv().getOrDefault("ACCESS_BENCHMARK_JFR_DIR",
        System.getProperty("java.io.tmpdir"));
    LOG.info("Java Flight Recorder: {}", jfr ? "enabled (outputDir=" + jfrDir + ")" : "disabled");

    var include = System.getenv().getOrDefault("ACCESS_BENCHMARK", "true");
    if (include.equals("true")) {
      include = "";
    }
    LOG.info("Benchmark include pattern: {}", include);

    var builder = new OptionsBuilder().include(include).mode(Mode.Throughput)
        .operationsPerInvocation(numExpressions).timeUnit(TimeUnit.MICROSECONDS)
        .warmupTime(TimeValue.seconds(5)).warmupIterations(3).measurementIterations(4).forks(3);

    if (jfr) {
      builder.addProfiler(JavaFlightRecorderProfiler.class, "dir=" + jfrDir);
    }

    try {
      new Runner(builder.build()).run();
    } catch (NoBenchmarksException e) {
      LOG.warn("No matching benchmarks");
    }
  }
}
