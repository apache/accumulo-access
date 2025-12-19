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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.AccumuloAccess;

public class AccessExample {

  public static void main(String[] args) {
    PrintStream out = System.out;
    var example = new AccessExample(out);
    if (args.length == 0) {
      out.printf("No authorizations provided. Running several examples.%n"
          + "Specify authorizations on the command line (separated by spaces) to try a specific example.%n%n");

      example.run("BLUE", "GREEN", "PINK", "RED");
      example.run();
      example.run("BLUE", "RED");
      example.run("GREEN", "RED");
      example.run("PINK");
    } else {
      example.run(args);
    }
  }

  private final PrintStream out;

  AccessExample(PrintStream out) {
    this.out = out;
  }

  void run(String... authorizations) {
    out.printf("Showing accessible records using authorizations: %s%n",
        Arrays.toString(authorizations));

    var accumuloAccess = AccumuloAccess.builder().authorizationValidator(auth -> {
      for (int i = 0; i < auth.length(); i++) {
        var c = auth.charAt(i);
        if (Character.isISOControl(c) || Character.isWhitespace(c) || !Character.isDefined(c)
            || c == '\uFFFD') {
          return false;
        }
      }
      return true;
    }).build();

    // Create an access evaluator using the provided authorizations
    AccessEvaluator evaluator =
        accumuloAccess.newEvaluator(accumuloAccess.newAuthorizations(Set.of(authorizations)));

    // Print each record whose access expression permits viewing using the provided authorizations
    getData().forEach((record, accessExpression) -> {
      if (evaluator.canAccess(accessExpression)) {
        out.printf("  %s : %s%n", record, accessExpression);
      }
    });
    out.println();
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
