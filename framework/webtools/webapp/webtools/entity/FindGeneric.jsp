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
<%try {%>

<%String entityName=request.getParameter("entityName");%>
<%ModelReader reader = delegator.getModelReader();%>
<%ModelEntity modelEntity = reader.getModelEntity(entityName);%>

<%boolean hasViewPermission = security.hasEntityPermission("ENTITY_DATA", "_VIEW", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_VIEW", session);%>
<%boolean hasCreatePermission = security.hasEntityPermission("ENTITY_DATA", "_CREATE", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_CREATE", session);%>
<%boolean hasUpdatePermission = security.hasEntityPermission("ENTITY_DATA", "_UPDATE", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_UPDATE", session);%>
<%boolean hasDeletePermission = security.hasEntityPermission("ENTITY_DATA", "_DELETE", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_DELETE", session);%>
<%if(hasViewPermission){%>
<%
  String rowClassTop1 = "viewOneTR1";
  String rowClassTop2 = "viewOneTR2";
  String rowClassTop = "";
  String rowClassResultIndex = "viewOneTR2";
  String rowClassResultHeader = "viewOneTR1";
  String rowClassResult1 = "viewManyTR1";
  String rowClassResult2 = "viewManyTR2";
  String rowClassResult = "";

  String find = request.getParameter("find");
  if (find == null) find="false";
  String curFindString = "entityName=" + entityName + "&amp;find=" + find;
  GenericEntity findByEntity = delegator.makeValue(entityName, null);
  for (int fnum=0; fnum < modelEntity.getFieldsSize(); fnum++) {
    ModelField field = modelEntity.getField(fnum);
    String fval = request.getParameter(field.getName());
    if (fval != null) {
      if (fval.length() > 0) {
        curFindString = curFindString + "&amp;" + field.getName() + "=" + fval;
        findByEntity.setString(field.getName(), fval);
      }
    }
  }
  curFindString = UtilFormatOut.encodeQuery(curFindString);

%>
<%
//--------------
  String viewIndexString = (String)request.getParameter("VIEW_INDEX");
  if (viewIndexString == null || viewIndexString.length() == 0) { viewIndexString = "0"; }
  int viewIndex = 0;
  try { viewIndex = Integer.valueOf(viewIndexString).intValue(); }
  catch (NumberFormatException nfe) { viewIndex = 0; }

  String viewSizeString = (String)request.getParameter("VIEW_SIZE");
  if (viewSizeString == null || viewSizeString.length() == 0) { viewSizeString = "10"; }
  int viewSize = 10;
  try { viewSize = Integer.valueOf(viewSizeString).intValue(); }
  catch (NumberFormatException nfe) { viewSize = 10; }

  int lowIndex = viewIndex*viewSize+1;
  int highIndex = (viewIndex+1)*viewSize;
  int arraySize = 0;
  List resultPartialList = null;
//--------------
  if ("true".equals(find)) {
    EntityCondition condition = new EntityFieldMap(findByEntity, EntityOperator.AND);
    arraySize = (int) delegator.findCountByCondition(findByEntity.getEntityName(), condition, null);
    if (arraySize < highIndex) highIndex = arraySize;
    if ((highIndex - lowIndex + 1) > 0) {
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            EntityFindOptions efo = new EntityFindOptions();
            efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
            EntityListIterator resultEli = null;
            //new ArrayList(findByEntity.getPrimaryKey().keySet())
            resultEli = delegator.findListIteratorByCondition(findByEntity.getEntityName(), condition, null, null, null, efo);
            resultPartialList = resultEli.getPartialList(lowIndex, highIndex - lowIndex + 1);
            resultEli.close();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "FindGeneric.jsp");
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error looking up entity values in WebTools Entity Data Maintenance", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), "FindGeneric.jsp");
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }
  }
//--------------
  Debug.log("viewIndex=" + viewIndex + " lowIndex=" + lowIndex + " highIndex=" + highIndex + " arraySize=" + arraySize);
%>
<div class="head1">Find Values</div>
<div class="head2">For Entity: <%=modelEntity.getEntityName()%></div>
<br/>
<div>
    <a href="<ofbiz:url>/entitymaint</ofbiz:url>" class="buttontext">Back To Entity List</a>
</div>
<div>
    <a href="<ofbiz:url>/ViewRelations?entityName=<%=entityName%></ofbiz:url>" class="buttontext">View Relations</a>
    <a href="<ofbiz:url>/FindGeneric?entityName=<%=entityName%>&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</ofbiz:url>" class="buttontext">Find All</a>
    <%if (hasCreatePermission) {%>
    <a href="<ofbiz:url>/ViewGeneric?entityName=<%=entityName%></ofbiz:url>" class="buttontext">Create New</a>
    <%}%>
</div>
<br/>
<div class="tabletext">To find ALL <%=modelEntity.getEntityName()%>s, leave all entries blank.</div>
<%--<div class="tabletext">You may use the '&' character as a wildcard for String fields.</div>--%>
<form method="post" action="<ofbiz:url>/FindGeneric?entityName=<%=entityName%></ofbiz:url>" style="margin:0;">
<input type="hidden" name="find" value="true">
<table border="1" cellpadding="2" cellspacing="0">
    <tr class="viewOneTR1">
      <td valign="top">
        <span class="tableheadtext">Field Name</span>
      </td>
      <td valign="top">
        <span class="tableheadtext">PK</span>
      </td>
      <td valign="top">
        <span class="tableheadtext">Field Type</span>
      </td>
      <td valign="top">
        <span class="tableheadtext"><input type="submit" value="Find" class="smallSubmit"></span>
      </td>
    </tr>
  <%for (int fnum=0; fnum<modelEntity.getFieldsSize(); fnum++) {%>
    <%ModelField field = modelEntity.getField(fnum);%>
    <%ModelFieldType type = delegator.getEntityFieldType(modelEntity, field.getType());%>
    <tr>
      <td valign="top">
        <span class="tableheadtext"><%=field.getName()%></span>
      </td>
      <td valign="top">
        <span class="tabletext"><%if (field.getIsPk()) {%>*<%}%>&nbsp;</span>
      </td>
      <td valign="top">
        <span class="tabletext"><%=type.getJavaType()%>,<%=type.getSqlType()%></span>
      </td>
      <td valign="top">
        <input type="text" name="<%=field.getName()%>" value="<%=(request.getParameter(field.getName()) != null? request.getParameter(field.getName()): "")%>" size="40" class="inputBox">
      </td>
    </tr>
  <%}%>
    <tr>
      <td valign="top" align="center" colspan="4"><input type="submit" value="Find" class="smallSubmit"></td>
  </tr>
</table>
</form>

<br/>
<%if (hasCreatePermission) {%>
  <a href='<ofbiz:url>/ViewGeneric?entityName=<%=entityName%></ofbiz:url>' class="buttontext">Create New <%=modelEntity.getEntityName()%></a>
<%}%>
<table border="0" width="100%" cellpadding="2">
<% if (arraySize > 0) { %>
    <tr>
      <td align="left">
        <span class="tableheadtext">
        <% if(viewIndex > 0) { %>
          <a href='<ofbiz:url>/FindGeneric?<%=curFindString%>&amp;VIEW_SIZE=<%=viewSize%>&amp;VIEW_INDEX=<%=(viewIndex-1)%></ofbiz:url>' class="buttontext">Previous</a> |
        <% } %>
        <% if(arraySize > 0) { %>
          <%=lowIndex%> - <%=highIndex%> of <%=arraySize%>
        <% } %>
        <% if(arraySize>highIndex) { %>
          | <a href='<ofbiz:url>/FindGeneric?<%=curFindString%>&amp;VIEW_SIZE=<%=viewSize%>&amp;VIEW_INDEX=<%=(viewIndex+1)%></ofbiz:url>' class="buttontext">Next</a>
        <% } %>
        </span>
      </td>
    </tr>
<%}%>
</table>

  <table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
    <tr class="viewOneTR1">
      <td>&nbsp;</td>
      <%if (hasDeletePermission) {%>
        <td>&nbsp;</td>
      <%}%>
    <%for (int fnum = 0; fnum < modelEntity.getFieldsSize(); fnum++) {%>
      <%ModelField field = modelEntity.getField(fnum);%>
      <td nowrap><span class="tableheadtext"><%=field.getName()%></span></td>
    <%}%>
    </tr>
<%
 if (resultPartialList != null) {
  //int loopIndex = lowIndex;
  Iterator resultPartialIter = resultPartialList.iterator();
  while (resultPartialIter.hasNext()) {
    GenericValue value = (GenericValue) resultPartialIter.next();
%>
    <%rowClassResult=(rowClassResult==rowClassResult1?rowClassResult2:rowClassResult1);%>
    <tr>
      <td>
        <%
          String findString = "entityName=" + entityName;
          for (int pknum = 0; pknum < modelEntity.getPksSize(); pknum++) {
            ModelField pkField = modelEntity.getPk(pknum);
            ModelFieldType type = delegator.getEntityFieldType(modelEntity, pkField.getType());
            findString += "&amp;" + pkField.getName() + "=" + value.get(pkField.getName());
          }
        %>
        <a href='<ofbiz:url>/ViewGeneric?<%=findString%></ofbiz:url>' class="buttontext">View</a>
      </td>
      <%if (hasDeletePermission) {%>
        <td>
          <a href='<ofbiz:url>/UpdateGeneric?<%=findString%>&amp;UPDATE_MODE=DELETE&amp;<%=curFindString%></ofbiz:url>' class="buttontext">Delete</a>
        </td>
      <%}%>
    <%for (int fnum = 0; fnum < modelEntity.getFieldsSize(); fnum++) {%>
      <%ModelField field = modelEntity.getField(fnum);%>
      <%ModelFieldType type = delegator.getEntityFieldType(modelEntity, field.getType());%>
      <td>
        <div class="tabletext">
      <%if(type.getJavaType().equals("Timestamp") || type.getJavaType().equals("java.sql.Timestamp")) {%>
        <%java.sql.Timestamp dtVal = value.getTimestamp(field.getName());%>
        <%=dtVal==null?"":dtVal.toString()%>
      <%} else if(type.getJavaType().equals("Date") || type.getJavaType().equals("java.sql.Date")) {%>
        <%java.sql.Date dateVal = value.getDate(field.getName());%>
        <%=dateVal==null?"":dateVal.toString()%>
      <%} else if(type.getJavaType().equals("Time") || type.getJavaType().equals("java.sql.Time")) {%>
        <%java.sql.Time timeVal = value.getTime(field.getName());%>
        <%=timeVal==null?"":timeVal.toString()%>
      <%} else if(type.getJavaType().indexOf("Integer") >= 0) {%>
        <%=UtilFormatOut.safeToString((Integer)value.get(field.getName()))%>
      <%} else if(type.getJavaType().indexOf("Long") >= 0) {%>
        <%=UtilFormatOut.safeToString((Long)value.get(field.getName()))%>
      <%} else if(type.getJavaType().indexOf("Double") >= 0) {%>
        <%=UtilFormatOut.safeToString((Double)value.get(field.getName()))%>
      <%} else if(type.getJavaType().indexOf("Float") >= 0) {%>
        <%=UtilFormatOut.safeToString((Float)value.get(field.getName()))%>
      <%} else if(type.getJavaType().indexOf("String") >= 0) {%>
        <%=UtilFormatOut.checkNull((String)value.get(field.getName()))%>
      <%}%>
        &nbsp;</div>
      </td><%}%>
    </tr>
  <%}%>
<%
 } else {
%>
<%rowClassResult=(rowClassResult==rowClassResult1?rowClassResult2:rowClassResult1);%>
<tr>
<td colspan="<%=modelEntity.getFieldsSize() + 2%>">
<div class="head2">No <%=modelEntity.getEntityName()%> records Found.</div>
</td>
</tr>
<%}%>
</table>

<table border="0" width="100%" cellpadding="2">
<% if (arraySize > 0) { %>
    <tr>
      <td align="left">
        <span class="tableheadtext">
        <% if (viewIndex > 0) { %>
          <a href='<ofbiz:url>/FindGeneric?<%=curFindString%>&amp;VIEW_SIZE=<%=viewSize%>&amp;VIEW_INDEX=<%=(viewIndex-1)%></ofbiz:url>' class="buttontext">Previous</a> |
        <% } %>
        <% if (arraySize > 0) { %>
          <%=lowIndex%> - <%=highIndex%> of <%=arraySize%>
        <% } %>
        <% if (arraySize>highIndex) { %>
          | <a href='<ofbiz:url>/FindGeneric?<%=curFindString%>&amp;VIEW_SIZE=<%=viewSize%>&amp;VIEW_INDEX=<%=(viewIndex+1)%></ofbiz:url>' class="buttontext">Next</a>
        <% } %>
        </span>
      </td>
    </tr>
<%}%>
</table>
<%if (hasCreatePermission){%>
  <a href='<ofbiz:url>/ViewGeneric?entityName=<%=entityName%></ofbiz:url>' class="buttontext">Create New <%=modelEntity.getEntityName()%></a>
<%}%>
<%} else {%>
  <div class="head2">You do not have permission to view this page (<%=modelEntity.getPlainTableName()%>_ADMIN, or <%=modelEntity.getPlainTableName()%>_VIEW needed).</div>
<%}%>
<%} catch (Exception e) { Debug.log(e); throw e; }%>
