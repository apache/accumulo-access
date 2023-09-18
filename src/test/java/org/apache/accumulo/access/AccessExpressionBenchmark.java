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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AccessExpressionBenchmark {


    public static class EvaluatorTests {
        AccessEvaluator evaluator;
        List<AccessExpression> parsedExpressions;

        List<byte[]> expressions;
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private ArrayList<byte[]> expressions;

        private ArrayList<String> strExpressions;

        private ArrayList<EvaluatorTests> evaluatorTests;

        @Setup
        public void loadData() throws IOException {
            List<AccessEvaluatorTest.TestDataSet> testData = AccessEvaluatorTest.readTestData();
            expressions = new ArrayList<>();
            strExpressions = new ArrayList<>();
            evaluatorTests = new ArrayList<>();

            for(var testDataSet : testData) {
                EvaluatorTests et = new EvaluatorTests();
                et.parsedExpressions = new ArrayList<>();
                et.expressions = new ArrayList<>();

                if(testDataSet.auths.length == 1) {
                    et.evaluator = AccessEvaluator.builder().authorizations(testDataSet.auths[0]).build();
                } else {
                    var authSets =
                            Stream.of(testDataSet.auths).map(Authorizations::of).collect(Collectors.toList());
                    et.evaluator = AccessEvaluator.builder().authorizations(authSets).build();
                }

                for(var tests : testDataSet.tests) {
                    if(tests.expectedResult != AccessEvaluatorTest.ExpectedResult.ERROR) {
                        for(var exp : tests.expressions) {
                            strExpressions.add(exp);
                            byte[] byteExp = exp.getBytes(UTF_8);
                            expressions.add(byteExp);
                            et.expressions.add(byteExp);
                            et.parsedExpressions.add(AccessExpression.of(exp));
                        }
                    }
                }

                evaluatorTests.add(et);
            }
        }

        List<byte[]> getBytesExpressions(){
            return expressions;
        }

        List<String> getStringExpressions(){
            return strExpressions;
        }

        public ArrayList<EvaluatorTests> getEvaluatorTests() {
            return evaluatorTests;
        }

    }

    @Benchmark
    public void measureBytesParsing(BenchmarkState state, Blackhole blackhole) {
        for(byte[] accessExpression : state.getBytesExpressions()) {
            blackhole.consume(AccessExpression.of(accessExpression));
        }
    }

    @Benchmark
    public void measureStringParsing(BenchmarkState state, Blackhole blackhole) {
        for(String accessExpression : state.getStringExpressions()) {
            blackhole.consume(AccessExpression.of(accessExpression));
        }
    }

    @Benchmark
    public void measureEvaluation(BenchmarkState state, Blackhole blackhole) {
        for(EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
            for(AccessExpression expression : evaluatorTests.parsedExpressions) {
                blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
            }
        }
    }

    @Benchmark
    public void measureEvaluationAndParsing(BenchmarkState state, Blackhole blackhole) {
        for(EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
            for(byte[] expression : evaluatorTests.expressions) {
                blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
            }
        }
    }

    public static void main(String[] args) throws RunnerException, IOException {

        var state = new BenchmarkState();
        state.loadData();

        int numExpressions =state.getBytesExpressions().size();

        Options opt = new OptionsBuilder()
                .include(AccessExpressionBenchmark.class.getSimpleName())
                .operationsPerInvocation(numExpressions)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(5))
                .warmupIterations(3)
                .measurementIterations(4)
                .forks(3)
                .build();

        new Runner(opt).run();
    }

}
