/*
 * $Id: BaseCompare.java 5462 2005-08-05 18:35:48Z jonesde $
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
 * Abstract class providing functionality for the compare SimpleMapOperations
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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

    public void doCompare(Object value1, Object value2, List messages, Locale locale, ClassLoader loader) {
        Boolean success = BaseCompare.doRealCompare(value1, value2, this.operator, this.type, this.format, messages, locale, loader);

        if (success != null && success.booleanValue() == false) {
            addMessage(messages, loader, locale);
        }
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {}

    public static Boolean doRealCompare(Object value1, Object value2, String operator, String type, String format,
        List messages, Locale locale, ClassLoader loader) {
        return ObjectType.doRealCompare(value1, value2, operator, type, format, messages, locale, loader);
    }
}
