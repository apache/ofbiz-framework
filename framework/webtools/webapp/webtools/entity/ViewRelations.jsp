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


<%@ page import="java.text.*, java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.entity.model.*, org.ofbiz.entity.util.*, org.ofbiz.entity.condition.*, org.ofbiz.entity.transaction.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<%  
  String rowClassResultIndex = "viewOneTR2";
  String rowClassResultHeader = "viewOneTR1";
  String rowClassResult1 = "viewManyTR1";
  String rowClassResult2 = "viewManyTR2";
  String rowClassResult = "";

   try {%>

<%String entityName=request.getParameter("entityName");%>
<%ModelReader reader = delegator.getModelReader();%>
<%ModelEntity modelEntity = reader.getModelEntity(entityName);%>

<%boolean hasViewPermission = security.hasEntityPermission("ENTITY_DATA", "_VIEW", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_VIEW", session);%>

<%if(hasViewPermission){%>
<div class="head1">Relations</div>
<div class="head2">For Entity: <%=entityName%></div>
<br/>
<div>
    <a href="<ofbiz:url>/FindGeneric?entityName=<%=entityName%>&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</ofbiz:url>" class="buttontext">Back To Find Screen</a>
</div>
<br/>

<table>
   <tr>
      <td class="<%=rowClassResultHeader%>"><b>Title</b></td>
      <td class="<%=rowClassResultHeader%>"><b>Related Entity</b></td>
      <td class="<%=rowClassResultHeader%>"><b>Relation Type</b></td>
      <td class="<%=rowClassResultHeader%>"><b>FK Name</b></td>
      <td class="<%=rowClassResultHeader%>"><b>Key Map</b></td>
   </tr>
   
<% for (Iterator rit = modelEntity.getRelationsIterator(); rit.hasNext(); ) {
    ModelRelation modelRelation = (ModelRelation) rit.next();%>
    <%rowClassResult=(rowClassResult==rowClassResult1?rowClassResult2:rowClassResult1);%>
    <tr class="<%=rowClassResult%>">
       <td><%=modelRelation.getTitle()%></td>
       <td><a href='<ofbiz:url>/FindGeneric?entityName=<%=modelRelation.getRelEntityName()%>&find=true&VIEW_SIZE=50&VIEW_INDEX=0</ofbiz:url>' class="buttontext"><%=modelRelation.getRelEntityName()%></a></td>
       <td> <%=modelRelation.getType()%></td>
       <td><%=modelRelation.getFkName()%></td>
       <td><% for (Iterator kit = modelRelation.getKeyMapsIterator(); kit.hasNext(); ) {
                  ModelKeyMap keyMap = (ModelKeyMap) kit.next(); %>
                <%=keyMap.getFieldName()%> -> <%=keyMap.getRelFieldName()%> <br/>
           <%}%>
    </tr>
<%}%>
</table>
<%} else {%>
  <h3>You do not have permission to view this page (<%=modelEntity.getPlainTableName()%>_ADMIN, or <%=modelEntity.getPlainTableName()%>_VIEW needed).</h3>
<%}%>

<%} catch (Exception e) { Debug.log(e); throw e; }%>
