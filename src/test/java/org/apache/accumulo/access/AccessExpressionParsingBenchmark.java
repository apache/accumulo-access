package org.apache.accumulo.access;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.access.AccessExpressionBenchmarkBase.BenchmarkState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Benchmarks Access Expression parsing using JMH.  To run, use the following commands.
 *
 * <p><blockquote><pre>
 * mvn clean package
 * mvn exec:exec -Dexec.executable="java" -Dexec.classpathScope=test -Dexec.args="-classpath %classpath org.apache.accumulo.access.AccessExpressionParsingBenchmark"
 * </code></blockquote>
 * </pre>
 */
public class AccessExpressionParsingBenchmark {
  
  /**
   * Measures the time it takes to parse an expression stored in byte[] and produce a parse tree.
   */
  @Benchmark
  public void measureParsingBytes(BenchmarkState state, Blackhole blackhole) {
      for(byte[] accessExpression : state.getBytesExpressions()) {
        blackhole.consume(AccessExpression.of(accessExpression));
      }
  }

  /**
   * Measures the time it takes to parse an expression stored in a String and produce a parse tree.
   */
  @Benchmark
  public void measureParsingString(BenchmarkState state, Blackhole blackhole) {
      for(String accessExpression : state.getStringExpressions()) {
        blackhole.consume(AccessExpression.of(accessExpression));
      }
  }

  public static void main(String[] args) throws RunnerException, IOException {

    var state = new BenchmarkState();
    state.loadData();

    int numExpressions = state.getBytesExpressions().size();
    System.out.println("Number of Expressions: " + numExpressions);

    Options opts = new OptionsBuilder()
            .include(AccessExpressionParsingBenchmark.class.getSimpleName())
            .mode(Mode.Throughput)
            .operationsPerInvocation(numExpressions)
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
