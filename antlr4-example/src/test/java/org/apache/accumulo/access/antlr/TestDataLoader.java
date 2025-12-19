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
package org.apache.accumulo.access.antlr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TestDataLoader {

  public enum ExpectedResult {
    ACCESSIBLE, INACCESSIBLE, ERROR
  }

  public static class TestExpressions {
    public ExpectedResult expectedResult;
    public String[] expressions;
  }

  public static class TestDataSet {
    public String description;
    public String[][] auths;
    public List<TestExpressions> tests;
  }

  public static List<TestDataSet> readTestData() throws IOException, URISyntaxException {

    try (var input = TestDataLoader.class.getClassLoader().getResourceAsStream("testdata.json")) {
      if (input == null) {
        throw new IllegalStateException("could not find resource : testdata.json");
      }
      var json = new String(input.readAllBytes(), UTF_8);

      Type listType = new TypeToken<ArrayList<TestDataSet>>() {}.getType();
      return new Gson().fromJson(json, listType);
    }
  }
}
