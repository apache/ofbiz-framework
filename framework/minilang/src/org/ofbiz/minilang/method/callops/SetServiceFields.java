/*
 * $Id: SetServiceFields.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method.callops;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.w3c.dom.Element;

/**
 * Sets all Service parameters/attributes in the to-map using the map as a source
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @version    $Rev$
 * @since      2.2
 */
public class SetServiceFields extends MethodOperation {
    
    public static final String module = CallService.class.getName();
    
    String serviceName;
    ContextAccessor mapAcsr;
    ContextAccessor toMapAcsr;

    public SetServiceFields(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        serviceName = element.getAttribute("service-name");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        toMapAcsr = new ContextAccessor(element.getAttribute("to-map-name"));
    }

    public boolean exec(MethodContext methodContext) {
        String serviceName = methodContext.expandString(this.serviceName);

        Map fromMap = (Map) mapAcsr.get(methodContext);
        if (fromMap == null) {
            Debug.logWarning("The from map in set-service-field was not found with name: " + mapAcsr, module);
            return true;
        }

        Map toMap = (Map) toMapAcsr.get(methodContext);
        if (toMap == null) {
            toMap = new HashMap();
            toMapAcsr.put(methodContext, toMap);
        }
        
        LocalDispatcher dispatcher = methodContext.getDispatcher();
        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            String errMsg = "In set-service-fields could not get service definition for service name [" + serviceName + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        Iterator inModelParamIter = modelService.getInModelParamList().iterator();
        while (inModelParamIter.hasNext()) {
            ModelParam modelParam = (ModelParam) inModelParamIter.next();
            
            if (fromMap.containsKey(modelParam.name)) {
                Object value = fromMap.get(modelParam.name);

                if (UtilValidate.isNotEmpty(modelParam.type)) {
                    try {
                        value = ObjectType.simpleTypeConvert(value, modelParam.type, null, null, false);
                    } catch (GeneralException e) {
                        String errMsg = "Could not convert field value for the parameter/attribute: [" + modelParam.name + "] on the [" + serviceName + "] service to the [" + modelParam.type + "] type for the value [" + value + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        throw new IllegalArgumentException(errMsg);
                    }
                }
                
                toMap.put(modelParam.name, value);
            }
        }
        
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<set-service-fields/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
