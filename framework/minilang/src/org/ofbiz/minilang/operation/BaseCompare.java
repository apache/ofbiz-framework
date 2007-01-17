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
package org.ofbiz.minilang.operation;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * Abstract class providing functionality for the compare SimpleMapOperations
 */
public abstract class BaseCompare extends SimpleMapOperation {
    String operator;
    String type;
    String format;

    public BaseCompare(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        this.operator = element.getAttribute("operator");
        this.type = element.getAttribute("type");
        this.format = element.getAttribute("format");

        /* -- Let ObjectType handle the default --
         if (this.format == null || this.format.length() == 0) {
         if ("Date".equals(type)) {
         this.format = "yyyy-MM-dd";
         } else if ("Time".equals(type)) {
         this.format = "HH:mm:ss";
         } else if ("Timestamp".equals(type)) {
         this.format = "yyyy-MM-dd HH:mm:ss";
         }
         }
         */
    }

    public void doCompare(Object value1, Object value2, List messages, Locale locale, ClassLoader loader, boolean value2InlineConstant) {
        Boolean success = BaseCompare.doRealCompare(value1, value2, this.operator, this.type, this.format, messages, locale, loader, value2InlineConstant);

        if (success != null && success.booleanValue() == false) {
            addMessage(messages, loader, locale);
        }
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {}

    public static Boolean doRealCompare(Object value1, Object value2, String operator, String type, String format,
        List messages, Locale locale, ClassLoader loader, boolean value2InlineConstant) {
        return ObjectType.doRealCompare(value1, value2, operator, type, format, messages, locale, loader, value2InlineConstant);
    }
}
