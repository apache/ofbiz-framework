/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.base.util.string;

import java.lang.reflect.Method;

/**
 * Small Uel utility class
 */
public final class UelMapping {

    /**
     * The key of this Uel, often composed by a domain and a name separated by a column
     */
    private final String myKey;

    /**
     * The method that is called by the Uel
     */
    private final Method myMethod;

    /**
     * The description of the Uel that will be displayed in the Uel screen
     */
    private final String myDescription;

    public UelMapping(String key, Method method) {
        myKey = key;
        myMethod = method;
        myDescription = "No description";
    }

    public UelMapping(String key, Method method, String description) {
        myKey = key;
        myMethod = method;
        myDescription = description;
    }

    public String getKey() {
        return myKey;
    }

    public Method getMethod() {
        return myMethod;
    }

    public String getDescription() {
        return myDescription;
    }

}
