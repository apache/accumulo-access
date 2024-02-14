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
grammar AccessExpression;

access_expression : EOF | expression EOF;
expression :     ( and_expression | or_expression | '(' expression ')' | access_token);
and_expression : ( access_token | '(' expression ')' ) (and_operator ( access_token | '(' expression ')' ) )+;
or_expression :  ( access_token | '(' expression ')' ) (or_operator ( access_token | '(' expression ')' ) )+;
access_token : ACCESS_TOKEN;
and_operator : '&';
or_operator : '|';


ACCESS_TOKEN : ( [A-Za-z] | [0-9] | '_' | '-' | '.' | ':' | '/' )+
               | '"' ( [\u0020-\u0021] | [\u0023-\u005B] | [\u005D-\u007E] | [\u0080-\uD7FF] | [\uE000-\u{10FFFF}] | ( '\\"' | '\\\\' ) )+ '"' ;
WS : [\r\t\b\u000C]+ -> skip;
