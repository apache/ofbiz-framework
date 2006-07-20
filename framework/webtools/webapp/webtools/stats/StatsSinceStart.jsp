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
    if ("true".equals(request.getParameter("clear"))) {
        ServerHitBin.requestSinceStarted.clear();
        ServerHitBin.eventSinceStarted.clear();
        ServerHitBin.viewSinceStarted.clear();
    }
%>

<h3 style='margin:0;'>Server Statistics Since Start Page</h3>
<div><a href="<ofbiz:url>/StatsSinceStart?clear=true</ofbiz:url>" class='buttontext'>[Clear Since Start Stats]</A>
<a href="<ofbiz:url>/StatsSinceStart</ofbiz:url>" class='buttontext'>[Reload Page]</A></div>
<div class='tabletext'>Current Time: <%=UtilDateTime.nowTimestamp().toString()%></div>
<%if (security.hasPermission("SERVER_STATS_VIEW", session)) {%>
<%
  String rowColor1 = "viewManyTR2";
  String rowColor2 = "viewManyTR1";
  String rowColor = "";
%>

<%-- REQUEST --%>

<TABLE border='0' cellpadding='2' cellspacing='2'>
  <TR class='viewOneTR1'>
    <TD>Reqest&nbsp;ID</TD>
    <TD>Start</TD>
    <TD>Stop</TD>
    <TD>Mins</TD>
    <TD>Hits</TD>
    <TD>Min</TD>
    <TD>Avg</TD>
    <TD>Max</TD>
    <TD>Hits/Minute</TD>
    <TD>View&nbsp;Bins</TD>
  </TR>

  <%TreeSet requestIds = new TreeSet(ServerHitBin.requestSinceStarted.keySet());%>
  <%Iterator requestIdIter = requestIds.iterator();%>
  <%if(requestIdIter!=null && requestIdIter.hasNext()){%>
    <%while(requestIdIter.hasNext()){%>
      <%String statsId = (String)requestIdIter.next();%>
      <%ServerHitBin bin = (ServerHitBin) ServerHitBin.requestSinceStarted.get(statsId);%>
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
        <TD><a href='<ofbiz:url>/StatBinsHistory?statsId=<%=bin.getId()%>&type=<%=bin.getType()%></ofbiz:url>' class='buttontext'>View&nbsp;Bins</a></TD>
      </TR>
    <%}%>
  <%}else{%>
      <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr bgcolor="<%=rowColor%>">
        <TD colspan="9">No Request stats found.</TD>
      </TR>
  <%}%>
</TABLE>
<BR>
<%-- EVENT --%>

<TABLE border='0' cellpadding='2' cellspacing='2'>
  <TR class='viewOneTR1'>
    <TD>Event&nbsp;ID</TD>
    <TD>Start</TD>
    <TD>Stop</TD>
    <TD>Mins</TD>
    <TD>Hits</TD>
    <TD>Min</TD>
    <TD>Avg</TD>
    <TD>Max</TD>
    <TD>Hits/Minute</TD>
    <TD>View&nbsp;Bins</TD>
  </TR>

  <%TreeSet eventIds = new TreeSet(ServerHitBin.eventSinceStarted.keySet());%>
  <%Iterator eventIdIter = eventIds.iterator();%>
  <%if(eventIdIter!=null && eventIdIter.hasNext()){%>
    <%while(eventIdIter.hasNext()){%>
      <%String statsId = (String)eventIdIter.next();%>
      <%ServerHitBin bin = (ServerHitBin) ServerHitBin.eventSinceStarted.get(statsId);%>
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
        <TD><a href='<ofbiz:url>/StatBinsHistory?statsId=<%=bin.getId()%>&type=<%=bin.getType()%></ofbiz:url>' class='buttontext'>View&nbsp;Bins</a></TD>
      </TR>
    <%}%>
  <%}else{%>
      <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr bgcolor="<%=rowColor%>">
        <TD colspan="9">No Event stats found.</TD>
      </TR>
  <%}%>
</TABLE>
<BR>
<%-- VIEW --%>

<TABLE border='0' cellpadding='2' cellspacing='2'>
  <TR class='viewOneTR1'>
    <TD>View&nbsp;ID</TD>
    <TD>Start</TD>
    <TD>Stop</TD>
    <TD>Mins</TD>
    <TD>Hits</TD>
    <TD>Min</TD>
    <TD>Avg</TD>
    <TD>Max</TD>
    <TD>Hits/Minute</TD>
    <TD>View&nbsp;Bins</TD>
  </TR>

  <%TreeSet viewIds = new TreeSet(ServerHitBin.viewSinceStarted.keySet());%>
  <%Iterator viewIdIter = viewIds.iterator();%>
  <%if(viewIdIter!=null && viewIdIter.hasNext()){%>
    <%while(viewIdIter.hasNext()){%>
      <%String statsId = (String)viewIdIter.next();%>
      <%ServerHitBin bin = (ServerHitBin) ServerHitBin.viewSinceStarted.get(statsId);%>
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
        <TD><a href='<ofbiz:url>/StatBinsHistory?statsId=<%=bin.getId()%>&type=<%=bin.getType()%></ofbiz:url>' class='buttontext'>View&nbsp;Bins</a></TD>
      </TR>
    <%}%>
  <%}else{%>
      <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr bgcolor="<%=rowColor%>">
        <TD colspan="9">No View stats found.</TD>
      </TR>
  <%}%>
</TABLE>

<%}else{%>
  <h3>You do not have permission to view this page (SERVER_STATS_VIEW needed).</h3>
<%}%>
