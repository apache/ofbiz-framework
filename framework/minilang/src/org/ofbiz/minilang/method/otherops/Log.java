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
package org.ofbiz.minilang.method.otherops;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Calculates a result based on nested calcops.
 */
public class Log extends MethodOperation {
    
    public static final String module = Log.class.getName();

    String levelStr;
    String message;
    List methodStrings = null;
    
    public Log(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.message = element.getAttribute("message");
        this.levelStr = element.getAttribute("level");

        List methodStringElements = UtilXml.childElementList(element);
        if (methodStringElements.size() > 0) {
            methodStrings = new LinkedList();
            
            Iterator methodStringIter = methodStringElements.iterator();
            while (methodStringIter.hasNext()) {
                Element methodStringElement = (Element) methodStringIter.next();
                if ("string".equals(methodStringElement.getNodeName())) {
                    methodStrings.add(new StringString(methodStringElement, simpleMethod)); 
                } else if ("field".equals(methodStringElement.getNodeName())) {
                    methodStrings.add(new FieldString(methodStringElement, simpleMethod)); 
                } else {
                    //whoops, invalid tag here, print warning
                    Debug.logWarning("Found an unsupported tag under the log tag: " + methodStringElement.getNodeName() + "; ignoring", module);
                }
            }
        }
    }

    public boolean exec(MethodContext methodContext) {
        String levelStr = methodContext.expandString(this.levelStr);
        String message = methodContext.expandString(this.message);
        
        int level;
        Integer levelInt = Debug.getLevelFromString(levelStr);
        if (levelInt == null) {
            Debug.logWarning("Specified level [" + levelStr + "] was not valid, using INFO", module);
            level = Debug.INFO;
        } else {
            level = levelInt.intValue();
        }

        //bail out quick if the logging level isn't on, ie don't even create string
        if (!Debug.isOn(level)) {
            return true;
        }
        
        StringBuffer buf = new StringBuffer();
        
        if (message != null) buf.append(message);
        
        if (methodStrings != null) {
            Iterator methodStringsIter = methodStrings.iterator();
            while (methodStringsIter.hasNext()) {
                MethodString methodString = (MethodString) methodStringsIter.next();
                String strValue = methodString.getString(methodContext);
                if (strValue != null) buf.append(strValue);
            }
        }        

        Debug.log(level, null, buf.toString(), module);
        
        return true;
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<log level=\"" + this.levelStr + "\" message=\"" + this.message + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
