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

/**
 * Copies a field in the in-map to the out-map
 */
public class Copy extends SimpleMapOperation {
    
    boolean replace = true;
    boolean setIfNull = true;
    String toField;

    public Copy(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        toField = element.getAttribute("to-field");
        if (this.toField == null || this.toField.length() == 0) {
            this.toField = this.fieldName;
        }

        // if anything but false it will be true
        replace = !"false".equals(element.getAttribute("replace"));
        // if anything but false it will be true
        setIfNull = !"false".equals(element.getAttribute("set-if-null"));
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {
        Object fieldValue = inMap.get(fieldName);

        if (fieldValue == null && !setIfNull)
            return;

        if (fieldValue instanceof java.lang.String) {
            if (((String) fieldValue).length() == 0) {
                if (setIfNull && (replace || !results.containsKey(toField))) {
                    results.put(toField, null);
                }
                return;
            }
        }

        if (replace) {
            results.put(toField, fieldValue);
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapProcessor.Copy.exec] Copied \"" + fieldValue + "\" to field \"" + toField + "\"", module);
        } else {
            if (results.containsKey(toField)) {// do nothing
            } else {
                results.put(toField, fieldValue);
                // if (Debug.infoOn()) Debug.logInfo("[SimpleMapProcessor.Copy.exec] Copied \"" + fieldValue + "\" to field \"" + toField + "\"", module);
            }
        }
    }
}
