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
