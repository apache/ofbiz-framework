/*
 * $Id: GroupServiceModel.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.service.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.base.util.Debug;
import org.w3c.dom.Element;

/**
 * GroupServiceModel.java
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class GroupServiceModel {
    
    public static final String module = GroupServiceModel.class.getName();

    private String serviceName, serviceMode;
    private boolean resultToContext = false;
    
    /**
     * Constructor using DOM element
     * @param service DOM element for the service
     */
    public GroupServiceModel(Element service) {
        this.serviceName = service.getAttribute("name");
        this.serviceMode = service.getAttribute("mode");
        this.resultToContext = service.getAttribute("result-to-context").equalsIgnoreCase("true") ? true : false;
    }  
    
    /**
     * Basic constructor
     * @param serviceName name of the service
     * @param serviceMode service invocation mode (sync|async)
     */
    public GroupServiceModel(String serviceName, String serviceMode) {
        this.serviceName = serviceName;
        this.serviceMode = serviceMode;
    }  
    
    /**
     * Getter for the service mode
     * @return String
     */
    public String getMode() {
        return this.serviceMode;
    }
    
    /**
     * Getter for the service name
     * @return String
     */
    public String getName() {
        return this.serviceName;
    }  
    
    /**
     * Returns true if the results of this service are to go back into the context
     * @return boolean
     */
    public boolean resultToContext() {
        return this.resultToContext;
    }
    
    /**
     * Invoker method to invoke this service
     * @param dispatcher ServiceDispatcher used for this invocation
     * @param localName Name of the LocalDispatcher used
     * @param context Context for this service (will use only valid parameters)
     * @return Map result Map
     * @throws GenericServiceException
     */
    public Map invoke(ServiceDispatcher dispatcher, String localName, Map context) throws GenericServiceException {
        DispatchContext dctx = dispatcher.getLocalContext(localName);
        ModelService model = dctx.getModelService(getName());
        if (model == null)
            throw new GenericServiceException("Group defined service (" + getName() + ") is not a defined service.");
            
        Map thisContext = model.makeValid(context, ModelService.IN_PARAM);
        Debug.logInfo("Running grouped service [" + serviceName + "]", module);
        if (getMode().equals("async")) {
            List requiredOut = model.getParameterNames(ModelService.OUT_PARAM, false);
            if (requiredOut.size() > 0) {
                Debug.logWarning("Grouped service (" + getName() + ") requested 'async' invocation; running sync because of required OUT parameters.", module);
                return dispatcher.runSync(localName, model, thisContext);
            } else {
                dispatcher.runAsync(localName, model, thisContext, false);
                return new HashMap();
            }
        } else {
            return dispatcher.runSync(localName, model, thisContext);
        }
    }
           
    /**     
     * @see java.lang.Object#toString()
     */           
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append(getName());
        str.append("::");
        str.append(getMode());
        str.append("::");
        return str.toString();
    }
}
