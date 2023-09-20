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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Benchmarks Access Expression evaluations using JMH.  To run, use the following commands.
 *
 * <p><blockquote><pre>
 * mvn clean package
 * mvn exec:exec -Dexec.executable="java" -Dexec.classpathScope=test -Dexec.args="-classpath %classpath org.apache.accumulo.access.AccessExpressionEvaluationBenchmark"
 * </code></blockquote>
 * </pre>
 */
public class AccessExpressionEvaluationBenchmark extends AccessExpressionBenchmarkBase {


    /**
     * Measures the time it takes to evaluate a previously parsed expression.
     */
    @Benchmark
    public void measureEvaluation(BenchmarkState state, Blackhole blackhole) {
        for(EvaluatorTests evaluatorTests : state.getEvaluatorTests()) {
            for(AccessExpression expression : evaluatorTests.parsedExpressions) {
                blackhole.consume(evaluatorTests.evaluator.canAccess(expression));
            }
        }
    }

    /**
     * Measures the time it takes to parse and evaluate an expression.  This has to create the parse tree an operate on it.
     */
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

        int numExpressions = state.getBytesExpressions().size();
        int numEvaluationTests = state.getEvaluatorTests().size();
        
        System.out.println("Number of Expressions: " + numExpressions);
        System.out.println("Number of Evaluation Tests: " + numEvaluationTests);

        Options opts = new OptionsBuilder()
            .include(AccessExpressionEvaluationBenchmark.class.getSimpleName())
            .mode(Mode.Throughput)
            .operationsPerInvocation(numExpressions * numEvaluationTests)
            .timeUnit(TimeUnit.MICROSECONDS)
            .warmupTime(TimeValue.seconds(5))
            .warmupIterations(3)
            .measurementTime(TimeValue.seconds(10))
            .measurementIterations(4)
            .forks(3)
            .build();

        new Runner(opts).run();
    }

}
