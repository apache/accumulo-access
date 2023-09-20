package org.apache.accumulo.access;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public abstract class AccessExpressionBenchmarkBase {

  public static class EvaluatorTests {
    AccessEvaluator evaluator;
    List<AccessExpression> parsedExpressions;

    List<byte[]> expressions;
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    private ArrayList<byte[]> allTestExpressions;

    private ArrayList<String> allTestExpressionsStr;

    private ArrayList<EvaluatorTests> evaluatorTests;

    @Setup
    public void loadData() throws IOException {
      List<AccessEvaluatorTest.TestDataSet> testData = AccessEvaluatorTest.readTestData();
      allTestExpressions = new ArrayList<>();
      allTestExpressionsStr = new ArrayList<>();
      evaluatorTests = new ArrayList<>();

      for (var testDataSet : testData) {
        EvaluatorTests et = new EvaluatorTests();
        et.parsedExpressions = new ArrayList<>();
        et.expressions = new ArrayList<>();

        if (testDataSet.auths.length == 1) {
          et.evaluator = AccessEvaluator.builder().authorizations(testDataSet.auths[0]).build();
        } else {
          var authSets =
              Stream.of(testDataSet.auths).map(Authorizations::of).collect(Collectors.toList());
          et.evaluator = AccessEvaluator.builder().authorizations(authSets).build();
        }

        for (var tests : testDataSet.tests) {
          if (tests.expectedResult != AccessEvaluatorTest.ExpectedResult.ERROR) {
            for (var exp : tests.expressions) {
              allTestExpressionsStr.add(exp);
              byte[] byteExp = exp.getBytes(UTF_8);
              allTestExpressions.add(byteExp);
              et.expressions.add(byteExp);
              et.parsedExpressions.add(AccessExpression.of(exp));
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

}
