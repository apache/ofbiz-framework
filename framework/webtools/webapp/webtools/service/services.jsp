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

<%@ page import="java.util.*,
                 org.ofbiz.service.ServiceDispatcher,
                 org.ofbiz.service.RunningService,
                 org.ofbiz.service.engine.GenericEngine,
                 java.sql.Timestamp" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*" %>
<%@ page import="org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="dispatcher" type="org.ofbiz.service.LocalDispatcher" scope="request" />

<%if(security.hasPermission("WEBTOOLS_VIEW", session)) {%>

<div class="head2">Service Log</div>
<a href="<ofbiz:url>/serviceList</ofbiz:url>" class="buttontext">[Refresh]</a>&nbsp;
<a href="<ofbiz:url>/jobList</ofbiz:url>" class="buttontext">[Job List]</a>&nbsp;
<a href="<ofbiz:url>/threadList</ofbiz:url>" class="buttontext">[Thread List]</a>&nbsp;
<a href="<ofbiz:url>/scheduleJob</ofbiz:url>" class="buttontext">[Schedule Job]</a>
<br/>

<br/>
<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">Service Name</div></td>
    <td><div class="tableheadtext">Dispatcher Name</div></td>
    <td><div class="tableheadtext">Mode</div></td>
    <td><div class="tableheadtext">Start Time</div></td>
	<td><div class="tableheadtext">End Time</div></td>
    <%--<td>&nbsp;</td>--%>
  </tr>
  <%

    Map log = ServiceDispatcher.getServiceLogMap();
    if (log != null) {
        Iterator i = log.keySet().iterator();
        while (i.hasNext()) {
            RunningService rs = (RunningService) i.next();
  %>
  <%
      int mode = rs.getMode();
      String modeStr = "";
      if (mode == GenericEngine.SYNC_MODE) {
          modeStr = "SYNC";
      } else {
          modeStr = "ASYNC";
      }

      String serviceName = rs.getModelService().name;
      String localName = rs.getLocalName();
      Timestamp startTime = rs.getStartStamp();
      Timestamp endTime = rs.getEndStamp();
  %>
  <tr>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(serviceName,"&nbsp;")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(localName, "&nbsp;")%></div></td>
    <td><div class="tabletext"><%=UtilFormatOut.checkNull(modeStr,"[none]")%></div></td>
    <td><div class="tabletext"><%=startTime%></div></td>
    <%if (endTime != null) {%>
    <td><div class="tabletext"><%=endTime%></div></td>
    <%}else{%>
    <td><div class="tabletext">[running]</div></td>
    <%}%>
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