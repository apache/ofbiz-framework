<%--
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
 * @author  Andy Zeneski (jaz@ofbiz.org)
 * @version 1.0
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
