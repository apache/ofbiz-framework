/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.sql.test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import junit.framework.TestCase;

import org.ofbiz.sql.Parser;
import org.ofbiz.sql.SQLSelect;
import org.ofbiz.sql.SQLStatement;

public class SQLTest extends TestCase {
    public SQLTest(String name) {
        super(name);
    }

    public void testFoo() throws Exception {
        List statements = new Parser(getClass().getResourceAsStream("TestSelect.sql")).SQLFile();
        for (Object statement: statements) {
            System.err.println(statement);
        }
    }
}
