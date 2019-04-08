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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.w3c.dom.Element;

/**
 * A MakeInStringOperation that inserts the value of an in-field
 */
public class InFieldOper extends MakeInStringOperation {

    public static final String module = InFieldOper.class.getName();

    String fieldName;

    public InFieldOper(Element element) {
        super(element);
        fieldName = element.getAttribute("field");
    }

    @Override
    public String exec(Map<String, Object> inMap, List<Object> messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(fieldName);
        if (obj == null) {
            Debug.logWarning("[SimpleMapProcessor.InFieldOper.exec] In field " + fieldName + " not found, not appending anything", module);
            return null;
        }
        try {
            return (String) ObjectType.simpleTypeConvert(obj, "String", null, locale);
        } catch (GeneralException e) {
            Debug.logWarning(e, module);
            messages.add("Error converting incoming field \"" + fieldName + "\" in map processor: " + e.getMessage());
            return null;
        }
    }
}
