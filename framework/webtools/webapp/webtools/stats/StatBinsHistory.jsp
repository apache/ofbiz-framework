<%
/**
 *  Title: View Server Stats Bins History
 *  Description: None
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones
 *@created    February 4, 2002
 *@version    1.0
 */
%> 

<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.webapp.stats.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>
<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />

<%
    String id = request.getParameter("statsId");
    String typeStr = request.getParameter("type");
    int type = -1;
    try {
        type = Integer.parseInt(typeStr);
    } catch (NumberFormatException e) {
        type = -1;
    }
%>
<h3 style='margin:0;'>Server Statistic Bins History Page</h3>
<div><a href="<ofbiz:url>/StatsSinceStart</ofbiz:url>" class='buttontext'>Stats Since Server Start</A></div>
<div class='tabletext'>Current Time: <%=UtilDateTime.nowTimestamp().toString()%></div>
<%if (security.hasPermission("SERVER_STATS_VIEW", session)) {%>
<%
  String rowColor1 = "viewManyTR2";
  String rowColor2 = "viewManyTR1";
  String rowColor = "";
%>

    <%
    LinkedList binList = null;
    if (type == ServerHitBin.REQUEST) {
        binList = (LinkedList) ServerHitBin.requestHistory.get(id);
    } else if (type == ServerHitBin.EVENT) {
        binList = (LinkedList) ServerHitBin.eventHistory.get(id);
    } else if (type == ServerHitBin.VIEW) {
        binList = (LinkedList) ServerHitBin.viewHistory.get(id);
    } else {%>
        <h3>The type specified (<%=typeStr%>) was not valid.</h3>
    <%}%>

    <%if (binList != null) {%>
        <TABLE border='0' cellpadding='2' cellspacing='2'>
          <TR class='viewOneTR1'>
            <TD><%=ServerHitBin.typeNames[type]%>&nbsp;ID</TD>
            <TD>Start</TD>
            <TD>Stop</TD>
            <TD>Mins</TD>
            <TD>Hits</TD>
            <TD>Min</TD>
            <TD>Avg</TD>
            <TD>Max</TD>
            <TD>Hits/Minute</TD>
          </TR>

          <%Iterator binIter = binList.iterator();%>
          <%if(binIter!=null && binIter.hasNext()){%>
            <%while(binIter.hasNext()){%>
              <%ServerHitBin bin = (ServerHitBin) binIter.next();%>
              <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%>
              <tr class="<%=rowColor%>">
                <TD><%=bin.getId()%></TD>
                <TD><%=bin.getStartTimeString()%></TD>
                <TD><%=bin.getEndTimeString()%></TD>
                <TD><%=UtilFormatOut.formatQuantity(bin.getBinLengthMinutes())%></TD>
                <TD><%=UtilFormatOut.formatQuantity(bin.getNumberHits())%></TD>
                <TD><%=UtilFormatOut.formatQuantity(bin.getMinTimeSeconds())%></TD>
                <TD><%=UtilFormatOut.formatQuantity(bin.getAvgTimeSeconds())%></TD>
                <TD><%=UtilFormatOut.formatQuantity(bin.getMaxTimeSeconds())%></TD>
                <TD><%=UtilFormatOut.formatQuantity(bin.getHitsPerMinute())%></TD>
                <%-- <TD><%=UtilFormatOut.formatQuantity(utilCache.getExpireTime())%></TD> --%>
              </TR>
            <%}%>
          <%}else{%>
              <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr bgcolor="<%=rowColor%>">
                <TD colspan="9">No View stats found.</TD>
              </TR>
          <%}%>
        </TABLE>
    <%}%>

<%}else{%>
  <h3>You do not have permission to view this page (SERVER_STATS_VIEW needed).</h3>
<%}%>
