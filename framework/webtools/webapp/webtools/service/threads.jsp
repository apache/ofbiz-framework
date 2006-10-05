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
<jsp:useBean id="dispatcher" type="org.ofbiz.service.LocalDispatcher" scope="request" />

<%if(security.hasPermission("WEBTOOLS_VIEW", session)) {%>

<div class="head2">Thread Pool List</div>
<a href="<ofbiz:url>/threadList</ofbiz:url>" class="buttontext">[Refresh]</a>&nbsp;
<a href="<ofbiz:url>/jobList</ofbiz:url>" class="buttontext">[Job List]</a>&nbsp;
<a href="<ofbiz:url>/serviceList</ofbiz:url>" class="buttontext">[Service Log]</a>&nbsp;
<a href="<ofbiz:url>/scheduleJob</ofbiz:url>" class="buttontext">[Schedule Job]</a>
<br/>

<br/>
<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">Thread</div></td>
    <td><div class="tableheadtext">Status</div></td>
    <td><div class="tableheadtext">Job</div></td>
    <td><div class="tableheadtext">Service</div></td>
	<td><div class="tableheadtext">Time (ms)</div></td>
    <%--<td>&nbsp;</td>--%>
  </tr>
  <%
    /*
    if (request.getParameter("killThread") != null) {
        String threadToKill = request.getParameter("killThread");
        dispatcher.getJobManager().killThread(threadToKill);
    }
    */
    List jobs = dispatcher.getJobManager().processList();
    if (jobs != null) {
        Iterator i = jobs.iterator();
        while (i.hasNext()) {
            Map job = (Map) i.next();
  %>
  <%
      String status = "Invalid Status";
      int state = ((Integer) job.get("status")).intValue();
      switch (state) {
          case 0 : status = "Sleeping"; break;
          case 1 : status = "Running"; break;
          case -1: status = "Shutting down"; break;
          default: status = "Invalid Status"; break;
      }
      String serviceName = (String) job.get("serviceName");
      String threadName = (String) job.get("threadName");
      String jobName = (String) job.get("jobName");
  %>
  <tr>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(threadName,"&nbsp;")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(status, "&nbsp;")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(jobName,"[none]")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(serviceName,"[none]")%></div></td>
    <td><div class="tabletext"><%=job.get("runTime")%></div></td>
    <%-- doesn't work <td align="center"><a href="<ofbiz:url>/threadList?killThread=<%=threadName%></ofbiz:url>" class="buttontext">[Kill]</a></td>--%>
  </tr>
  <%
        }
    }
  %>
</table>

<%}else{%>
  <hr/>
  <div>You do not have permission to use this page.</div>
<%}%>