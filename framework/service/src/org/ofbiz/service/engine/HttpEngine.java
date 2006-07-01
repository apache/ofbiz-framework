/*
 * $Id: HttpEngine.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.service.engine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;

/**
 * HttpEngine.java
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class HttpEngine extends GenericAsyncEngine {
    
    public static final String module = HttpEngine.class.getName();
    private static final boolean exportAll = false;

    public HttpEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }
    
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {       
        DispatchContext dctx = dispatcher.getLocalContext(localName);
        String xmlContext = null;
        
        try {
            if (Debug.verboseOn()) Debug.logVerbose("Serializing Context --> " + context, module);
            xmlContext = XmlSerializer.serialize(context);
        } catch (Exception e) {
            throw new GenericServiceException("Cannot serialize context.", e);
        }
        
        Map parameters = new HashMap();
        parameters.put("serviceName", modelService.invoke);
        if (xmlContext != null)
            parameters.put("serviceContext", xmlContext);
        
        HttpClient http = new HttpClient(this.getLocation(modelService), parameters);
        String postResult = null;        
        try {
            postResult = http.post();
        } catch (HttpClientException e) {
            throw new GenericServiceException("Problems invoking HTTP request", e);
        }
        
        Map result = null;
        try {
            Object res = XmlSerializer.deserialize(postResult, dctx.getDelegator());
            if (res instanceof Map)
                result = (Map) res;
            else
                throw new GenericServiceException("Result not an instance of Map.");
        } catch (Exception e) {
            throw new GenericServiceException("Problems deserializing result.", e);
        }
        
        return result;
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        Map result = runSync(localName, modelService, context);
    } 
    
    /**
     * Event for handling HTTP services
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @return null
     */
    public static String httpEngine(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        
        String serviceName = request.getParameter("serviceName");
        String serviceMode = request.getParameter("serviceMode");
        String xmlContext = request.getParameter("serviceContext");
        
        Map result = new HashMap();
        Map context = null;
        
        if (serviceName == null)
            result.put(ModelService.ERROR_MESSAGE, "Cannot have null service name");
            
        if (serviceMode == null)
            serviceMode = "SYNC";
                
        // deserialize the context
        if (!result.containsKey(ModelService.ERROR_MESSAGE)) {
            if (xmlContext != null) {
                try {
                    Object o = XmlSerializer.deserialize(xmlContext, delegator);
                    if (o instanceof Map)
                        context = (Map) o;
                    else {
                        Debug.logError("Context not an instance of Map error", module);
                        result.put(ModelService.ERROR_MESSAGE, "Context not an instance of Map");
                    }
                } catch (Exception e) {
                    Debug.logError(e, "Deserialization error", module);
                    result.put(ModelService.ERROR_MESSAGE, "Error occurred deserializing context: " + e.toString());
                }
            }
        }
        
        // invoke the service
        if (!result.containsKey(ModelService.ERROR_MESSAGE)) {            
            try {
                ModelService model = dispatcher.getDispatchContext().getModelService(serviceName);
                if (model.export || exportAll) {
                    if (serviceMode.equals("ASYNC")) {
                        dispatcher.runAsync(serviceName, context);
                    } else {
                        result = dispatcher.runSync(serviceName, context);
                    }
                } else {
                    Debug.logWarning("Attempt to invoke a non-exported service: " + serviceName, module);
                    throw new GenericServiceException("Cannot find requested service");
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Service invocation error", module);
                result.put(ModelService.ERROR_MESSAGE, "Service invocation error: " + e.toString());
            }
        }
        
        // backup error message
        StringBuffer errorMessage = new StringBuffer();
        
        // process the result
        String resultString = null;
        try {
            resultString = XmlSerializer.serialize(result);
        } catch (Exception e) {
            Debug.logError(e, "Cannot serialize result", module);
            if (result.containsKey(ModelService.ERROR_MESSAGE))
                errorMessage.append(result.get(ModelService.ERROR_MESSAGE));
            errorMessage.append("::");
            errorMessage.append(e);
        }
        
        // handle the response
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("plain/text");
            
            if (errorMessage.length() > 0) {
                response.setContentLength(errorMessage.length());
                out.write(errorMessage.toString());
            } else {
                response.setContentLength(resultString.length());
                out.write(resultString);
            }
            
            out.flush();
            response.flushBuffer();
        } catch (IOException e) {
            Debug.logError(e, "Problems w/ getting the servlet writer.", module);
            return "error";
        }
                                                                
        return null;
    }

    
}
