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

package example;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.accumulo.access.AccessEvaluator;

public class AccessExample {

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.printf("No authorizations provided. Running several examples.%n"
          + "Specify the authorizations on the command line to try a specific example.%n%n");

      run("BLUE", "GREEN", "PINK", "RED");
      run();
      run("BLUE", "RED");
      run("GREEN", "RED");
      run("PINK");
    } else {
      run(args);
    }
  }

  private static void run(String... authorizations) {
    System.out.printf("Showing accessible records using authorizations: %s%n",
        Arrays.toString(authorizations));

    // Create an access evaluator using the provided authorizations
    AccessEvaluator evaluator = AccessEvaluator.of(authorizations);

    // Print each record whose access expression permits viewing using the provided authorizations
    getData().forEach((record, accessExpression) -> {
      if (evaluator.canAccess(accessExpression)) {
        System.out.printf("  %s : %s%n", record, accessExpression);
      }
    });
    System.out.println();
  }

  // Create a simple example data set as a sorted map of records and their access expression
  private static TreeMap<String,String> getData() {
    // @formatter:off
    return new TreeMap<>(Map.of(
        "data1", "(RED&GREEN)|(BLUE&PINK)",
        "data2", "(RED&GREEN)|(BLUE&PINK)",
        "data3", "(RED|GREEN)&(BLUE|PINK)",
        "data4", "(RED&GREEN)|(BLUE&PINK)",
        "data5", "(RED|GREEN)&(BLUE|PINK)",
        "data6", "",
        "data7", "PINK",
        "data8", "RED&BLUE&GREEN&PINK",
        "data9", "PINK|(BLUE&RED)"));
    // @formatter:on
  }

}
