<%--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
--%>

<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.workflow.definition.*, org.ofbiz.entity.util.*, org.ofbiz.entity.condition.*, org.ofbiz.entity.transaction.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<%
    List messages = new LinkedList();
    
    String xpdlLoc = request.getParameter("XPDL_LOCATION");
    boolean xpdlIsUrl = request.getParameter("XPDL_IS_URL")!=null?true:false;
    boolean xpdlImport = request.getParameter("XPDL_IMPORT")!=null?true:false;
    
    URL xpdlUrl = null;
    try { xpdlUrl = xpdlIsUrl?new URL(xpdlLoc):UtilURL.fromFilename(xpdlLoc); }
    catch (java.net.MalformedURLException e) { messages.add(e.getMessage()); messages.add(e.toString()); Debug.log(e); }
    if (xpdlUrl == null) messages.add("Could not find file/URL: " + xpdlLoc);
    
    List toBeStored = null;
    try { if (xpdlUrl != null) toBeStored = XpdlReader.readXpdl(xpdlUrl, delegator); }
    catch (Exception e) { messages.add(e.getMessage()); messages.add(e.toString()); Debug.log(e); }
		
    if (toBeStored != null && xpdlImport) {
    	boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            delegator.storeAll(toBeStored);
            TransactionUtil.commit(beganTransaction);
            messages.add("Wrote/Updated " + toBeStored.size() + " toBeStored objects to the data source.");
        } catch (GenericEntityException e) {
            TransactionUtil.rollback(beganTransaction, "Error storing data from XPDL file", e);
            messages.add(e.getMessage()); messages.add(e.toString()); Debug.log(e);
        }
    }
%>
<h3>Read XPDL File</h3>
<div>This page is used to read and import XPDL files into the workflow process repository.</div>

<%if(security.hasPermission("WORKFLOW_MAINT", session)) {%>
  <FORM method="post" action='<ofbiz:url>/readxpdl</ofbiz:url>'>
    XPDL Filename or URL: <INPUT name='XPDL_LOCATION' class='inputBox' type="text" size='60' value='<%=UtilFormatOut.checkNull(xpdlLoc)%>'> Is URL?:<INPUT type="checkbox" name='XPDL_IS_URL' <%=xpdlIsUrl?"checked":""%>><BR>
    Import/Update to DB?:<INPUT type="checkbox" name='XPDL_IMPORT'> <INPUT type="submit" class=smallSubmit value='View'>
  </FORM>

  <hr/>

  <%if(messages.size() > 0) {%>
    <H4>The following occurred:</H4>
    <UL>
    <%Iterator errMsgIter = messages.iterator();%>
    <%while(errMsgIter.hasNext()) {%>
      <LI><%=errMsgIter.next()%>
    <%}%>
    </UL>
    <hr/>
  <%}%>

    <%Iterator viter = UtilMisc.toIterator(toBeStored);%>
    <%while (viter != null && viter.hasNext()) {%>
        <PRE><%=viter.next().toString()%></PRE>
    <%}%>

    <%if (toBeStored != null) {%>
        <div>Read and printed <%=toBeStored.size()%> entities.</div>
    <%} else {%>
        <div>No toBeStored read.</div>
    <%}%>
    
    
<%}else{%>
  <hr/>
  <div>You do not have permission to use this page (WORKFLOW_MAINT needed)</div>
<%}%>
