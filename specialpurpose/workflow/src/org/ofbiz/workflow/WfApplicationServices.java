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
package org.ofbiz.workflow;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;

/**
 * Workflow Application Services - 'Services' and 'Workers' for interaction with Workflow Application API
 */
public class WfApplicationServices {

    // -------------------------------------------------------------------
    // Appication 'Service' Methods
    // -------------------------------------------------------------------

    public static final String module = WfApplicationServices.class.getName();

    /**
     * Activate an application by inserting expected arguments in
     * the ApplicationSandbox table.
     *
     * Note: Assume that the activity (workEffort) has only one performer as
     * defined in XPDL specification, this means that there is only one
     * partyAssigment w/ CAL_ACEPTED state.
     * @param ctx Service dispatch Context
     * @param context Actual parameters
     * @throws GenericServiceException
     * @return Empty result
     */
    public static Map<String, Object> activateApplication(DispatchContext ctx, Map<String, Object> context) {
        final String workEffortId = (String) context.get("workEffortId");
        final Map<String, Object> result = new HashMap<String, Object>();

        try {
            final GenericValue weAssigment = getWorkEffortPartyAssigment(ctx.getDelegator(), workEffortId);

            final String partyId = (String) weAssigment.get("partyId");
            final String roleTypeId = (String) weAssigment.get("roleTypeId");
            final Timestamp fromDate = (Timestamp) weAssigment.get("fromDate");
            result.put("applicationId", insertAppSandbox(ctx.getDelegator(), workEffortId, partyId,
                    roleTypeId, fromDate, context));
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericServiceException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }

        return result;
    }

