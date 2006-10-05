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

<%@ page import="java.util.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*" %>
<%@ page import="org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<%if(security.hasPermission("WEBTOOLS_VIEW", session)) {%>

<%
	List jobs = delegator.findAll("JobSandbox", UtilMisc.toList("-runTime"));
	if (jobs != null) pageContext.setAttribute("jobs", jobs);
%>
<div class="head2">Scheduled Jobs</div>
<a href="<ofbiz:url>/jobList</ofbiz:url>" class="buttontext">[Refresh]</a>&nbsp;
<a href="<ofbiz:url>/threadList</ofbiz:url>" class="buttontext">[Thread List]</a>
<a href="<ofbiz:url>/serviceList</ofbiz:url>" class="buttontext">[Service Log]</a>&nbsp;
<a href="<ofbiz:url>/scheduleJob</ofbiz:url>" class="buttontext">[Schedule Job]</a>
<br/>

<br/>
<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">Job</div></td>
    <td><div class="tableheadtext">Pool</div></td>
    <td><div class="tableheadtext">Run Time</div></td>
	<td><div class="tableheadtext">Start Time</div></td>
    <td><div class="tableheadtext">Service</div></td>
    <td><div class="tableheadtext">Finish Time</div></td>
    <td>&nbsp;</td>
  </tr>
  <ofbiz:iterator name="job" property="jobs">
  <%
      String finishTime = job.getString("finishDateTime");
      String cancelTime = job.getString("cancelDateTime");
      String endTime = finishTime;
      if (endTime == null) {
          endTime = cancelTime;
      }
      String serviceName = job.getString("serviceName");
  %>
  <tr>
    <td><a href="#" class="buttontext"><%=UtilFormatOut.checkNull(job.getString("jobName"),"&nbsp;")%></a></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(job.getString("poolId"), "&nbsp;")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(job.getString("runTime"),"&nbsp;")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(job.getString("startDateTime"),"&nbsp;")%></div></td>
    <td><div class="tabletext"><a href="<ofbiz:url>/availableServices?sel_service_name=<%=serviceName%></ofbiz:url>" class="buttontext"><%=UtilFormatOut.checkNull(serviceName,"&nbsp;")%></a></div></td>
    <td>
      <div class="tabletext">
        <%if (endTime != null && cancelTime != null && endTime.equals(cancelTime)) {%>
        <font color="red">
        <%}%>

        <%=UtilFormatOut.checkNull(endTime,"&nbsp;")%>

        <%if (endTime != null && cancelTime != null && endTime.equals(cancelTime)) {%>
        </font>
        <%}%>
      </div>
    </td>
    <%--<td align='center'><a href="#" class="buttontext">[View Data]</a></td>--%>
    <td align="center">
      <%if (job.getString("startDateTime") != null || endTime != null) {%>
      &nbsp;
      <%}else{%>
      <nobr><a href="<ofbiz:url>/cancelJob?jobId=<%=job.getString("jobId")%></ofbiz:url>" class="buttontext">[Cancel Job]</a></nobr>
      <%}%>
    </td>
  </tr>
  </ofbiz:iterator>
</table>

<%}else{%>
  <hr/>
  <div>You do not have permission to use this page.</div>
<%}%>
