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

import java.util.ArrayList;
import com.ibm.icu.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityTypeUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.AlreadyRunning;
import org.ofbiz.workflow.CannotComplete;
import org.ofbiz.workflow.CannotResume;
import org.ofbiz.workflow.CannotStart;
import org.ofbiz.workflow.CannotStop;
import org.ofbiz.workflow.InvalidData;
import org.ofbiz.workflow.InvalidState;
import org.ofbiz.workflow.NotRunning;
import org.ofbiz.workflow.NotSuspended;
import org.ofbiz.workflow.ResultNotAvailable;
import org.ofbiz.workflow.TransitionNotAllowed;
import org.ofbiz.workflow.WfActivity;
import org.ofbiz.workflow.WfAssignment;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfFactory;
import org.ofbiz.workflow.WfProcess;
import org.ofbiz.workflow.WfResource;

/**
 * WfActivityImpl - Workflow Activity Object implementation
 */
public class WfActivityImpl extends WfExecutionObjectImpl implements WfActivity {

    public static final String module = WfActivityImpl.class.getName();

    private static final int CHECK_ASSIGN = 1;
    private static final int CHECK_COMPLETE = 2;

    protected String processId = null;

    public WfActivityImpl(GenericValue value, String processId) throws WfException {
        super(value, processId);
        this.processId = processId;
        init();
    }

    public WfActivityImpl(Delegator delegator, String workEffortId) throws WfException {
        super(delegator, workEffortId);
        if (UtilValidate.isEmpty(activityId))
            throw new WfException("Execution object is not of type WfActivity");
        this.processId = getRuntimeObject().getString("workEffortParentId");
    }

    private void init() throws WfException {
        GenericValue valueObject = getDefinitionObject();

        // set the activity context
        this.setProcessContext(container().contextKey());

        // parse the descriptions
        this.parseDescriptions(this.processContext());

        // check for inheritPriority attribute
        boolean inheritPriority = valueObject.getBoolean("inheritPriority").booleanValue() || false;

        if (inheritPriority) {
            GenericValue runTime = getRuntimeObject();
            Map<String, Object> context = processContext();

            if (context.containsKey("previousActivity")) {
                String previousActivity = (String) context.get("previousActivity");
                WfActivity pAct = WfFactory.getWfActivity(getDelegator(), previousActivity);

                if (pAct != null) {
                    try {
                        runTime.set("priority", new Long(pAct.priority()));
                        runTime.store();
                    } catch (GenericEntityException e) {
                        throw new WfException(e.getMessage(), e);
                    }
                }
            }
        }

        GenericValue performer = null;
        if (valueObject.get("performerParticipantId") != null) {
            try {
                performer = valueObject.getRelatedOne("PerformerWorkflowParticipant");
                if (performer == null) {
                    Map<String, Object> performerFields = UtilMisc.toMap("packageId", (Object) valueObject.getString("packageId"),
                            "packageVersion", valueObject.getString("packageVersion"), "processId", "_NA_",
                            "processVersion", "_NA_", "participantId", valueObject.getString("performerParticipantId"));
                    performer = delegator.findOne("WorkflowParticipant", performerFields, false);
                }
            } catch (GenericEntityException e) {
                throw new WfException(e.getMessage(), e);
            }
        }
        if (performer != null)
            createAssignments(performer);

        boolean limitAfterStart = valueObject.getBoolean("limitAfterStart").booleanValue();

        if (Debug.verboseOn()) {
            Debug.logVerbose("[WfActivity.init]: limitAfterStart - " + limitAfterStart, module);
        }
        if (!limitAfterStart && valueObject.get("limitService") != null && !valueObject.getString("limitService").equals("")) {
            Debug.logVerbose("[WfActivity.init]: limit service is not after start, setting up now.", module);
            setLimitService();
        }
    }

