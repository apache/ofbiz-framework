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
package org.apache.ofbiz.service.engine;

import static org.apache.ofbiz.base.util.UtilGenerics.cast;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptContext;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GroovyUtil;
import org.apache.ofbiz.base.util.ScriptHelper;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Script;

/**
 * Groovy Script Service Engine
 */
public final class GroovyEngine extends GenericAsyncEngine {

    public static final String module = GroovyEngine.class.getName();
    private static final Object[] EMPTY_ARGS = {};
    private static final Set<String> protectedKeys = createProtectedKeys();

    private static Set<String> createProtectedKeys() {
        Set<String> newSet = new HashSet<>();
        /* Commenting out for now because some scripts write to the parameters Map - which should not be allowed.
        newSet.add(ScriptUtil.PARAMETERS_KEY);
        */
        newSet.add("dctx");
        newSet.add("dispatcher");
        newSet.add("delegator");
        newSet.add("visualTheme");
        return Collections.unmodifiableSet(newSet);
    }

    public GroovyEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context)
            throws GenericServiceException {
        if (UtilValidate.isEmpty(modelService.location)) {
            throw new GenericServiceException("Cannot run Groovy service with empty location");
        }
        Map<String, Object> params = new HashMap<>();
        params.putAll(context);

        Map<String, Object> gContext = new HashMap<>();
        gContext.putAll(context);
        gContext.put(ScriptUtil.PARAMETERS_KEY, params);

        DispatchContext dctx = dispatcher.getLocalContext(localName);
        gContext.put("dctx", dctx);
        gContext.put("security", dctx.getSecurity());
        gContext.put("dispatcher", dctx.getDispatcher());
        gContext.put("delegator", dispatcher.getDelegator());
        try {
            ScriptContext scriptContext = ScriptUtil.createScriptContext(gContext, protectedKeys);
            ScriptHelper scriptHelper = (ScriptHelper)scriptContext.getAttribute(ScriptUtil.SCRIPT_HELPER_KEY);
            if (scriptHelper != null) {
                gContext.put(ScriptUtil.SCRIPT_HELPER_KEY, scriptHelper);
            }

            Script script = InvokerHelper.createScript(
                    GroovyUtil.getScriptClassFromLocation(getLocation(modelService)),
                    GroovyUtil.getBinding(gContext));

            // Groovy services can either be implemented as a stand-alone script or with a method inside a script.
            Object resultObj = UtilValidate.isEmpty(modelService.invoke)
                    ? script.run()
                    : script.invokeMethod(modelService.invoke, EMPTY_ARGS);

            if (resultObj == null) {
                resultObj = scriptContext.getAttribute(ScriptUtil.RESULT_KEY);
            }
            if (resultObj != null && resultObj instanceof Map<?, ?>) {
                return cast(resultObj);
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.putAll(modelService.makeValid(
                    scriptContext.getBindings(ScriptContext.ENGINE_SCOPE),
                    ModelService.OUT_PARAM));
            return result;
        } catch (GeneralException ge) {
            throw new GenericServiceException(ge);
        } catch (Exception e) {
            // detailMessage can be null.  If it is null, the exception won't be properly returned and logged,
            // and that will make spotting problems very difficult.
            // Disabling this for now in favor of returning a proper exception.
            throw new GenericServiceException("Error running Groovy method [" + modelService.invoke + "]"
                    + " in Groovy file [" + modelService.location + "]: ", e);
        }
    }
}
