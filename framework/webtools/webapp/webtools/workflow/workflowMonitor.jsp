<%--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
--%>

<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.entity.condition.*, org.ofbiz.entity.util.*, org.ofbiz.workflow.definition.*, org.ofbiz.workflow.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<%
	String workflow = request.getParameter("workflow");
	GenericValue workflowDef = null;
	if (workflow == null) {									
		List runningProcesses = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortTypeId", "WORK_FLOW", "currentStatusId", "WF_RUNNING"));
		if (runningProcesses != null)
			pageContext.setAttribute("runningProcesses", runningProcesses);
	} else {
        workflowDef = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workflow));
        if (workflowDef != null)
        	pageContext.setAttribute("workflow", workflowDef);
		List activities = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", workflow));
		if (activities != null)
			pageContext.setAttribute("activities", activities);
	}
%>
<div class="head2">Active Workflow Monitor</div>

<%if(security.hasPermission("WORKFLOW_MAINT", session)) {%>

<!-- list all running processes -->
<ofbiz:unless name="workflow">
  <ofbiz:unless name="runningProcesses">
     <div class="head1">No running processes</div>
  </ofbiz:unless>
  <ofbiz:if name="runningProcesses"> 
    <div>&nbsp;</div>
    <div class="tabletext">This page is used to view the status of running workflows.</div>  
    <table cellpadding="2" cellspacing="0" border="1" width="100%">
      <tr>
        <td><div class="tableheadtext">Package/Version</div></td>
	    <td><div class="tableheadtext">Process/Version</div></td>
        <td><div class="tableheadtext">Current Status</div></td>
        <td><div class="tableheadtext">Priority</div></td>
        <td><div class="tableheadtext">Actual StartDate</div></td>
        <td><div class="tableheadtext">Source Reference ID</div></td>
        <td>&nbsp;</td>
      </tr>
      <ofbiz:iterator name="runningProcess" property="runningProcesses">
        <tr>
          <td><div class="tabletext"><%=runningProcess.getString("workflowPackageId")%> / <%=runningProcess.getString("workflowPackageVersion")%></div></td>
          <td><div class="tabletext"><%=runningProcess.getString("workflowProcessId")%> / <%=runningProcess.getString("workflowProcessVersion")%></div></td>
          <td><div class="tabletext"><%=WfUtil.getOMGStatus(runningProcess.getString("currentStatusId"))%></div></td>
          <td><div class="tabletext"><%=UtilFormatOut.checkNull(runningProcess.getString("priority"), "&nbsp;")%></div></td>
          <td><div class="tabletext"><%=UtilFormatOut.checkNull(runningProcess.getString("actualStartDate"), "N/A")%></div></td>
          <td><div class="tabletext"><%=UtilFormatOut.checkNull(runningProcess.getString("sourceReferenceId"), "&nbsp;")%></div></td>
          <td align="center">&nbsp;<a href="<ofbiz:url>/workflowMonitor?workflow=<%=runningProcess.getString("workEffortId")%></ofbiz:url>" class="buttontext">[View]</a>&nbsp;</td>
        </tr>
      </ofbiz:iterator>
    </table>
  </ofbiz:if>
</ofbiz:unless>

<!-- list all steps in the process -->
<ofbiz:if name="workflow">
  <ofbiz:unless name="activities">
    <div class="head1">No steps found for running workflow process: <%=workflow%></div>
  </ofbiz:unless>
  <ofbiz:if name="activities">
    <div><a href="<ofbiz:url>/workflowMonitor</ofbiz:url>" class="buttontext">[Workflows]</a></div>
    <div>&nbsp;</div>
    <span class="head1">Activity list for:&nbsp;</span><span class="head2"><%=workflowDef.getString("workflowPackageId") + " / " + workflowDef.getString("workflowProcessId")%> 
    <table cellpadding="2" cellspacing="0" border="1" width="100%">
      <tr>
        <td><div class="tabletext"><b>Activity ID</b></div></td>
        <td><div class="tabletext"><b>Priority</b></div></td>
        <td><div class="tabletext"><b>Current Status</b></div></td>
        <td><div class="tabletext"><b>Actual StartDate</b></div></td>
        <td><div class="tabletext"><b>Actual CompleteDate</b></div></td>
        <td><div class="tabletext"><b>Assignment(s)</b></div></td>
      </tr>
      <ofbiz:iterator name="step" property="activities">
        <% 
            List assignments = step.getRelated("WorkEffortPartyAssignment");
			assignments = EntityUtil.filterByDate(assignments);
            Iterator assignmentIterator = assignments.iterator();
        %>
        
        <tr>
          <td><a href="/workeffort/control/activity?workEffortId=<%=step.getString("workEffortId")%>" target="workeffort" class="buttontext"><%=step.getString("workflowActivityId")%></a></td>
          <td><div class="tabletext"><%=UtilFormatOut.checkNull(step.getString("priority"), "&nbsp;")%></div></td>
          <td><div class="tabletext"><%=WfUtil.getOMGStatus(step.getString("currentStatusId"))%></div></td>
          <td><div class="tabletext"><%=UtilFormatOut.checkNull(step.getString("actualStartDate"), "N/A")%></div></td>
          <td><div class="tabletext"><%=UtilFormatOut.checkNull(step.getString("actualCompletionDate"),"N/A")%></div></td>
          <td nobr>
            <% if (!assignmentIterator.hasNext()) { %><span class="tabletext">N/A</span><%}%>
            <% while (assignmentIterator.hasNext()) { GenericValue assignment = (GenericValue) assignmentIterator.next(); %>
            <a href="/partymgr/control/viewprofile?party_id=<%=assignment.getString("partyId")%>" target="partymgr" class="buttontext"><%=assignment.getString("partyId")%></a>
            <% if (assignmentIterator.hasNext()) { %>,&nbsp;<%}%>
            <%}%>
          </td>                                                                    
        </tr>
      </ofbiz:iterator>
    </table>
  </ofbiz:if>
</ofbiz:if>

<%}else{%>
  <hr>
  <div>You do not have permission to use this page (WORKFLOW_MAINT needed)</div>
<%}%>
