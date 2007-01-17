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
package org.ofbiz.workflow.impl;

import java.util.Map;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.WfException;

/**
 * WfActivitySubFlowImplementation.java
 */
public class WfActivitySubFlowImplementation extends WfActivityAbstractImplementation {

    public static final String module = WfActivitySubFlowImplementation.class.getName();

    public WfActivitySubFlowImplementation(WfActivityImpl wfActivity) {
        super(wfActivity);
    }

    /**
     * @see org.ofbiz.workflow.impl.WfActivityAbstractImplementation#run()
     */
    public void run() throws WfException {
        GenericValue subFlow = null;
        try {
            subFlow = getActivity().getDefinitionObject().getRelatedOne("WorkflowActivitySubFlow");
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        if (subFlow == null)
            return;

        String type = "WSE_SYNCHR";
        if (subFlow.get("executionEnumId") != null)
            type = subFlow.getString("executionEnumId");

        // Build a model service
        ModelService service = new ModelService();
        service.name = service.toString();
        service.engineName = "workflow";
        service.location = subFlow.getString("packageId");
        service.invoke = subFlow.getString("subFlowProcessId");
        service.validate = false;        

        // we don't use the service definition parameters (since there is no definition) so, we will just grab 
        // the actual parameters and let the workflow engine test the contextSignature
        String actualParameters = subFlow.getString("actualParameters");
        GenericResultWaiter waiter = runService(service, actualParameters, null);
        if (type.equals("WSE_SYNCHR")) {
            Map subResult = waiter.waitForResult();
            this.setResult(subResult);
        }
        setComplete(true);
    }
}
