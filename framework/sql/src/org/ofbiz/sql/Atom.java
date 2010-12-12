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
package org.ofbiz.sql;

import java.util.Collection;
import java.util.Map;

import org.ofbiz.base.lang.Appender;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;

public abstract class Atom implements Appender<StringBuilder> {
    public static boolean equalsHelper(Object l, Object r) {
        return UtilObject.equalsHelper(l, r);
    }

    public static <T extends Collection<I>, I> T checkEmpty(T col) {
        return UtilValidate.isEmpty(col) ? null : col;
    }

    public static <T extends Map<K, V>, K, V> T checkEmpty(T map) {
        return UtilValidate.isEmpty(map) ? null : map;
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }
}
