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
<%@ page import="org.ofbiz.service.*, org.ofbiz.service.config.*, org.ofbiz.service.calendar.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<jsp:useBean id="dispatcher" type="org.ofbiz.service.LocalDispatcher" scope="request" />

<%if(security.hasPermission("WEBTOOLS_VIEW", session)) {%>

<%
	String serviceName = request.getParameter("SERVICE_NAME");
    String poolName = ServiceConfigUtil.getSendPool();
%>

<% if (serviceName == null) { %>
  <div class="head2">Schedule A Job</div>
  <div class="tabletext">Step 1: Service & Recurrence Information</div>
  <br/>

  <FORM name="scheduleForm" method='POST' action='<ofbiz:url>/scheduleJob</ofbiz:url>'>
    <table border='0'>
      <tr>
        <td align="right"><div class="tabletext">Service:</div></td>
        <td><input type="text" class="inputBox" size="20" name="SERVICE_NAME" value="<%=(serviceName != null ? serviceName : "")%>"></td>
      </tr>      
      <tr>
        <td align="right"><div class="tabletext">Pool Name:</div></td>
        <td><input type="text" class="inputBox" size="20" name="POOL_NAME" value="<%=poolName%>"></td>
      </tr>      
      <tr>
        <td align="right"><div class="tabletext">Start Date/Time:</div></td>
        <td>
          <input type="text" class="inputBox" size="25" name="SERVICE_TIME">
          <a href="javascript:call_cal(document.scheduleForm.SERVICE_TIME, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
        </td>
      </tr>    
      <tr>
        <td align="right"><div class="tabletext">Finish Date/Time:</div></td>
        <td>
          <input type="text" class="inputBox" size="25" name="SERVICE_END_TIME">
          <a href="javascript:call_cal(document.scheduleForm.SERVICE_END_TIME, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
        </td>
      </tr>   
      <tr>
        <td align="right"><div class="tabletext">Frequency:</div></td>
        <td>
          <select class="selectBox" name="SERVICE_FREQUENCY">    
            <option value="<%=RecurrenceRule.DAILY%>">None</option>                                                                    
            <option value="<%=RecurrenceRule.YEARLY%>">Yearly</option>
            <option value="<%=RecurrenceRule.MONTHLY%>">Monthly</option>
            <option value="<%=RecurrenceRule.WEEKLY%>">Weekly</option>
            <option value="<%=RecurrenceRule.DAILY%>">Daily</option>
            <option value="<%=RecurrenceRule.HOURLY%>">Hourly</option>
            <option value="<%=RecurrenceRule.MINUTELY%>">Minutely</option>
            <option value="<%=RecurrenceRule.SECONDLY%>">Secondly</option>

        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Interval:</div></td>
        <td><input type="text" class="inputBox" size="6" name="SERVICE_INTERVAL">&nbsp;<span class='tabletext'>(for use with frequency)</span></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Count:</div></td>
        <td><input type="text" class="inputBox" size="6" name="SERVICE_COUNT" value="1">&nbsp;<span class="tabletext">(number of time the job will run; use -1 for no limit i.e. forever)</span></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Max Retry:</div></td>
        <td><input type="text" class="inputBox" size="6" name="SERVICE_MAXRETRY" value="">&nbsp;<span class="tabletext">(number of time the job will retry on error; use -1 for no limit or leave empty for service default)</span></td>
      </tr>

      <tr>
        <td colspan="2">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="2" align="center"><input type="submit" class="smallSubmit" value="Submit"></td>
      </tr>
    </table>
  </FORM>
<%} else {%>
  <div class="head2">Schedule A Job</div>
  <div class="tabletext">Step 2: Service Parameters</div>
  <br/>

  <FORM name="scheduleForm" method='POST' action='<ofbiz:url>/scheduleService</ofbiz:url>'>
    <% 
  		Enumeration e = request.getParameterNames();  		
  		while (e.hasMoreElements()) {
  			String paramName = (String) e.nextElement();
  			String paramValue = request.getParameter(paramName);
    %>
	<input type='hidden' name='<%=paramName%>' value='<%=paramValue%>'>
	<%  } %>
	
	<%
		DispatchContext dctx = dispatcher.getDispatchContext();
		ModelService model = dctx.getModelService(serviceName);

	%>

    <table border='0'>
      <%
      	Iterator pi = model.getInParamNames().iterator();
      	while (pi.hasNext()) {
      		ModelParam par = model.getParam((String)pi.next());
      		if (par.internal) continue;
      %>
      <tr>
        <td align="right"><div class="tabletext"><%=par.name%> (<%=par.type%>)</div></td>
        <td>
          <input type="text" class="inputBox" size="20" name="<%=par.name%>">
          <span class="tabletext"><%if(!par.optional){%>(required)<%}else{%>(optional)<%}%>
        </td>
      </tr>
      <%}%>
      <tr>
        <td colspan="2">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="2" align="center"><input type="submit" class="smallSubmit" value="Submit"></td>
      </tr>      
    </table>
  </FORM>	
<%}%>
	
<%}else{%>
  <hr>
  <div>You do not have permission to use this page.</div>
<%}%>	
	