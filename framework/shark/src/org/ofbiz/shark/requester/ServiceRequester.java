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
package org.ofbiz.shark.requester;

import java.util.HashMap;
import java.util.Map;

import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.client.wfmodel.InvalidPerformer;
import org.enhydra.shark.api.client.wfmodel.WfEventAudit;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.shark.container.SharkContainer;

public class ServiceRequester extends AbstractRequester {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final String module = ServiceRequester.class.getName();
    public static final int ASYNC = 0;
    public static final int SYNC = 1;

    protected Map initialContext = new HashMap();
    protected String serviceName = null;
    protected String eventType = null;
    protected int serviceMode = 1;

    // new requester
    public ServiceRequester(GenericValue userLogin, String eventType) {
        super(userLogin);
        this.setEventType(eventType);
    }

    public ServiceRequester(GenericValue userLogin) {
        super(userLogin);
    }

    // -------------------
    // WfRequester methods
    // -------------------

    public void receive_event(WfEventAudit event) throws BaseException, InvalidPerformer {
        Debug.logInfo("Call : ServiceRequester.receive_event(WfEventAudit event)", module);
        if (this.getEventType() == null || this.getEventType().equals(event.event_type())) {
            try {
                this.run(event);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                throw new BaseException(e);
            }
        }
    }

    public void receive_event(SharkTransaction trans, WfEventAudit event) throws BaseException, InvalidPerformer {
        Debug.logInfo("Call : ServiceRequester.receive_event (SharkTransaction trans, WfEventAudit event)", module);
        receive_event(event);
    }

    // -------------
    // local methods
    // -------------


    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return this.eventType;
    }

    public void setService(String serviceName, int serviceMode) {
        this.serviceName = serviceName;
        this.serviceMode = serviceMode;
    }

    public void setService(String serviceName) {
        this.setService(serviceName, ServiceRequester.ASYNC);
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public int getServiceMode() {
        return this.serviceMode;
    }

    public void setInitalContextValues(Map initialContext) {
        this.initialContext = new HashMap(initialContext);
    }

    private void run(WfEventAudit event) throws GenericServiceException {
        // get the dispatcher
        LocalDispatcher dispatcher = SharkContainer.getDispatcher();
        if (dispatcher == null) {
            throw new GenericServiceException("Cannot run service with null dispatcher");
        }

        // get the service context
        Map serviceContext = makeServiceContext(event, dispatcher);

        // invoke the service
        String serviceName = this.getServiceName();
        if (serviceName != null) {
            int mode = this.getServiceMode();
            if (mode == ServiceRequester.SYNC) {
                dispatcher.runSyncIgnore(serviceName, serviceContext);
            } else {
                dispatcher.runAsync(serviceName, serviceContext);
            }
        } else {
            Debug.logWarning("ServiceRequester -> receive_event() called with no service defined!", module);
        }
    }

    private Map makeServiceContext(WfEventAudit event, LocalDispatcher dispatcher) throws GenericServiceException {
        DispatchContext dctx = dispatcher.getDispatchContext();
        try {
            return dctx.getModelService(this.getServiceName()).makeValid(getWRD(event, initialContext), ModelService.IN_PARAM);
        } catch (BaseException e) {
            throw new GenericServiceException(e);
        }
    }
}
