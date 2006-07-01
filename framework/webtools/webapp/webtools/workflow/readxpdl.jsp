<%--
 *  Copyright (c) 2001-2005 The Open For Business Project and respective authors.
 
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
 * @author David E. Jones (jonesde@ofbiz.org)
 * @version 1.0
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

  <hr>

  <%if(messages.size() > 0) {%>
    <H4>The following occurred:</H4>
    <UL>
    <%Iterator errMsgIter = messages.iterator();%>
    <%while(errMsgIter.hasNext()) {%>
      <LI><%=errMsgIter.next()%>
    <%}%>
    </UL>
    <HR>
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
  <hr>
  <div>You do not have permission to use this page (WORKFLOW_MAINT needed)</div>
<%}%>
