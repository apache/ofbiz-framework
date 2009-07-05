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
package org.ofbiz.webslinger;

import java.io.IOException;
import java.util.Map;

import org.apache.bsf.BSFException;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.engine.GenericAsyncEngine;

public class WebslingerBSFServiceEngine extends GenericAsyncEngine {
    private static final String[] paramNames;
    private static final Class<?>[] paramTypes;
    static {
        paramNames = new String[] { "dispatcher", "context" };
        paramTypes = new Class<?>[] { DispatchContext.class, Map.class };
    }

    public WebslingerBSFServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Object[] params = new Object[] { dispatcher.getLocalContext(localName), context };
        try {
            return UtilGenerics.checkMap(WebslingerContainer.runEvent(modelService.engineName, modelService.location, paramNames, paramTypes, params));
        } catch (IOException e) {
            throw UtilMisc.initCause(new GenericServiceException(e.getMessage()), e);
        } catch (BSFException e) {
            throw UtilMisc.initCause(new GenericServiceException(e.getMessage()), e);
        }
    }
}
