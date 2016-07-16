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
package org.apache.ofbiz.service.rmi;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;

import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.engine.GenericAsyncEngine;

/**
 * RmiServiceEngine.java
 */
public class RmiServiceEngine extends GenericAsyncEngine {

    public RmiServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        return run(modelService, context);
    }

    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        run(modelService, context);
    }

    protected Map<String, Object> run(ModelService service, Map<String, Object> context) throws GenericServiceException {
        // locate the remote dispatcher
        RemoteDispatcher rd = null;
        try {
            rd = (RemoteDispatcher) Naming.lookup(this.getLocation(service));
        } catch (NotBoundException e) {
            throw new GenericServiceException("RemoteDispatcher not bound to : " + service.location, e);
        } catch (java.net.MalformedURLException e) {
            throw new GenericServiceException("Invalid format for location");
        } catch (RemoteException e) {
            throw new GenericServiceException("RMI Error", e);
        }

        Map<String, Object> result = null;
        if (rd != null) {
            try {
                result = rd.runSync(service.invoke, context);
            } catch (RemoteException e) {
                throw new GenericServiceException("RMI Invocation Error", e);
            }
        } else {
            throw new GenericServiceException("RemoteDispatcher came back as null");
        }

        if (result == null) {
            throw new GenericServiceException("Null result returned");
        }

        return result;
    }
}