    private void createAssignments(GenericValue currentPerformer) throws WfException {
        GenericValue valueObject = getDefinitionObject();
        GenericValue performer = checkPerformer(currentPerformer);
        boolean assignAll = false;

        if (valueObject.get("acceptAllAssignments") != null) {
            assignAll = valueObject.getBoolean("acceptAllAssignments").booleanValue();
        }

        // first check for single assignment
        if (!assignAll) {
            if (performer != null) {
                Debug.logVerbose("[WfActivity.createAssignments] : (S) Single assignment", module);
                assign(WfFactory.getWfResource(performer), false);
            }
        }

        // check for a party group
        else if (performer.get("partyId") != null && !performer.getString("partyId").equals("_NA_")) {
            GenericValue partyType = null;
            GenericValue groupType = null;

            try {
                Map<String, Object> fields1 = UtilMisc.toMap("partyId", (Object) performer.getString("partyId"));
                GenericValue v1 = getDelegator().findOne("Party", fields1, false);

                partyType = v1.getRelatedOne("PartyType");
                Map<String, Object> fields2 = UtilMisc.toMap("partyTypeId", (Object) "PARTY_GROUP");
                groupType = getDelegator().findOne("PartyType", fields2, true);
            } catch (GenericEntityException e) {
                throw new WfException(e.getMessage(), e);
            }
            if (EntityTypeUtil.isType(partyType, groupType)) {
                // party is a group
                List<GenericValue> partyRelations = null;
                try {
                    Map<String, Object> fields = UtilMisc.toMap("partyIdFrom", (Object) performer.getString("partyId"),
                            "partyRelationshipTypeId", "GROUP_ROLLUP");
                    partyRelations = getDelegator().findByAnd("PartyRelationship", fields);
                } catch (GenericEntityException e) {
                    throw new WfException(e.getMessage(), e);
                }

                // make assignments for these parties
                Debug.logVerbose("[WfActivity.createAssignments] : Group assignment", module);
                for (GenericValue value : partyRelations) {
                    assign(
                        WfFactory.getWfResource(getDelegator(), null, null, value.getString("partyIdTo"), null),
                        true);
                }
            } else {
                // not a group
                Debug.logVerbose("[WfActivity.createAssignments] : (G) Single assignment", module);
                assign(WfFactory.getWfResource(performer), false);
            }
        }

        // check for role types
        else if (performer.get("roleTypeId") != null && !performer.getString("roleTypeId").equals("_NA_")) {
            List<GenericValue> partyRoles = null;

            try {
                Map<String, Object> fields = UtilMisc.toMap("roleTypeId", (Object) performer.getString("roleTypeId"));
                partyRoles = getDelegator().findByAnd("PartyRole", fields);
            } catch (GenericEntityException e) {
                throw new WfException(e.getMessage(), e);
            }

            // loop through the roles and create assignments
            Debug.logVerbose("[WfActivity.createAssignments] : Role assignment", module);
            for (GenericValue value : partyRoles) {
                assign(WfFactory.getWfResource(value.getDelegator(), null, null, value.getString("partyId"), null), true);
            }
        }
    }

    private List<WfAssignment> getAssignments() throws WfException {
        List<WfAssignment> assignments = new ArrayList<WfAssignment>();
        List<GenericValue> assignList = this.getAllAssignments();

        if (assignList == null)
            return assignments;

        for (GenericValue value : assignList) {
            String party = value.getString("partyId");
            String role = value.getString("roleTypeId");
            String status = value.getString("statusId");
            java.sql.Timestamp from = value.getTimestamp("fromDate");

            if (status.equals("CAL_SENT") || status.equals("CAL_ACCEPTED") || status.equals("CAL_TENTATIVE"))
                assignments.add(WfFactory.getWfAssignment(getDelegator(), runtimeKey(), party, role, from));
        }
        if (Debug.verboseOn()) Debug.logVerbose("Found [" + assignments.size() + "] assignment(s)", module);
        return assignments;
    }

    private List<GenericValue> getAllAssignments() throws WfException {
        List<GenericValue> assignList = null;
        try {
            assignList = getDelegator().findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", runtimeKey()));
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }

        if (assignList != null) {
            assignList = EntityUtil.filterByDate(assignList);
        } else {
            return new ArrayList<GenericValue>();
        }
        return assignList;
    }

    // create a new assignment
    private WfAssignment assign(WfResource resource, boolean append) throws WfException {
        if (!append) {
            for (WfAssignment a : getAssignments()) {
                a.remove();
            }
        }

        WfAssignment assign = WfFactory.getWfAssignment(this, resource, null, true);
        return assign;
    }

