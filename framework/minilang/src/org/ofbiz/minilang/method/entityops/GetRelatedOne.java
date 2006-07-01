/*
 * $Id: GetRelatedOne.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method.entityops;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Gets a list of related entity instance according to the specified relation-name
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class GetRelatedOne extends MethodOperation {
    
    public static final String module = GetRelatedOne.class.getName();
    
    ContextAccessor valueAcsr;
    ContextAccessor toValueAcsr;
    String relationName;
    String useCacheStr;

    public GetRelatedOne(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        toValueAcsr = new ContextAccessor(element.getAttribute("to-value-name"));
        relationName = element.getAttribute("relation-name");
        useCacheStr = element.getAttribute("use-cache");
    }

    public boolean exec(MethodContext methodContext) {
        String relationName = methodContext.expandString(this.relationName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);
        boolean useCache = "true".equals(useCacheStr);

        Object valueObject = valueAcsr.get(methodContext);
        if (!(valueObject instanceof GenericValue)) {
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [env variable for value-name " + valueAcsr.toString() + " is not a GenericValue object; for the relation-name: " + relationName + "]";
            Debug.logError(errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        GenericValue value = (GenericValue) valueObject;
        if (value == null) {
            Debug.logWarning("Value not found with name: " + valueAcsr + ", not getting related...", module);
            return true;
        }
        try {
            if (useCache) {
                toValueAcsr.put(methodContext, value.getRelatedOneCache(relationName));
            } else {
                toValueAcsr.put(methodContext, value.getRelatedOne(relationName));
            }
        } catch (GenericEntityException e) {
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem getting related one from entity with name " + value.getEntityName() + " for the relation-name: " + relationName + ": " + e.getMessage() + "]";
            Debug.logError(e, errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<get-related-one/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
