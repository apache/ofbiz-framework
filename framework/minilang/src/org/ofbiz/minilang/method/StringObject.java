/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.minilang.method;

import org.w3c.dom.*;

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;

/**
 * A type of MethodObject that represents a String constant value to be used as an Object
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class StringObject extends MethodObject {
    
    String value;
    String cdataValue;

    public StringObject(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        value = element.getAttribute("value");
        cdataValue = UtilXml.elementValue(element);
    }

    /** Get the name for the type of the object */
    public String getTypeName() {
        return "java.lang.String";
    }
    
    public Class getTypeClass(ClassLoader loader) {
        return java.lang.String.class;
    }
    
    public Object getObject(MethodContext methodContext) {
        String value = methodContext.expandString(this.value);
        String cdataValue = methodContext.expandString(this.cdataValue);
        
        boolean valueExists = UtilValidate.isNotEmpty(value);
        boolean cdataValueExists = UtilValidate.isNotEmpty(cdataValue);
        
        if (valueExists && cdataValueExists) {
            return value + cdataValue;
        } else {
            if (valueExists) {
                return value;
            } else {
                return cdataValue;
            }
        }
    }
}
