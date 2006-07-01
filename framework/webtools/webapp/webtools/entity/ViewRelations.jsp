<%--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     <a href='mailto:sichen@opensourcestrategies.com'>Si Chen</a>
 *@created    July 5, 2005
 *@version    1.0
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
<h3>Relations</h3>
<p>for entity <a href='<ofbiz:url>/FindGeneric?entityName=<%=entityName%>&find=true&VIEW_SIZE=50&VIEW_INDEX=0</ofbiz:url>' class="buttonext"><%=entityName%></a></p>

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
       <td><a href='<ofbiz:url>/FindGeneric?entityName=<%=modelRelation.getRelEntityName()%>&find=true&VIEW_SIZE=50&VIEW_INDEX=0</ofbiz:url>'><%=modelRelation.getRelEntityName()%></a></td>
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
