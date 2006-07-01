/*
 * $Id: PropertyOper.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang.operation;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * A MakeInStringOperation that insert the value of a property from a properties file
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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
