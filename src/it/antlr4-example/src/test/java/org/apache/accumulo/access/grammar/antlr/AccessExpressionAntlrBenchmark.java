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
package org.apache.accumulo.access.grammar.antlr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.accumulo.access.Authorizations;
import org.apache.accumulo.access.antlr.TestDataLoader;
import org.apache.accumulo.access.grammars.AccessExpressionParser.Access_expressionContext;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Benchmarks Access Expressions using JMH. To run, use the following commands.
 *
 * <p>
 * <blockquote>
 *
 * <pre>
 * mvn clean package
 * mvn exec:exec -Dexec.executable="java" -Dexec.classpathScope=test -Dexec.args="-classpath %classpath org.apache.accumulo.access.grammar.antlr.AccessExpressionAntlrBenchmark"
 * </code></blockquote>
 * </pre>
 */
public class AccessExpressionAntlrBenchmark {

  public static class EvaluatorTests {
    AccessExpressionAntlrEvaluator evaluator;

    List<Access_expressionContext> parsedExpressions;

    List<byte[]> expressions;
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    private ArrayList<byte[]> allTestExpressions;

    private ArrayList<String> allTestExpressionsStr;

    private ArrayList<EvaluatorTests> evaluatorTests;

    @Setup
    public void loadData() throws IOException, URISyntaxException {
      List<TestDataLoader.TestDataSet> testData = TestDataLoader.readTestData();
      allTestExpressions = new ArrayList<>();
      allTestExpressionsStr = new ArrayList<>();
      evaluatorTests = new ArrayList<>();

      for (var testDataSet : testData) {
        EvaluatorTests et = new EvaluatorTests();
        et.parsedExpressions = new ArrayList<>();
        et.expressions = new ArrayList<>();

        et.evaluator = new AccessExpressionAntlrEvaluator(
            Stream.of(testDataSet.auths).map(a -> Authorizations.of(Set.of(a))).collect(Collectors.toList()));

        for (var tests : testDataSet.tests) {
          if (tests.expectedResult != TestDataLoader.ExpectedResult.ERROR) {
            for (var exp : tests.expressions) {
              allTestExpressionsStr.add(exp);
              byte[] byteExp = exp.getBytes(UTF_8);
              allTestExpressions.add(byteExp);
              et.expressions.add(byteExp);
              et.parsedExpressions.add(AccessExpressionAntlrParser.parseAccessExpression(exp));
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

    public ArrayList<EvaluatorTests> getEvaluatorTests() {
      return evaluatorTests;
    }

  }

  /**
   * Measures the time it takes to parse an expression stored in byte[] and produce a parse tree.
   */
  @Benchmark
  public void measureBytesParsing(BenchmarkState state, Blackhole blackhole) {
    for (byte[] accessExpression : state.getBytesExpressions()) {
      blackhole.consume(AccessExpressionAntlrParser.parseAccessExpression(accessExpression));
    }
  }

  /**
   * Measures the time it takes to parse an expression stored in a String and produce a parse tree.
   */
  @Benchmark
  public void measureStringParsing(BenchmarkState state, Blackhole blackhole) {
    for (String accessExpression : state.getStringExpressions()) {
      blackhole.consume(AccessExpressionAntlrParser.parseAccessExpression(accessExpression));
    }
  }

  /**
   * Measures the time it takes to evaluate a previously parsed expression.
   */
  @Benchmark
  public void measureEvaluation(BenchmarkState state, Blackhole blackhole) {
    for (EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
      for (Access_expressionContext expression : evaluatorTests.parsedExpressions) {
        blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
      }
    }
  }

  /**
   * Measures the time it takes to parse and evaluate an expression. This has to create the parse
   * tree an operate on it.
   */
  @Benchmark
  public void measureEvaluationAndParsing(BenchmarkState state, Blackhole blackhole) {
    for (EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
      for (byte[] expression : evaluatorTests.expressions) {
        blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
      }
    }
  }

  public static void main(String[] args) throws Exception {

    var state = new BenchmarkState();
    state.loadData();

    int numExpressions = state.getBytesExpressions().size();

    System.out.println("Number of Expressions: " + numExpressions);

    Options opt = new OptionsBuilder().include(AccessExpressionAntlrBenchmark.class.getSimpleName())
        .mode(Mode.Throughput).operationsPerInvocation(numExpressions)
        .timeUnit(TimeUnit.MICROSECONDS).warmupTime(TimeValue.seconds(5)).warmupIterations(3)
        .measurementIterations(4).forks(3).build();

    new Runner(opt).run();
  }

}
