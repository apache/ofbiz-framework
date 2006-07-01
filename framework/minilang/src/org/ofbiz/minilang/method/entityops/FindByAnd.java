/*
 * $Id: FindByAnd.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFieldMap;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by anding the map fields
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FindByAnd extends MethodOperation {
    
    public static final String module = FindByAnd.class.getName();         
    
    ContextAccessor listAcsr;
    String entityName;
    ContextAccessor mapAcsr;
    ContextAccessor orderByListAcsr;
    String delegatorName;
    String useCacheStr;
    String useIteratorStr;

    public FindByAnd(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        orderByListAcsr = new ContextAccessor(element.getAttribute("order-by-list-name"));
        delegatorName = element.getAttribute("delegator-name");

        useCacheStr = element.getAttribute("use-cache");
        useIteratorStr = element.getAttribute("use-iterator");
    }

    public boolean exec(MethodContext methodContext) {
        String entityName = methodContext.expandString(this.entityName);
        String delegatorName = methodContext.expandString(this.delegatorName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);
        String useIteratorStr = methodContext.expandString(this.useIteratorStr);
        
        boolean useCache = "true".equals(useCacheStr);
        boolean useIterator = "true".equals(useIteratorStr);
        
        List orderByNames = null;
        if (!orderByListAcsr.isEmpty()) {
            orderByNames = (List) orderByListAcsr.get(methodContext);
        }

        GenericDelegator delegator = methodContext.getDelegator();
        if (delegatorName != null && delegatorName.length() > 0) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }

        try {
            if (useIterator) {
                EntityCondition whereCond = null;
                if (!mapAcsr.isEmpty()) {
                    whereCond = new EntityFieldMap((Map) mapAcsr.get(methodContext), EntityOperator.AND);
                }
                listAcsr.put(methodContext, delegator.findListIteratorByCondition(entityName, whereCond, null, null, orderByNames, null));
            } else {
                if (useCache) {
                    listAcsr.put(methodContext, delegator.findByAndCache(entityName, (Map) mapAcsr.get(methodContext), orderByNames));
                } else {
                    listAcsr.put(methodContext, delegator.findByAnd(entityName, (Map) mapAcsr.get(methodContext), orderByNames));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem finding the " + entityName + " entity: " + e.getMessage() + "]";

            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<find-by-and/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
