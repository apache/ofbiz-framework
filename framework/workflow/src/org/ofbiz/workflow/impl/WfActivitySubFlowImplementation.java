/*
 * $Id: WfActivitySubFlowImplementation.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.workflow.impl;

import java.util.Map;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.WfException;

/**
 * WfActivitySubFlowImplementation.java
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @author     Oswin Ondarza and Manuel Soto
 * @version    $Rev$
 * @since      2.0
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
