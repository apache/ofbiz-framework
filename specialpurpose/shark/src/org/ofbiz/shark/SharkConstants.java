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

package org.ofbiz.shark;

public class SharkConstants {

    public static String WfAssignmentEventAudit = new String("WfAssignmentEventAudit");
    public static String eventAuditId = new String("eventAuditId");
    public static String oldUserName = new String("oldUserName");
    public static String oldName = new String("oldName");
    public static String newUserName = new String("newUserName");
    public static String newName = new String("newName");
    public static String isAccepted = new String("isAccepted");
    public static String WfCreateProcessEventAudit = new String("WfCreateProcessEventAudit");
    public static String pActivityId = new String("pActivityId");
    public static String pActivityDefId = new String("pActivityDefId");
    public static String pActivitySetDefId = new String("pActivitySetDefId");
    public static String pProcessId = new String("pProcessId");
    public static String pProcessName = new String("pProcessName");
    public static String pProcessDefId = new String("pProcessDefId");
    public static String pProcessDefName = new String("pProcessDefName");
    public static String pProcessDefVer = new String("pProcessDefVer");
    public static String pPackageId = new String("pPackageId");
    public static String WfDataEventAudit = new String("WfDataEventAudit");
    public static String oldData = new String("oldData");
    public static String newData = new String("newData");
    public static String WfEventAudit = new String("WfEventAudit");
    public static String auditTime = new String("auditTime");
    public static String auditType = new String("auditType");
    public static String packageId = new String("packageId");
    public static String processId = new String("processId");
    public static String processName = new String("processName");
    public static String processDefId = new String("processDefId");
    public static String processDefName = new String("processDefName");
    public static String processDefVer = new String("processDefVer");
    public static String activityId = new String("activityId");
    public static String activityName = new String("activityName");
    public static String activityDefId = new String("activityDefId");
    public static String activitySetDefId = new String("activitySetDefId");
    public static String WfStateEventAudit = new String("WfStateEventAudit");
    public static String oldState = new String("oldState");
    public static String newState = new String("newState");
    public static String SharkGroup = new String("SharkGroup");
    public static String groupName = new String("groupName");
    public static String description = new String("description");
    public static String SharkGroupMember = new String("SharkGroupMember");
    public static String userName = new String("userName");
    public static String SharkGroupRollup = new String("SharkGroupRollup");
    public static String parentGroupName = new String("parentGroupName");
    public static String SharkUser = new String("SharkUser");
    public static String firstName = new String("firstName");
    public static String lastName = new String("lastName");
    public static String passwd = new String("passwd");
    public static String emailAddress = new String("emailAddress");
    public static String WfActivity = new String("WfActivity");
    public static String setDefinitionId = new String("setDefinitionId");
    public static String definitionId = new String("definitionId");
    public static String subFlowId = new String("subFlowId");
    public static String blockId = new String("blockId");
    public static String isSubAsync = new String("isSubAsync");
    public static String resourceUser = new String("resourceUser");
    public static String processMgrName = new String("processMgrName");
    public static String currentState = new String("currentState");
    public static String priority = new String("priority");
    public static String accepted = new String("accepted");
    public static String activatedTime = new String("activatedTime");
    public static String timeLimit = new String("timeLimit");
    public static String acceptedTime = new String("acceptedTime");
    public static String lastStateTime = new String("lastStateTime");
    public static String WfActivityVariable = new String("WfActivityVariable");
    public static String activityVariableId = new String("activityVariableId");
    public static String valueField = new String("valueField");
    public static String strValue = new String("strValue");
    public static String numValue = new String("numValue");
    public static String dblValue = new String("dblValue");
    public static String objValue = new String("objValue");
    public static String isModified = new String("isModified");
    public static String WfAndJoin = new String("WfAndJoin");
    public static String andJoinId = new String("andJoinId");
    public static String WfAssignment = new String("WfAssignment");
    public static String isValid = new String("isValid");
    public static String mgrName = new String("mgrName");
    public static String WfDeadline = new String("WfDeadline");
    public static String deadlineId = new String("deadlineId");
    public static String exceptionName = new String("exceptionName");
    public static String isExecuted = new String("isExecuted");
    public static String isSync = new String("isSync");
    public static String WfProcess = new String("WfProcess");
    public static String packageVer = new String("packageVer");
    public static String activityReqProcessId = new String("activityReqProcessId");
    public static String activityReqId = new String("activityReqId");
    public static String resourceReqId = new String("resourceReqId");
    public static String externalReq = new String("externalReq");
    public static String createdTime = new String("createdTime");
    public static String startedTime = new String("startedTime");
    public static String ExternalRequesterClassName = new String("ExternalRequesterClassName");
    public static String WfProcessMgr = new String("WfProcessMgr");
    public static String created = new String("created");
    public static String WfProcessVariable = new String("WfProcessVariable");
    public static String processVariableId = new String("processVariableId");
    public static String WfResource = new String("WfResource");
    public static String resourceName = new String("resourceName");
    public static String WfApplicationMap = new String("WfApplicationMap");
    public static String applicationDefId = new String("applicationDefId");
    public static String toolAgentName = new String("toolAgentName");
    public static String applicationName = new String("applicationName");
    public static String applicationMode = new String("applicationMode");
    public static String WfParticipantMap = new String("WfParticipantMap");
    public static String participantMapId = new String("participantMapId");
    public static String participantId = new String("participantId");
    public static String isGroupUser = new String("isGroupUser");
    public static String WfRepository = new String("WfRepository");
    public static String xpdlId = new String("xpdlId");
    public static String xpdlVersion = new String("xpdlVersion");
    public static String xpdlData = new String("xpdlData");
    public static String isHistorical = new String("isHistorical");
    public static String XPDLClassVersion = new String("XPDLClassVersion");
    public static String serializedPkg = new String("serializedPkg");
    public static String WfRepositoryRef = new String("WfRepositoryRef");
    public static String refXpdlId = new String("refXpdlId");
    public static String refNumber = new String("refNumber");
    public static String WfRequester = new String("WfRequester");
    public static String requesterId = new String("requesterId");
    public static String fromDate = new String("fromDate");
    public static String thruDate = new String("thruDate");
    public static String className = new String("className");
    public static String classData = new String("classData");
}

