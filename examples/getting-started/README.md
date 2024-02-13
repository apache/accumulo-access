<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->

# Getting started with Accumulo Access

This standalone Accumulo Access example has the following components.

 * [AccessExample](src/main/java/gse/AccessExample.java) Example that shows how to evaluate if data is accessible.
 * [pom.xml](pom.xml) Maven build file that shows how to use Accumulo Access as a dependency
 * [run.sh](run.sh) Bash script that runs the example

To run this example clone the Accumulo Access repository and then do the following.

```bash
cd accumulo-access
# This step installs a snapshot version of the Accumulo Access library.
# This step will not longer be needed once Accumulo Access is released.
mvn install
cd examples/getting-started
# Build the example.  If you change the example java code, run this step again.
mvn clean package
# Run the example
./run.sh
```

## Example runs

Running with the authorizations set `{BLUE,GREEN,PINK,RED}` shows the complete data set because all data is accessible with these authorizations.

```
$ ./run.sh BLUE GREEN PINK RED
data1 : (RED&GREEN)|(BLUE&PINK)
data2 : (RED&GREEN)|(BLUE&PINK)
data3 : (RED|GREEN)&(BLUE|PINK)
data4 : (RED&GREEN)|(BLUE&PINK)
data5 : (RED|GREEN)&(BLUE|PINK)
data6 : 
data7 : PINK
data8 : RED&BLUE&GREEN&PINK
data9 : PINK|(BLUE&RED)
```

Running with the empty set of authorizations shows only `data6` which has an empty access expression and is always accessible with any authorization set.

```
$ ./run.sh 
data6 : 
```

Running with the authorizations set `{BLUE,RED}`

```
$ ./run.sh BLUE RED
data3 : (RED|GREEN)&(BLUE|PINK)
data5 : (RED|GREEN)&(BLUE|PINK)
data6 : 
data9 : PINK|(BLUE&RED)
```

Running with the authorizations set `{GREEN,RED}`

```
$ ./run.sh GREEN RED
data1 : (RED&GREEN)|(BLUE&PINK)
data2 : (RED&GREEN)|(BLUE&PINK)
data4 : (RED&GREEN)|(BLUE&PINK)
data6 : 
```

Running with the authorizations set `{PINK}`

```
$ ./run.sh PINK
data6 : 
data7 : PINK
data9 : PINK|(BLUE&RED)
```



