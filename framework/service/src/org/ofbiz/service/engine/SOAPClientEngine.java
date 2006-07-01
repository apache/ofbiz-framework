/*
 * $Id: SOAPClientEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.service.engine;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;

import org.apache.axis.Message;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPEnvelope;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

/**
 * Generic Service SOAP Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:">Andy Chen</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public final class SOAPClientEngine extends GenericAsyncEngine {
    
    public static final String module = SOAPClientEngine.class.getName();
    
    public SOAPClientEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }
    
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }
    
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {
        Object result = serviceInvoker(modelService, context);
        
        if (result == null)
            throw new GenericServiceException("Service did not return expected result");
        if (!(result instanceof Map)) {
            Map newResult = new HashMap();
            
            newResult.put("result", result);
            return newResult;
        }
        return (Map) result;
    }
    
    // Invoke the remote SOAP service
    private Object serviceInvoker(ModelService modelService, Map context) throws GenericServiceException {
        if (modelService.location == null || modelService.invoke == null)
            throw new GenericServiceException("Cannot locate service to invoke");
        
        Service service = null;
        Call call = null;
        
        try {
            service = new Service();
            call = (Call) service.createCall();
        } catch (javax.xml.rpc.JAXRPCException e) {
            throw new GenericServiceException("RPC service error", e);
        } catch (ServiceException e) {//Add by Andy.Chen 2003.01.15
            throw new GenericServiceException("RPC service error", e);
        }
        
        URL endPoint = null;
        
        try {
            endPoint = new URL(this.getLocation(modelService));
        } catch (MalformedURLException e) {
            throw new GenericServiceException("Location not a valid URL", e);
        }
        
        List inModelParamList = modelService.getInModelParamList();
        Object[] params = new Object[inModelParamList.size()];
        
        if (Debug.infoOn()) Debug.logInfo("[SOAPClientEngine.invoke] : Parameter length - " + params.length, module);
        
        call.setTargetEndpointAddress(endPoint);
        
        if (UtilValidate.isNotEmpty(modelService.nameSpace)){
            call.setOperationName(new QName(modelService.nameSpace, modelService.invoke));
        } else {
            call.setOperationName(modelService.invoke);
        }
        
        int i = 0;
        
        call.setOperation(call.getOperationName().getLocalPart());
        Vector vParams = new Vector();
        Iterator iter = inModelParamList.iterator();
        while (iter.hasNext()) {
            ModelParam p = (ModelParam) iter.next();
            
            if (Debug.infoOn()) Debug.logInfo("[SOAPClientEngine.invoke} : Parameter: " + p.name + " (" + p.mode + ") - " + i, module);
            
            //Exclude params that ModelServiceReader insert into
            if(!p.name.trim().equals("userLogin") && !p.name.trim().equals("locale")) {
                QName qName = call.getParameterTypeByName(p.name); //.getTypeMapping().getTypeQName((Class) ObjectType.classNameClassMap.get(p.type));
                call.addParameter(p.name, qName, getMode(p.mode));
                vParams.add(context.get(p.name));
            }
            
            // if the value is null, that's fine, it will go in null...
            params[i] = context.get(p.name);
            
            i++;
        }
        
        call.setReturnType(XMLType.XSD_ANYTYPE);
        params=vParams.toArray();
        
        Object result = null;
        
        try {
            Debug.logInfo("[SOAPClientEngine.invoke] : Sending Call To SOAP Server", module);
            result = call.invoke(params);
        } catch (java.rmi.RemoteException e) {
            throw new GenericServiceException("RPC error", e);
        }
        if (Debug.verboseOn()) {
            Debug.log("SOAP Service Result - " + result, module);
        }

        return getResponseParams(call.getMessageContext().getResponseMessage());
    }
        
    private Map getResponseParams(Message respMessage) {
        Map mRet = new Hashtable();
        try {
            SOAPEnvelope resEnv = (SOAPEnvelope) respMessage.getSOAPEnvelope();
            List bodies = resEnv.getBodyElements();
            Iterator i = bodies.iterator();
            while (i.hasNext()) {
                Object o = i.next();

                if (o instanceof RPCElement) {
                    RPCElement body = (RPCElement) o;
                    List params = null;
                    params = body.getParams();

                    Iterator p = params.iterator();
                    while (p.hasNext()) {
                        RPCParam param = (RPCParam) p.next();
                        mRet.put(param.getName(), param.getValue());
                        if (Debug.verboseOn()) {
                            Debug.log("SOAP Client Param - " + param.getName() + "=" + param.getValue(), module);
                        }
                    }
                }
            }
        } catch (org.apache.axis.AxisFault e) {
            Debug.logError(e, "AxisFault", module);
        } catch (org.xml.sax.SAXException e) {
            Debug.logError(e, "SAXException", module);
        }
        return mRet;
    }
        
    private ParameterMode getMode(String sMode) {
        if (sMode.equals("IN")) {
            return ParameterMode.IN;
        } else if (sMode.equals("OUT")) {
            return ParameterMode.OUT;
        } else if (sMode.equals("INOUT")) {
            return ParameterMode.INOUT;
        } else {
            return null;
        }
    }
}
