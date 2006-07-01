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
 *@since      2.0
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
  <hr>
  <div>You do not have permission to use this page.</div>
<%}%>