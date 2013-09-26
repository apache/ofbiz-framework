/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.service.engine;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.xmlrpc.XmlRpcClient;

/**
 * Engine For XML RPC CLient Configuration management 
 */
public class XMLRPCClientEngine extends GenericAsyncEngine {

    public static final String module = XMLRPCClientEngine.class.getName();

    public XMLRPCClientEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> result = serviceInvoker(modelService, context);

        if (result == null)
            throw new GenericServiceException("Service did not return expected result");
        return result;
    }
    
    /*
     *  Invoke the remote XMLRPC SERVICE : This engine convert all value in IN mode to one struct.
     */
    private Map<String, Object> serviceInvoker(ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        if (modelService.location == null || modelService.invoke == null)
            throw new GenericServiceException("Cannot locate service to invoke");
        
        XmlRpcClientConfigImpl config = null;
        XmlRpcClient client = null;
        String serviceName = modelService.invoke;
        String engine = modelService.engineName;
        String url = null;
        String login = null;
        String password = null;
        String keyStoreComponent = null;
        String keyStoreName = null;
        String keyAlias  = null;
        try {
            url = ServiceConfigUtil.getEngineParameter(engine, "url");
            login = ServiceConfigUtil.getEngineParameter(engine, "login");
            password = ServiceConfigUtil.getEngineParameter(engine, "password");
            keyStoreComponent = ServiceConfigUtil.getEngineParameter(engine, "keyStoreComponent");
            keyStoreName = ServiceConfigUtil.getEngineParameter(engine, "keyStoreName");
            keyAlias = ServiceConfigUtil.getEngineParameter(engine, "keyAlias");
            config = new XmlRpcClientConfigImpl();
            config.setBasicUserName(login);
            config.setBasicPassword(password);
            config.setServerURL(new URL(url));
        }catch (MalformedURLException e) {
            throw new GenericServiceException("Cannot invoke service : engine parameters are not correct");
        }
        catch (GenericConfigException e) {
            throw new GenericServiceException("Cannot invoke service : engine parameters are not correct");
        }
        if(UtilValidate.isNotEmpty(keyStoreComponent) && UtilValidate.isNotEmpty(keyStoreName) && UtilValidate.isNotEmpty(keyAlias)){
            client = new XmlRpcClient(config, keyStoreComponent, keyStoreName, keyAlias);
        }
        else{
            client = new XmlRpcClient(config);
        }
        List<ModelParam> inModelParamList = modelService.getInModelParamList();

        if (Debug.verboseOn()) {
            Debug.logVerbose("[XMLRPCClientEngine.invoke] : Parameter length - " + inModelParamList.size(), module);
            for (ModelParam p: inModelParamList) {
                Debug.logVerbose("[XMLRPCClientEngine.invoke} : Parameter: " + p.name + " (" + p.mode + ")", module);
            }
        }

        Map<String, Object> result = null;
        Map<String, Object> params = FastMap.newInstance();
        for (ModelParam modelParam: modelService.getModelParamList()) {
            // don't include OUT parameters in this list, only IN and INOUT
            if ("OUT".equals(modelParam.mode) || modelParam.internal) continue;

            Object paramValue = context.get(modelParam.name);
            if (paramValue != null) {
                params.put(modelParam.name, paramValue);
            }
        }
        
        List<Map<String,Object>> listParams = UtilMisc.toList(params);
        try{
            result = UtilGenerics.cast(client.execute(serviceName, listParams.toArray()));
        }catch (XmlRpcException e) {
            result = ServiceUtil.returnError(e.getLocalizedMessage());
        }
        return result;
    }
}