    // check the performer: dynamic; defined partyId/roleTypeId
    private GenericValue checkPerformer(GenericValue performer) throws WfException {
        GenericValue newPerformer = GenericValue.create(performer);
        Map<String, Object> context = processContext();

        String performerType = performer.getString("participantTypeId");
        String partyId = performer.getString("partyId");
        String roleTypeId = performer.getString("roleTypeId");

        // check for dynamic party
        if (partyId != null && partyId.trim().toLowerCase().startsWith("expr:")) {
            if (Debug.verboseOn()) Debug.logVerbose("Dynamic performer: Found a party expression", module);
            Object value = null;
            try {
                value = BshUtil.eval(partyId.trim().substring(5).trim(), context);
            } catch (bsh.EvalError e) {
                throw new WfException("Bsh evaluation error occurred.", e);
            }
            if (value != null) {
                if (value instanceof String) {
                    newPerformer.set("partyId", value);
                } else {
                    throw new WfException("Expression did not return a String");
                }
            }
        }

        // check for dynamic role
        if (roleTypeId != null && roleTypeId.trim().toLowerCase().startsWith("expr:")) {
            if (Debug.verboseOn()) Debug.logVerbose("Dynamic performer: Found a role expression", module);
            Object value = null;
            try {
                value = BshUtil.eval(roleTypeId.trim().substring(5).trim(), context);
            } catch (bsh.EvalError e) {
                throw new WfException("Bsh evaluation error occurred.", e);
            }
            if (value != null) {
                if (value instanceof String) {
                    newPerformer.set("roleTypeId", value);
                } else {
                    throw new WfException("Expression did not return a String");
                }
            }
        }

        // check for un-defined party
        if (performerType.equals("HUMAN") || performerType.equals("ORGANIZATIONAL_UNIT")) {
            if (partyId == null) {
                newPerformer.set("partyId", performer.getString("participantId"));
            }
        }

        // check for un-defined role
        if (performerType.equals("ROLE")) {
            if (roleTypeId == null) {
                newPerformer.set("roleTypeId", performer.getString("participantId"));
            }
        }

        return newPerformer;
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#activate()
     */
    public void activate() throws WfException, CannotStart, AlreadyRunning {
        // make sure we aren't already running
        if (this.state().equals("open.running"))
            throw new AlreadyRunning();

        // check the start mode
        String mode = getDefinitionObject().getString("startModeEnumId");

        if (mode == null) {
            throw new WfException("Start mode cannot be null");
        }

        if (mode.equals("WAM_AUTOMATIC")) {
            Debug.logVerbose("Activity start mode is AUTO", module);
            for (WfAssignment assignment : getAssignments()) {
                 assignment.changeStatus("CAL_ACCEPTED"); // accept all assignments (AUTO)
            }
            Debug.logVerbose("All assignments accepted, starting activity: " + this.runtimeKey(), module);
            startActivity();
        } else if (howManyAssignment() > 0 && checkAssignStatus(CHECK_ASSIGN)) {
            Debug.logVerbose("Starting activity: " + this.runtimeKey(), module);
            startActivity();
        }
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#complete()
     */
    public void complete() throws WfException, CannotComplete {
        // check to make sure all assignements are complete
        if (howManyAssignment() > 0 && !checkAssignStatus(CHECK_COMPLETE))
            throw new CannotComplete("All assignments have not been completed");
        try {
            container().receiveResults(this, result());
        } catch (InvalidData e) {
            throw new WfException("Invalid result data was passed", e);
        }
        try {
            changeState("closed.completed");
            GenericValue activityWe = getRuntimeObject();
            activityWe.set("actualCompletionDate", UtilDateTime.nowTimestamp());
            activityWe.store();
        } catch (InvalidState is) {
            throw new WfException("Invalid WF State", is);
        } catch (TransitionNotAllowed tna) {
            throw new WfException(tna.getMessage(), tna);
        } catch (GenericEntityException gee) {
            throw new WfException(gee.getMessage(), gee);
        }

        container().activityComplete(this);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#resume()
     */
    @Override
    public void resume() throws WfException, CannotResume, NotRunning, NotSuspended {
        super.resume();
        try {
            Debug.logVerbose("Checking to see if we can complete the activity", module);
            this.checkComplete();
        } catch (CannotComplete e) {
            throw new CannotResume("Attempt to complete activity failed", e);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#abort()
     */
    @Override
    public void abort() throws WfException, CannotStop, NotRunning {
        super.abort();

        // cancel the active assignments
        for (WfAssignment assignment : this.getAssignments()) {
            assignment.changeStatus("CAL_CANCELLED");
        }
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#isMemberOfAssignment(org.ofbiz.workflow.WfAssignment)
     */
    public boolean isMemberOfAssignment(WfAssignment member) throws WfException {
        return getAssignments().contains(member);
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#container()
     */
    public WfProcess container() throws WfException {
        return WfFactory.getWfProcess(delegator, processId);
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#setResult(java.util.Map)
     */
    public void setResult(Map<String, Object> newResult) throws WfException, InvalidData {
        if (UtilValidate.isNotEmpty(newResult)) {
            if (Debug.verboseOn())
                Debug.logVerbose(
                    "[WfActivity.setResult]: putting (" + newResult.size() + ") keys into context.",
                    module);
            Map<String, Object> context = processContext();
            context.putAll(newResult);
            setSerializedData(context);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#howManyAssignment()
     */
    public int howManyAssignment() throws WfException {
        return getAssignments().size();
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#result()
     */
    public Map<String, Object> result() throws WfException, ResultNotAvailable {
        // Get the results from the signature.
        Map<String, Object> resultSig = container().manager().resultSignature();
        Map<String, Object> results = new HashMap<String, Object>();
        Map<String, Object> context = processContext();

        if (resultSig != null) {
            Set<String> resultKeys = resultSig.keySet();
            for (String key : resultKeys) {
                if (context.containsKey(key))
                    results.put(key, context.get(key));
            }
        }
        return results;
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#getSequenceAssignment(int)
     */
    public List<WfAssignment> getSequenceAssignment(int maxNumber) throws WfException {
        if (maxNumber > 0)
            return getAssignments().subList(0, (maxNumber - 1));
        return getAssignments();
    }

    /**
     * @see org.ofbiz.workflow.WfActivity#getIteratorAssignment()
     */
    public Iterator<WfAssignment> getIteratorAssignment() throws WfException {
        return getAssignments().iterator();
    }

    /**
     * @see org.ofbiz.workflow.impl.WfExecutionObjectImpl#executionObjectType()
     */
    @Override
    public String executionObjectType() {
        return "WfActivity";
    }

    // Checks to see if we can complete
    private void checkComplete() throws WfException, CannotComplete {
        String mode = getDefinitionObject().getString("finishModeEnumId");

        if (mode == null)
            throw new CannotComplete("Finish mode cannot be null");

        // Default mode is MANUAL -- only finish if we are automatic
        if (mode.equals("WAM_AUTOMATIC")) {
            // check and make sure we are not suspended
            if (state().equals("open.running")) {
                // set the status of the assignments
                for (WfAssignment assignment : getAssignments()) {
                    assignment.changeStatus("CAL_COMPLETED");
                }
                this.complete();
            }
        }
    }

    // Checks the staus of all assignments
    // type 1 -> accept status
    // type 2 -> complete status
    private boolean checkAssignStatus(int type) throws WfException {
        boolean acceptAll = false;
        boolean completeAll = false;
        GenericValue valueObject = getDefinitionObject();

        if (valueObject.get("acceptAllAssignments") != null)
            acceptAll = valueObject.getBoolean("acceptAllAssignments").booleanValue();
        if (valueObject.get("completeAllAssignments") != null)
            completeAll = valueObject.getBoolean("completeAllAssignments").booleanValue();

        List<String> validStatus = null;

        if (type == CHECK_ASSIGN)
            validStatus = UtilMisc.toList("CAL_ACCEPTED", "CAL_COMPLETED", "CAL_DELEGATED");
        else if (type == CHECK_COMPLETE)
            validStatus = UtilMisc.toList("CAL_COMPLETED", "CAL_DELEGATED");
        else
            throw new WfException("Invalid status type");

        boolean foundOne = false;

        for (GenericValue value : this.getAllAssignments()) {
            String party = value.getString("partyId");
            String role = value.getString("roleTypeId");
            java.sql.Timestamp from = value.getTimestamp("fromDate");
            WfAssignment a = WfFactory.getWfAssignment(getDelegator(), runtimeKey(), party, role, from);

            if (validStatus.contains(a.status())) {
                // if we find one set this to true
                foundOne = true;
            } else {
                // if we must completeAll / assignAll and this one fails return false
                if ((type == CHECK_COMPLETE && completeAll) || (type == CHECK_ASSIGN && acceptAll))
                    return false;
            }
        }

        // we are here only if all are done, or complete/assign is false; so if not false we are done
        if ((type == CHECK_COMPLETE && completeAll) || (type == CHECK_ASSIGN && acceptAll)) {
            return true;
        } else {
            // if not all done, we don't require all, so use that foundOne stored above
            Debug.logVerbose("[checkAssignStatus] : need only one assignment to pass", module);
            if (foundOne)
                return true;
            Debug.logVerbose("[checkAssignStatus] : found no assignment(s)", module);
            return false;
        }
    }

    // Starts or activates this automatic activity
    private void startActivity() throws WfException, CannotStart {
        try {
            changeState("open.running");
        } catch (InvalidState is) {
            throw new CannotStart(is.getMessage(), is);
        } catch (TransitionNotAllowed tna) {
            throw new CannotStart(tna.getMessage(), tna);
        }
        // check the limit service
        boolean limitAfterStart = getDefinitionObject().getBoolean("limitAfterStart").booleanValue();

        if (limitAfterStart
            && getDefinitionObject().get("limitService") != null
            && !getDefinitionObject().getString("limitService").equals("")) {
            Debug.logVerbose("[WfActivity.init]: limit service is after start, setting up now.", module);
            setLimitService();
        }

        // set the new previousActivity
        Map<String, Object> context = processContext();
        context.put("previousActivity", workEffortId);
        this.setProcessContext(context);

        // set the actualStartDate
        try {
            GenericValue v = getRuntimeObject();
            v.set("actualStartDate", UtilDateTime.nowTimestamp());
            v.store();
        } catch (GenericEntityException e) {
            Debug.logWarning("Could not set 'actualStartDate'.", module);
            e.printStackTrace();
        }

        // get the type of this activity
        String type = getDefinitionObject().getString("activityTypeEnumId");

        if (type == null)
            throw new WfException("Illegal activity type");

        WfActivityAbstractImplementation executor = WfActivityImplementationFact.getConcretImplementation(type, this);
        executor.run();
        this.setResult(executor.getResult());
        if (executor.isComplete())
            this.checkComplete();
    }

    // schedule the limit service to run
    private void setLimitService() throws WfException {
        LocalDispatcher dispatcher = getDispatcher();


        DispatchContext dctx = dispatcher.getDispatchContext();
        String limitService = getDefinitionObject().getString("limitService");
        ModelService service = null;

        try {
            service = dctx.getModelService(limitService);
            Debug.logVerbose("[WfActivity.setLimitService] : Found service model.", module);
        } catch (GenericServiceException e) {
            Debug.logError(e, "[WfActivity.setLimitService] : Cannot get service model.", module);
        }
        if (service == null) {
            Debug.logWarning("[WfActivity.setLimitService] : Cannot determine limit service, ignoring.", module);
            return;
        }

        // make the limit service context
        List<String> inList = new ArrayList<String>(service.getInParamNames());
        String inParams = StringUtil.join(inList, ",");

        Map<String, Object> serviceContext = actualContext(inParams, null, null, true);
        Debug.logVerbose("Setting limit service with context: " + serviceContext, module);

        Double timeLimit = null;

        if (getDefinitionObject().get("timeLimit") != null)
            timeLimit = getDefinitionObject().getDouble("timeLimit");
        if (timeLimit == null)
            return;

        String durationUOM = null;

        if (container().getDefinitionObject().getString("durationUomId") != null)
            durationUOM = container().getDefinitionObject().getString("durationUomId");
        if (durationUOM == null)
            return;

        char durChar = durationUOM.charAt(0);
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date());
        switch (durChar) {
            case 'Y' :
                cal.add(Calendar.YEAR, timeLimit.intValue());
                break;

            case 'M' :
                cal.add(Calendar.MONTH, timeLimit.intValue());
                break;

            case 'D' :
                cal.add(Calendar.DATE, timeLimit.intValue());
                break;

            case 'h' :
                cal.add(Calendar.HOUR, timeLimit.intValue());
                break;

            case 'm' :
                cal.add(Calendar.MINUTE, timeLimit.intValue());
                break;

            case 's' :
                cal.add(Calendar.SECOND, timeLimit.intValue());
                break;

            default :
                throw new WfException("Invalid duration unit");
        }

        long startTime = cal.getTime().getTime();

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("serviceName", limitService);
        context.put("serviceContext", serviceContext);
        context.put("workEffortId", runtimeKey());

        try {
            dispatcher.schedule("wfLimitInvoker", context, startTime); // yes we are hard coded!
        } catch (GenericServiceException e) {
            throw new WfException(e.getMessage(), e);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("[WfActivity.setLimitService]: Set limit service (" + limitService + ") to run at " + startTime, module);
        }
    }

    Map<String, Object> actualContext(String actualParameters, String extendedAttr, List<String> serviceSignature, boolean ignoreUnknown) throws WfException {
        Map<String, Object> actualContext = new HashMap<String, Object>();
        Map<String, Object> context = processContext();

        // extended attributes take priority over context attributes
        Map<String, String> extendedAttributes = StringUtil.strToMap(extendedAttr);

        if (UtilValidate.isNotEmpty(extendedAttributes)) {
            context.putAll(extendedAttributes);
        }

        // setup some internal buffer parameters
        GenericValue userLogin = null;

        if (context.containsKey("runAsUser")) {
            userLogin = getUserLogin((String) context.get("runAsUser"));
            actualContext.put("userLogin", userLogin);
        } else if (context.containsKey("workflowOwnerId")) {
            userLogin = getUserLogin((String) context.get("workflowOwnerId"));
        }

        // some static context values
        context.put("userLogin", userLogin);
        context.put("workEffortId", runtimeKey());
        if (howManyAssignment() == 1) {
            Debug.logVerbose("Single assignment; getting assignment info.", module);
            List<WfAssignment> assignments = getAssignments();
            WfAssignment assign = assignments.iterator().next();
            WfResource res = assign.assignee();
            context.put("assignedPartyId", res.resourcePartyId());
            context.put("assignedRoleTypeId", res.resourceRoleId());
        }

        // first we will pull out the values from the context for the actual parameters
        if (actualParameters != null) {
            List<String> params = StringUtil.split(actualParameters, ",");
            for (String key : params) {
                if (key != null && key.trim().toLowerCase().startsWith("expr:")) {
                    // check for bsh expressions; this does not place values into the context
                    try {
                        BshUtil.eval(key.trim().substring(5).trim(), context);
                    } catch (bsh.EvalError e) {
                        throw new WfException("Bsh evaluation error.", e);
                    }
                } else if (key != null && key.trim().toLowerCase().startsWith("name:")) {
                    // name mapping of context values
                    List<String> couple = StringUtil.split(key.trim().substring(5).trim(), "=");
                    String mName = couple.get(0); // mapped name
                    String cName = couple.get(1); // context name

                    // trim out blank space
                    if (mName != null) mName = mName.trim();
                    if (cName != null) cName = cName.trim();

                    if (mName != null && cName != null && context.containsKey(cName)) {
                        actualContext.put(mName, context.get(cName));
                    }
                } else if (context.containsKey(key)) {
                    // direct assignment from context
                    actualContext.put(key, context.get(key));
                } else if (!actualContext.containsKey(key) && !ignoreUnknown) {
                    throw new WfException("Context does not contain the key: '" + key + "'");
                }
            }
        }

        // the serviceSignature should not limit which parameters are in the actualContext
        // so instead we will use this signature to pull out values so they do not all have to be defined
        if (serviceSignature != null) {
            for (String key : serviceSignature) {
                if (!actualContext.containsKey(key) && context.containsKey(key)) {
                    actualContext.put(key, context.get(key));
                }
            }
        }
        return actualContext;
    }

    // Gets a UserLogin object for service invocation
    // This allows a workflow to invoke a service as a specific user
    private GenericValue getUserLogin(String userId) throws WfException {
        GenericValue userLogin = null;
        try {
            userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", userId), false);
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        return userLogin;
    }
}
