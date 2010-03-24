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

import static org.ofbiz.base.util.UtilGenerics.cast;
import groovy.lang.Script;

import java.util.Map;

import javolution.util.FastMap;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GroovyUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Groovy Script Service Engine
 */
public final class GroovyEngine extends GenericAsyncEngine {

    protected static final Object[] EMPTY_ARGS = {};

    public GroovyEngine(ServiceDispatcher dispatcher) {
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
        return serviceInvoker(localName, modelService, context);
    }

    private Map<String, Object> serviceInvoker(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        if (UtilValidate.isEmpty(modelService.location)) {
            throw new GenericServiceException("Cannot run Groovy service with empty location");
        }
        Map<String, Object> params = FastMap.newInstance();
        params.putAll(context);
        context.put("parameters", params);
        context.put("dctx", dispatcher.getLocalContext(localName));
        context.put("dispatcher", dispatcher);
        context.put("delegator", dispatcher.getDelegator());
        try {
            Script script = InvokerHelper.createScript(GroovyUtil.getScriptClassFromLocation(this.getLocation(modelService)), GroovyUtil.getBinding(context));
            Object resultObj = null;
            if (UtilValidate.isEmpty(modelService.invoke)) {
                resultObj = script.run();
            } else {
                resultObj = script.invokeMethod(modelService.invoke, EMPTY_ARGS);
            }
            if (resultObj != null && resultObj instanceof Map<?, ?>) {
                return cast(resultObj);
            } else if (context.get("result") != null && context.get("result") instanceof Map<?, ?>) {
                return cast(context.get("result"));
            }
        } catch (GeneralException e) {
            throw new GenericServiceException(e);
        }
        return ServiceUtil.returnSuccess();
    }
}
