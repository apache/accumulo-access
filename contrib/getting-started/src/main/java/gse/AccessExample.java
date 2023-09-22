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
package gse;

import org.apache.accumulo.access.AccessEvaluator;

import java.util.ArrayList;
import java.util.Collection;

public class AccessExample {

    public static void main(String[] args) {
        // Create an access evaluator using the all the arguments passed in on the command line as authorizations.
        AccessEvaluator evaluator = AccessEvaluator.builder().authorizations(args).build();

        // For each record use the access evaluator to determine if it can be accessed using the authorizations from
        // the command line and the access expression associated with each record.
        for (Record record : getData()) {
            if (evaluator.canAccess(record.accessExpression)) {
                System.out.printf("%s : %s\n", record.data, record.accessExpression);
            }
        }
    }

    public static Collection<Record> getData() {
        Collection<Record> records = new ArrayList<>();

        records.add(new Record("data1", "(RED&GREEN)|(BLUE&PINK)"));
        records.add(new Record("data2", "(RED&GREEN)|(BLUE&PINK)"));
        records.add(new Record("data3", "(RED|GREEN)&(BLUE|PINK)"));
        records.add(new Record("data4", "(RED&GREEN)|(BLUE&PINK)"));
        records.add(new Record("data5", "(RED|GREEN)&(BLUE|PINK)"));
        records.add(new Record("data6", ""));
        records.add(new Record("data7", "PINK"));
        records.add(new Record("data8", "RED&BLUE&GREEN&PINK"));
        records.add(new Record("data9", "PINK|(BLUE&RED)"));

        return records;
    }

    public static class Record {
        public final String data;
        public final String accessExpression;

        public Record(String data, String accessExpression) {
            this.data = data;
            this.accessExpression = accessExpression;
        }
    }
}
