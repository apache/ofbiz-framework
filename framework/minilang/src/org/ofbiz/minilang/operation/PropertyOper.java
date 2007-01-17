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
 * A MakeInStringOperation that insert the value of a property from a properties file
 */
public class PropertyOper extends MakeInStringOperation {
    
    public static final String module = PropertyOper.class.getName();
    
    String resource;
    String property;

    public PropertyOper(Element element) {
        super(element);
        resource = element.getAttribute("resource");
        property = element.getAttribute("property");
    }

    public String exec(Map inMap, List messages, Locale locale, ClassLoader loader) {
        String propStr = UtilProperties.getPropertyValue(UtilURL.fromResource(resource, loader), property);

        if (propStr == null || propStr.length() == 0) {
            Debug.logWarning("[SimpleMapProcessor.PropertyOper.exec] Property " + property + " in resource " + resource + " not found, not appending anything", module);
            return null;
        } else {
            return propStr;
        }
    }
}