    public static Map<String, Object> getApplicationContext(DispatchContext ctx, Map<String, Object> context) throws GenericServiceException {
        final Delegator delegator = ctx.getDelegator();
        final Map<String, Object> result = new HashMap<String, Object>();
        try {
            result.put("applicationContext",
                    getRunTimeContext(delegator, getRuntimeData(delegator, (String) context.get("applicationId"))));
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericServiceException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    public static Map<String, Object> completeApplication(DispatchContext ctx, Map<String, Object> context) throws GenericServiceException {
        final Delegator delegator = ctx.getDelegator();
        final String applicationId = (String) context.get("applicationId");
        final Map<String, Object> result = new HashMap<String, Object>();

        GenericValue application = getApplicationSandbox(delegator, applicationId);
        GenericValue runTimeData = getRuntimeData(delegator, applicationId);
        Map<String, Object> runTimeContext = getRunTimeContext(delegator, runTimeData);
        Map<String, Object> contextSignature = new HashMap<String, Object>();
        Map<String, Object> resultSignature = new HashMap<String, Object>();
        Map<String, Object> resultContext = new HashMap<String, Object>();
        Map<String, Object> runContext = new HashMap<String, Object>(context);

        try {
            // copy all OUT & INOUT formal parameters
            getApplicationSignatures(delegator, application, contextSignature, resultSignature);
            for (String name : resultSignature.keySet()) {
                Object value = null;
                if (runTimeContext.containsKey(name)
                    && contextSignature.containsKey(name)
                    && resultSignature.containsKey(name))
                    value = runTimeContext.get(name);
                if (UtilGenerics.checkMap(context.get("result")).containsKey(name)) {
                    value = UtilGenerics.checkMap(context.get("result")).get(name);
                }
                if (value != null) {
                    resultContext.put(name,
                            ObjectType.simpleTypeConvert(value, (String) resultSignature.get(name), null, null));
                }
            }
            runTimeContext.putAll(resultContext);
            // fin de agregar
            if (Debug.verboseOn()) {
                Debug.logVerbose("Completing Application: " + applicationId, module);
                Debug.logVerbose("  Result Signature: " + resultSignature.toString(), module);
                Debug.logVerbose("  Result Values: " + resultContext.toString(), module);

            }

            setRunTimeContext(runTimeData, runTimeContext);

            runContext.remove("applicationId");

            final String workEffortId = (String) runTimeContext.get("workEffortId");
            final Timestamp fromDate = (Timestamp) application.get("fromDate");
            final String partyId = (String) application.get("partyId");
            final String roleTypeId = (String) application.get("roleTypeId");

            runContext.put("workEffortId", workEffortId);
            runContext.put("fromDate", fromDate);
            runContext.put("partyId", partyId);
            runContext.put("roleTypeId", roleTypeId);
            runContext.put("result", resultContext);

            result.putAll(ctx.getDispatcher().runSync("wfCompleteAssignment", runContext));
        } catch (GenericEntityException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        } catch (GenericServiceException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        } catch (GeneralException ge) {
            ge.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, ge.getMessage());
        }
        return result;
    }

    private static String insertAppSandbox(Delegator delegator, String workEffortId, String partyId,
            String roleTypeId, Timestamp fromDate, Map<String, Object> context) throws GenericServiceException {
        String dataId = null;
        String applicationId = Long.toString((new Date().getTime()));

        try {
            dataId = delegator.getNextSeqId("RuntimeData");
            GenericValue runtimeData = delegator.makeValue("RuntimeData", UtilMisc.toMap("runtimeDataId", dataId));
            runtimeData.set("runtimeInfo", XmlSerializer.serialize(context));
            delegator.create(runtimeData);
        } catch (GenericEntityException ee) {
            throw new GenericServiceException(ee.getMessage(), ee);
        } catch (SerializeException se) {
            throw new GenericServiceException(se.getMessage(), se);
        } catch (IOException ioe) {
            throw new GenericServiceException(ioe.getMessage(), ioe);
        }
        Map<String, Object> aFields = UtilMisc.toMap("applicationId", applicationId, "workEffortId", workEffortId,
                "partyId", partyId, "roleTypeId", roleTypeId, "fromDate", fromDate, "runtimeDataId", dataId);

        GenericValue appV = null;
        try {
            appV = delegator.makeValue("ApplicationSandbox", aFields);
            delegator.create(appV);
        } catch (GenericEntityException e) {
            throw new GenericServiceException(e.getMessage(), e);
        }
        return applicationId;
    }

    private static GenericValue getApplicationSandbox(Delegator delegator, String applicationId)
            throws GenericServiceException {
        try {
            GenericValue application =
                delegator.findByPrimaryKey("ApplicationSandbox", UtilMisc.toMap("applicationId", applicationId));
            return application;
        } catch (GenericEntityException ee) {
            throw new GenericServiceException(ee.getMessage(), ee);
        }
    }

    private static Map<String, Object> getRunTimeContext(Delegator delegator, GenericValue runTimeData)
            throws GenericServiceException {
        try {
            return UtilGenerics.checkMap(XmlSerializer.deserialize((String) runTimeData.get("runtimeInfo"), delegator));
        } catch (Exception e) {
            throw new GenericServiceException(e.getMessage(), e);
        }
    }

    private static void setRunTimeContext(GenericValue runTimeData, Map<String, Object> context) throws GenericServiceException {
        try {
            runTimeData.set("runtimeInfo", XmlSerializer.serialize(context));
            runTimeData.store();
        } catch (GenericEntityException ee) {
            throw new GenericServiceException(ee.getMessage(), ee);
        } catch (SerializeException se) {
            throw new GenericServiceException(se.getMessage(), se);
        } catch (IOException ioe) {
            throw new GenericServiceException(ioe.getMessage(), ioe);
        }
    }

    private static GenericValue getRuntimeData(Delegator delegator, String applicationId)
            throws GenericServiceException {
        try {
            GenericValue application =
                delegator.findByPrimaryKey("ApplicationSandbox", UtilMisc.toMap("applicationId", applicationId));
            return application.getRelatedOne("RuntimeData");
        } catch (GenericEntityException ee) {
            throw new GenericServiceException(ee.getMessage(), ee);
        }
    }

    private static void getApplicationSignatures(Delegator delegator, GenericValue application,
            Map<String, Object> contextSignature, Map<String, Object> resultSignature) throws GenericEntityException {
        Map<String, Object> expresions = null;
        // look for the 1st application.
        final GenericValue workEffort =
            delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", application.get("workEffortId")));
        String packageId = (String) workEffort.get("workflowPackageId");
        String packageVersion = (String) workEffort.get("workflowPackageVersion");
        String processId = (String) workEffort.get("workflowProcessId");
        String processVersion = (String) workEffort.get("workflowProcessVersion");
        String activityId = (String) workEffort.get("workflowActivityId");

        expresions = new HashMap<String, Object>();
        expresions.putAll(UtilMisc.toMap("packageId", packageId));
        expresions.putAll(UtilMisc.toMap("packageVersion", packageVersion));
        expresions.putAll(UtilMisc.toMap("processId", processId));
        expresions.putAll(UtilMisc.toMap("processVersion", processVersion));
        expresions.putAll(UtilMisc.toMap("activityId", activityId));

        final List<GenericValue> wfActivityTools = delegator.findByAnd("WorkflowActivityTool", expresions);
        final GenericValue wfActivityTool = (GenericValue) wfActivityTools.toArray()[0];

        packageId = (String) wfActivityTool.get("packageId");
        packageVersion = (String) wfActivityTool.get("packageVersion");
        processId = (String) wfActivityTool.get("processId");
        processVersion = (String) wfActivityTool.get("processVersion");
        final String applicationId = (String) wfActivityTool.get("toolId");

        expresions = new HashMap<String, Object>();
        expresions.putAll(UtilMisc.toMap("packageId", packageId));
        expresions.putAll(UtilMisc.toMap("packageVersion", packageVersion));
        expresions.putAll(UtilMisc.toMap("processId", processId));
        expresions.putAll(UtilMisc.toMap("processVersion", processVersion));
        expresions.putAll(UtilMisc.toMap("applicationId", applicationId));

        final List<GenericValue> params = delegator.findByAnd("WorkflowFormalParam", expresions);

        for (GenericValue param : params) {
            String name = param.getString("formalParamId");
            String mode = param.getString("modeEnumId");
            String type = param.getString("dataTypeEnumId");
            if (mode.equals("WPM_IN") || mode.equals("WPM_INOUT"))
                contextSignature.put(name, WfUtil.getJavaType(type));
            else if (mode.equals("WPM_OUT") || mode.equals("WPM_INOUT"))
                resultSignature.put(name, WfUtil.getJavaType(type));
        }
    }

    private static GenericValue getWorkEffortPartyAssigment(Delegator delegator, String workEffortId)
            throws GenericServiceException {
        Map<String, Object> expresions = new HashMap<String, Object>();
        expresions.putAll(UtilMisc.toMap("workEffortId", workEffortId));
        expresions.putAll(UtilMisc.toMap("statusId", "CAL_ACCEPTED"));
        List<String> orderBy = UtilMisc.toList("-fromDate");

        try {
            final List<GenericValue> assigments = delegator.findByAnd("WorkEffortPartyAssignment", expresions, orderBy);
            if (UtilValidate.isEmpty(assigments)) {
                Debug.logError("No accepted activities found for the workEffortId=" + workEffortId, module);
                throw new GenericServiceException("Can not find WorkEffortPartyAssignment for the Workflow service. WorkEffortId=" + workEffortId);
            }
            if (assigments.size() != 1)
                Debug.logWarning("More than one accepted activities found for the workEffortId=" + workEffortId, module);
            return assigments.iterator().next();
        } catch (GenericEntityException ee) {
            throw new GenericServiceException(ee.getMessage(), ee);
        }
    }
}
