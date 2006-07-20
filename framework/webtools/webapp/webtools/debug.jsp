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
<%@ page import="org.ofbiz.workflow.definition.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<h3>Update Debug Levels</h3>
<div>This page is used to set and update the debugging levels.</div>
<br/>

<%if(security.hasPermission("UTIL_DEBUG_EDIT", session)) {%>
  <FORM method='POST' action='<ofbiz:url>/updateDebugLevel</ofbiz:url>'>
    <table border='0'>
      <tr>
        <td align="right"><div class="tabletext">Verbose:</div></td>
        <td><input type="checkbox" name="verbose" value="true" <%=Debug.verboseOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Timing:</div></td>
        <td><input type="checkbox" name="timing" value="true" <%=Debug.timingOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Info:</div></td>
        <td><input type="checkbox" name="info" value="true" <%=Debug.infoOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Important:</div></td>
        <td><input type="checkbox" name="important" value="true" <%=Debug.importantOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Warning:</div></td>
        <td><input type="checkbox" name="warning" value="true" <%=Debug.warningOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Error:</div></td>
        <td><input type="checkbox" name="error" value="true" <%=Debug.errorOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">Fatal:</div></td>
        <td><input type="checkbox" name="fatal" value="true" <%=Debug.fatalOn() ? "checked" : ""%>></td>
      </tr>
      <tr>
        <td colspan="2">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="2" align="center"><input type="submit" value="Submit"></td>
      </tr>
    </table>
  </FORM>
<%}else{%>
  <hr>
  <div>You do not have permission to use this page (UTIL_DEBUG_EDIT needed)</div>
<%}%>
