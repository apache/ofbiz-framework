/*
 * $Id: ContextAccessor.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method;

import java.util.Map;

import org.ofbiz.base.util.collections.FlexibleMapAccessor;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class ContextAccessor {

    protected String name;
    protected FlexibleMapAccessor fma;
    protected boolean needsExpand;
    protected boolean empty;

    public ContextAccessor(String name) {
        init(name);
    }
    
    public ContextAccessor(String name, String defaultName) {
        if (name == null || name.length() == 0) {
            init(defaultName);
        } else {
            init(name);
        }
    }
    
    protected void init(String name) {
        this.name = name;
        if (name == null || name.length() == 0) {
            empty = true;
            needsExpand = false;
            fma = new FlexibleMapAccessor(name);
        } else {
            empty = false;
            int openPos = name.indexOf("${");
            if (openPos != -1 && name.indexOf("}", openPos) != -1) {
                fma = null;
                needsExpand = true;
            } else {
                fma = new FlexibleMapAccessor(name);
                needsExpand = false;
            }
        }
    }
    
    public boolean isEmpty() {
        return this.empty;
    }
    
    /** Based on name get from Map or from List in Map */
    public Object get(MethodContext methodContext) {
        if (this.needsExpand) {
            return methodContext.getEnv(name);
        } else {
            return methodContext.getEnv(fma);
        }
    }
    
    /** Based on name put in Map or from List in Map;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     */
    public void put(MethodContext methodContext, Object value) {
        if (this.needsExpand) {
            methodContext.putEnv(name, value);
        } else {
            methodContext.putEnv(fma, value);
        }
    }
    
    /** Based on name remove from Map or from List in Map */
    public Object remove(MethodContext methodContext) {
        if (this.needsExpand) {
            return methodContext.removeEnv(name);
        } else {
            return methodContext.removeEnv(fma);
        }
    }
    
    /** Based on name get from Map or from List in Map */
    public Object get(Map theMap, MethodContext methodContext) {
        if (this.needsExpand) {
            FlexibleMapAccessor fma = new FlexibleMapAccessor(methodContext.expandString(name));
            return fma.get(theMap);
        } else {
            return fma.get(theMap);
        }
    }
    
    /** Based on name put in Map or from List in Map;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     */
    public void put(Map theMap, Object value, MethodContext methodContext) {
        if (this.needsExpand) {
            FlexibleMapAccessor fma = new FlexibleMapAccessor(methodContext.expandString(name));
            fma.put(theMap, value);
        } else {
            fma.put(theMap, value);
        }
    }
    
    /** Based on name remove from Map or from List in Map */
    public Object remove(Map theMap, MethodContext methodContext) {
        if (this.needsExpand) {
            FlexibleMapAccessor fma = new FlexibleMapAccessor(methodContext.expandString(name));
            return fma.remove(theMap);
        } else {
            return fma.remove(theMap);
        }
    }
    
    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key */    
    public int hashCode() {
        return this.name.hashCode();
    }

    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key */    
    public boolean equals(Object obj) {
        if (obj instanceof ContextAccessor) {
            ContextAccessor contextAccessor = (ContextAccessor) obj;
            if (this.name == null) {
                return contextAccessor.name == null;
            }
            return this.name.equals(contextAccessor.name);
        } else {
            String str = (String) obj;
            if (this.name == null) {
                return str == null;
            }
            return this.name.equals(str);
        }
    }

    /** To be used for a string representation of the accessor, returns the original name. */    
    public String toString() {
        return this.name;
    }
}
