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

import java.util.HashMap;
import java.util.Map;

public abstract class Recorder<I> {
    public Map<I, Integer> counts = new HashMap<I, Integer>();

    protected void record(I key) {
        Integer count = counts.get(key);
        if (count == null) {
            counts.put(key, Integer.valueOf(1));
        } else {
            counts.put(key, Integer.valueOf(count.intValue() + 1));
        }
    }
}
