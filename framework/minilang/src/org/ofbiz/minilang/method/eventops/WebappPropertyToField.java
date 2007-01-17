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
package org.ofbiz.minilang.method.eventops;

import java.net.*;
import java.util.*;
import javax.servlet.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies a property value from a properties file in a ServletContext resource to a field
 */
public class WebappPropertyToField extends MethodOperation {
    
    public static final String module = WebappPropertyToField.class.getName();
    
    String resource;
    String property;
    String defaultVal;
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;

    public WebappPropertyToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        resource = element.getAttribute("resource");
        property = element.getAttribute("property");
        defaultVal = element.getAttribute("default");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
    }

    public boolean exec(MethodContext methodContext) {
        String resource = methodContext.expandString(this.resource);
        String property = methodContext.expandString(this.property);
        String defaultVal = methodContext.expandString(this.defaultVal);
                
        String fieldVal = null;

        // only run this if it is in an EVENT context
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            ServletContext servletContext = (ServletContext) methodContext.getRequest().getAttribute("servletContext");
            URL propsUrl = null;

            try {
                propsUrl = servletContext.getResource(resource);
            } catch (java.net.MalformedURLException e) {
                Debug.logWarning(e, "Error finding webapp resource (properties file) not found with name " + resource, module);
            }

            if (propsUrl == null) {
                Debug.logWarning("Webapp resource (properties file) not found with name " + resource, module);
            } else {
                fieldVal = UtilProperties.getPropertyValue(propsUrl, property);
                if (fieldVal == null || fieldVal.length() == 0) {
                    Debug.logWarning("Webapp resource property value not found with name " + property + " in resource " + resource, module);
                }
            }
        }

        // if fieldVal is null, or has zero length, use defaultVal
        if ((fieldVal == null) || (fieldVal.length() == 0)){
            fieldVal = defaultVal;
        }

        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);

            if (fromMap == null) {
                Debug.logWarning("Map not found with name " + mapAcsr + " creating a new map", module);
                fromMap = new HashMap();
                mapAcsr.put(methodContext, fromMap);
            }

            fieldAcsr.put(fromMap, fieldVal, methodContext);
        } else {
            fieldAcsr.put(methodContext, fieldVal);
        }
        return true;
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<webapp-property-to-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
