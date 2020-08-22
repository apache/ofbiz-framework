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
package org.apache.ofbiz.minilang.operation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilValidate;
import org.w3c.dom.Element;

/**
 * Checks to see if the current field is empty (null or zero length)
 */
public class NotEmpty extends SimpleMapOperation {

    public NotEmpty(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
    }

    @Override
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(getFieldName());
        if (obj instanceof java.lang.String) {
            String fieldValue = (java.lang.String) obj;
            if (UtilValidate.isEmpty(fieldValue)) {
                addMessage(messages, loader, locale);
            }
        } else {
            if (obj == null) {
                addMessage(messages, loader, locale);
            }
        }
    }
}
