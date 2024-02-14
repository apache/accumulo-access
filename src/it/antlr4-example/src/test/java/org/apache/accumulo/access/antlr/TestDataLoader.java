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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
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

    URL url = TestDataLoader.class.getClassLoader().getResource(".");
    File testClassesDir = new File(url.toURI());
    File accumuloAccessParentDir = testClassesDir.getParentFile().getParentFile().getParentFile()
        .getParentFile().getParentFile();
    File accumuloAccessSourceDir = new File(accumuloAccessParentDir, "src");
    assertTrue(accumuloAccessSourceDir.exists());
    File accumuloAccessTestDir = new File(accumuloAccessSourceDir, "test");
    assertTrue(accumuloAccessTestDir.exists());
    File accumuloAccessTestResourcesDir = new File(accumuloAccessTestDir, "resources");
    assertTrue(accumuloAccessTestResourcesDir.exists());
    File testDataFile = new File(accumuloAccessTestResourcesDir, "testdata.json");
    assertTrue(testDataFile.exists());

    try (FileInputStream input = new FileInputStream(testDataFile)) {
      var json = new String(input.readAllBytes(), UTF_8);

      Type listType = new TypeToken<ArrayList<TestDataSet>>() {}.getType();
      return new Gson().fromJson(json, listType);
    }
  }
}
