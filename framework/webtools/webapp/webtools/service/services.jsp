<%--
 *  Description: None
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski
 *@version    $Rev$
 *@since      3.3
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
  <hr>
  <div>You do not have permission to use this page.</div>
<%}%>