<%--
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
 *@author     <a href='mailto:jonesde@ofbiz.org'>David E. Jones (jonesde@ofbiz.org)</a>
 *@created    Aug 18 2001
 *@version    1.0
--%>

<%@ page import="java.text.*, java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.entity.model.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<%try {%>

<%String entityName = request.getParameter("entityName");%>
<%ModelReader reader = delegator.getModelReader();%>
<%ModelEntity entity = reader.getModelEntity(entityName);%>

<%boolean hasAllView = security.hasEntityPermission("ENTITY_DATA", "_VIEW", session);%>
<%boolean hasAllCreate = security.hasEntityPermission("ENTITY_DATA", "_CREATE", session);%>
<%boolean hasAllUpdate = security.hasEntityPermission("ENTITY_DATA", "_UPDATE", session);%>
<%boolean hasAllDelete = security.hasEntityPermission("ENTITY_DATA", "_DELETE", session);%>

<%boolean hasViewPermission = hasAllView || security.hasEntityPermission(entity.getPlainTableName(), "_VIEW", session);%>
<%boolean hasCreatePermission = hasAllCreate || security.hasEntityPermission(entity.getPlainTableName(), "_CREATE", session);%>
<%boolean hasUpdatePermission = hasAllUpdate || security.hasEntityPermission(entity.getPlainTableName(), "_UPDATE", session);%>
<%boolean hasDeletePermission = hasAllDelete || security.hasEntityPermission(entity.getPlainTableName(), "_DELETE", session);%>
<%if(hasViewPermission){%>

<%
  boolean useValue = true;

  String rowClass1 = "viewOneTR1";
  String rowClass2 = "viewOneTR2";
  String rowClass = "";
  String curFindString = "entityName=" + entityName;
  GenericPK findByPK = delegator.makePK(entityName, null);
  for(int fnum = 0; fnum < entity.getPksSize(); fnum++) {
    ModelField field = entity.getPk(fnum);
    ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());
    String fval = request.getParameter(field.getName());
    if(fval != null && fval.length() > 0) {
      curFindString = curFindString + "&" + field.getName() + "=" + fval;
      findByPK.setString(field.getName(), fval);
    }
  }
  curFindString = UtilFormatOut.encodeQuery(curFindString);

  GenericValue value = null;
  //only try to find it if this is a valid primary key...
  if(findByPK.isPrimaryKey()) value = delegator.findByPrimaryKey(findByPK);
  if(value == null) useValue = false;
%>
<br/>
<STYLE>
  .topouter { overflow: visible; border-style: none; }
  .topcontainer { POSITION: absolute; VISIBILITY: visible; width: 90%; border-style: none; }
  .topcontainerhidden { POSITION: absolute; VISIBILITY: hidden; }
</STYLE>
<script language="JavaScript" type="text/javascript">  
var numTabs=<%=entity.getRelationsSize()+2%>;
function ShowTab(lname) {
  for(inc=1; inc <= numTabs; inc++) {
    document.getElementById('tab' + inc).className = (lname == 'tab' + inc) ? 'ontab' : 'offtab';
    document.getElementById('lnk' + inc).className = (lname == 'tab' + inc) ? 'onlnk' : 'offlnk';
    document.getElementById('area' + inc).className = (lname == 'tab' + inc) ? 'topcontainer' : 'topcontainerhidden';
  }
}
</script>
<div style='color: white; background-color: black; padding:3;'>
  <b>View Entity: <%=entityName%> with PK: <%=findByPK.toString()%></b>
</div>

<a href='<ofbiz:url>/FindGeneric?entityName=<%=entityName%></ofbiz:url>' class="buttontext">[Find <%=entityName%>]</a>
<%if (hasCreatePermission) {%>
  <a href='<ofbiz:url>/ViewGeneric?entityName=<%=entityName%></ofbiz:url>' class="buttontext">[Create New <%=entityName%>]</a>
<%}%>
<%if (value != null) {%>
  <%if (hasDeletePermission) {%>
    <a href='<ofbiz:url>/UpdateGeneric?UPDATE_MODE=DELETE&<%=curFindString%></ofbiz:url>' class="buttontext">[Delete this <%=entityName%>]</a>
  <%}%>
<%}%>
<br/>
<br/>
<table cellpadding='0' cellspacing='0'><tr>  
  <td id='tab1' class='ontab'>
    <a href='javascript:ShowTab("tab1")' id=lnk1 class=onlnk>View <%=entityName%></a>
  </td>
  <%if (hasUpdatePermission || hasCreatePermission) {%>
  <td id='tab2' class='offtab'>
    <a href='javascript:ShowTab("tab2")' id=lnk2 class=offlnk>Edit <%=entityName%></a>
  </td>
  <%}%>
</tr>
<%if (value != null) {%>
<tr>
  <%for(int tabIndex = 0; tabIndex < entity.getRelationsSize(); tabIndex++){%>
    <%ModelRelation relation = entity.getRelation(tabIndex);%>
    <%ModelEntity relatedEntity = reader.getModelEntity(relation.getRelEntityName());%>
    <%if (hasAllView || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_VIEW", session)) {%>
      <td id='tab<%=tabIndex+3%>' class='offtab'>
        <a href='javascript:ShowTab("tab<%=tabIndex+3%>")' id='lnk<%=tabIndex+3%>' class='offlnk' style='FONT-SIZE: xx-small;'>
          <%=relation.getTitle()%><%=relation.getRelEntityName()%></a><SPAN class='tabletext' style='FONT-SIZE: xx-small;'>(<%=relation.getType()%>)</SPAN>
      </td>
    <%}%>
    <%if ((tabIndex+1)%4 == 0) {%></tr><tr><%}%>
  <%}%>
</tr>
<%}%>
</table>
<div class='topouter'>
  <DIV id='area1' class='topcontainer' width="1%">

<table border="0" cellspacing="2" cellpadding="2">
<%if (value == null) {%>
<tr class="<%=rowClass1%>"><td><h3>Specified <%=entityName%> was not found.</h3></td></tr>
<%} else {%> 
    <%for (int fnum = 0; fnum < entity.getFieldsSize(); fnum++) {%>
      <%ModelField field = entity.getField(fnum);%>
      <%ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());%>
    <%rowClass=(rowClass==rowClass1?rowClass2:rowClass1);%><tr class="<%=rowClass%>">
      <td valign="top"><div class="tabletext"><b><%=field.getName()%></b></div></td>
      <td valign="top">
        <div class="tabletext">
      <%if(type.getJavaType().equals("Timestamp") || type.getJavaType().equals("java.sql.Timestamp")){%>
        <%java.sql.Timestamp dtVal = value.getTimestamp(field.getName());%>
        <%=dtVal==null?"":dtVal.toString()%>
      <%} else if(type.getJavaType().equals("Date") || type.getJavaType().equals("java.sql.Date")){%>
        <%java.sql.Date dateVal = value.getDate(field.getName());%>
        <%=dateVal==null?"":dateVal.toString()%>
      <%} else if(type.getJavaType().equals("Time") || type.getJavaType().equals("java.sql.Time")){%>
        <%java.sql.Time timeVal = value.getTime(field.getName());%>
        <%=timeVal==null?"":timeVal.toString()%>
      <%}else if(type.getJavaType().indexOf("Integer") >= 0){%>
        <%=UtilFormatOut.safeToString((Integer)value.get(field.getName()))%>
      <%}else if(type.getJavaType().indexOf("Long") >= 0){%>
        <%=UtilFormatOut.safeToString((Long)value.get(field.getName()))%>
      <%}else if(type.getJavaType().indexOf("Double") >= 0){%>
        <%=UtilFormatOut.safeToString((Double)value.get(field.getName()))%>
      <%}else if(type.getJavaType().indexOf("Float") >= 0){%>
        <%=UtilFormatOut.safeToString((Float)value.get(field.getName()))%>
      <%}else if(type.getJavaType().indexOf("String") >= 0){%>
        <%=UtilFormatOut.checkNull((String)value.get(field.getName()))%>
      <%}%>
        &nbsp;</div>
      </td>
    </tr>
    <%}%>
<%} //end if value == null %>
</table>
  </div>

<%GenericValue valueSave = value;%>
<%if(hasUpdatePermission || hasCreatePermission){%>
  <DIV id='area2' class='topcontainerhidden' width="1%">
<%boolean showFields = true;%>
<%if(value == null && (findByPK.getAllFields().size() > 0)){%>
    <%=entity.getEntityName()%> with primary key <%=findByPK.toString()%> not found.<br/>
<%}%>
<%
  String lastUpdateMode = request.getParameter("UPDATE_MODE");
  if((session.getAttribute("_ERROR_MESSAGE_") != null || request.getAttribute("_ERROR_MESSAGE_") != null) && 
      lastUpdateMode != null && !lastUpdateMode.equals("DELETE")) {
    //if we are updating and there is an error, don't use the entity data for the fields, use parameters to get the old value
    useValue = false;
  }
%>
<form action='<ofbiz:url>/UpdateGeneric?entityName=<%=entityName%></ofbiz:url>' method="POST" name="updateForm" style="margin:0;">
<table cellpadding="2" cellspacing="2" border="0">

<%if (value == null) {%>
  <%if (hasCreatePermission) {%>
    You may create a <%=entityName%> by entering the values you want, and clicking Update.
    <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
    <%for (int fnum = 0; fnum < entity.getPksSize();fnum++) {%>
      <%ModelField field = entity.getPk(fnum);%>
      <%ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());%>
    <%rowClass=(rowClass==rowClass1?rowClass2:rowClass1);%><tr class="<%=rowClass%>">
      <td valign="top"><div class="tabletext"><b><%=field.getName()%></b></div></td>
      <td valign="top">
        <div class="tabletext">
      <%if (type.getJavaType().equals("Timestamp") || type.getJavaType().equals("java.sql.Timestamp")) {%>
        <%
          String dateTimeString = null;
          if (findByPK != null && useValue) {
            java.sql.Timestamp dtVal = findByPK.getTimestamp(field.getName());
            if(dtVal != null) {
              dateTimeString = dtVal.toString();
            }
          } else if (!useValue) {
            dateTimeString = request.getParameter(field.getName());
          }
        %>
        DateTime(YYYY-MM-DD HH:mm:SS.sss):<input class='editInputBox' type="text" name="<%=field.getName()%>" size="24" value="<%=UtilFormatOut.checkNull(dateTimeString)%>">
        <a href="javascript:call_cal(document.updateForm.<%=field.getName()%>, '<%=UtilFormatOut.checkNull(dateTimeString)%>');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
      <%} else if(type.getJavaType().equals("Date") || type.getJavaType().equals("java.sql.Date")){%>
        <%
          String dateString = null;
          if (findByPK != null && useValue) {
            java.sql.Date dateVal = value.getDate(field.getName());
            dateString = dateVal==null?"":dateVal.toString();
          } else if (!useValue) {
            dateString = request.getParameter(field.getName());
          }
        %>
        Date(YYYY-MM-DD):<input class='editInputBox' type="text" name="<%=field.getName()%>" size="11" value="<%=UtilFormatOut.checkNull(dateString)%>">
        <a href="javascript:call_cal(document.updateForm.<%=field.getName()%>, '<%=UtilFormatOut.checkNull(dateString)%>');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
      <%} else if(type.getJavaType().equals("Time") || type.getJavaType().equals("java.sql.Time")){%>
        <%
          String timeString = null;
          if (findByPK != null && useValue) {
            java.sql.Time timeVal = value.getTime(field.getName());
            timeString = timeVal==null?"":timeVal.toString();
          } else if (!useValue) {
            timeString = request.getParameter(field.getName());
          }
        %>
        Time(HH:mm:SS.sss):<input class='editInputBox' type="text" size="6" maxlength="10" name="<%=field.getName()%>" value="<%=UtilFormatOut.checkNull(timeString)%>">
      <%}else if(type.getJavaType().indexOf("Integer") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(findByPK!=null&&useValue)?UtilFormatOut.safeToString((Integer)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%>">
      <%}else if(type.getJavaType().indexOf("Long") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(findByPK!=null&&useValue)?UtilFormatOut.safeToString((Long)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%>"> 
      <%}else if(type.getJavaType().indexOf("Double") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(findByPK!=null&&useValue)?UtilFormatOut.safeToString((Double)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%>"> 
      <%}else if(type.getJavaType().indexOf("Float") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(findByPK!=null&&useValue)?UtilFormatOut.safeToString((Float)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%>">
      <%}else if(type.getJavaType().indexOf("String") >= 0){%>
        <%if(type.stringLength() <= 80){%>
        <input class='editInputBox' type="text" size="<%=type.stringLength()%>" maxlength="<%=type.stringLength()%>" name="<%=field.getName()%>" value="<%=(findByPK!=null&&useValue)?UtilFormatOut.checkNull((String)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%>">
        <%} else if(type.stringLength() <= 255){%>
          <input class='editInputBox' type="text" size="80" maxlength="<%=type.stringLength()%>" name="<%=field.getName()%>" value="<%=(findByPK!=null&&useValue)?UtilFormatOut.checkNull((String)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%>">
        <%} else {%>
          <textarea cols="60" rows="3" maxlength="<%=type.stringLength()%>" name="<%=field.getName()%>"><%=(findByPK!=null&&useValue)?UtilFormatOut.checkNull((String)findByPK.get(field.getName())):(useValue?"":UtilFormatOut.checkNull(request.getParameter(field.getName())))%></textarea>
        <%}%>
      <%}%>
        &nbsp;</div>
      </td>
    </tr>
    <%}%>
  <%}else{%>
    <%showFields=false;%>
    You do not have permission to create a <%=entityName%> (<%=entity.getPlainTableName()%>_ADMIN, or <%=entity.getPlainTableName()%>_CREATE needed).
  <%}%>
<%}else{%>
  <%if(hasUpdatePermission){%>
    <input type="hidden" name="UPDATE_MODE" value="UPDATE">

    <%for (int fnum = 0; fnum < entity.getPksSize();fnum++){%>
      <%ModelField field = entity.getPk(fnum);%>
      <%ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());%>
    <%rowClass=(rowClass==rowClass1?rowClass2:rowClass1);%><tr class="<%=rowClass%>">
      <td valign="top"><div class="tabletext"><b><%=field.getName()%></b></div></td>
      <td valign="top">
        <div class="tabletext">
      <%if (type.getJavaType().equals("Timestamp") || type.getJavaType().equals("java.sql.Timestamp")) {%>
        <%java.sql.Timestamp dtVal = value.getTimestamp(field.getName());%>
        <%String dtStr = dtVal==null?"":dtVal.toString();%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=dtStr%>">
        <%=dtStr%>
      <%} else if(type.getJavaType().equals("Date") || type.getJavaType().equals("java.sql.Date")) {%>
        <%java.sql.Date dateVal = value.getDate(field.getName());%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=dateVal==null?"":dateVal.toString()%>">
        <%=dateVal==null?"":dateVal.toString()%>
      <%} else if(type.getJavaType().equals("Time") || type.getJavaType().equals("java.sql.Time")) {%>
        <%java.sql.Time timeVal = value.getTime(field.getName());%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=timeVal==null?"":timeVal.toString()%>">
        <%=timeVal==null?"":timeVal.toString()%>
      <%} else if(type.getJavaType().indexOf("Integer") >= 0) {%>
        <%Integer numVal = (Integer) value.get(field.getName());%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=numVal==null?"":numVal.toString()%>">
        <%=numVal==null?"":numVal.toString()%>
      <%} else if(type.getJavaType().indexOf("Long") >= 0) {%>
        <%Long numVal = (Long) value.get(field.getName());%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=numVal==null?"":numVal.toString()%>">
        <%=numVal==null?"":numVal.toString()%>
      <%} else if(type.getJavaType().indexOf("Double") >= 0) {%>
        <%Double numVal = (Double) value.get(field.getName());%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=numVal==null?"":numVal.toString()%>">
        <%=numVal==null?"":numVal.toString()%>
      <%} else if(type.getJavaType().indexOf("Float") >= 0) {%>
        <%Float numVal = (Float) value.get(field.getName());%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=numVal==null?"":numVal.toString()%>">
        <%=numVal==null?"":numVal.toString()%>
      <%} else if(type.getJavaType().indexOf("String") >= 0) {%>
        <input type="hidden" name="<%=field.getName()%>" value="<%=UtilFormatOut.checkNull((String)value.get(field.getName()))%>">
        <%=UtilFormatOut.checkNull((String)value.get(field.getName()))%>
      <%}%>
        &nbsp;</div>
      </td>
    </tr>
    <%}%>

  <%} else {%>
    <%showFields=false;%>
    You do not have permission to update a <%=entityName%> (<%=entity.getPlainTableName()%>_ADMIN, or <%=entity.getPlainTableName()%>_UPDATE needed).
  <%}%>
<%} //end if value == null %>

<%if (showFields) {%>
    <%for (int fnum = 0; fnum < entity.getNopksSize(); fnum++) {%>
      <%ModelField field = entity.getNopk(fnum);%>
      <%ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());%>
    <%rowClass=(rowClass==rowClass1?rowClass2:rowClass1);%><tr class="<%=rowClass%>">
      <td valign="top"><div class="tabletext"><b><%=field.getName()%></b></div></td>
      <td valign="top">
        <div class="tabletext">
      <%if (type.getJavaType().equals("Timestamp") || type.getJavaType().equals("java.sql.Timestamp")) {%>
        <%
          String dateTimeString = null;
          if (value != null && useValue) {
            java.sql.Timestamp dtVal = value.getTimestamp(field.getName());
            if(dtVal != null) {
              dateTimeString = dtVal.toString();
            }
          } else if (!useValue) {
            dateTimeString = request.getParameter(field.getName());
          }
        %>
        DateTime(YYYY-MM-DD HH:mm:SS.sss):<input class='editInputBox' type="text" name="<%=field.getName()%>" size="24" value="<%=UtilFormatOut.checkNull(dateTimeString)%>">
        <a href="javascript:call_cal(document.updateForm.<%=field.getName()%>, '<%=UtilFormatOut.checkNull(dateTimeString)%>');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
      <%} else if (type.getJavaType().equals("Date") || type.getJavaType().equals("java.sql.Date")) {%>
        <%
          String dateString = null;
          if (value != null && useValue) {
            java.sql.Date dateVal = value.getDate(field.getName());
            dateString = dateVal==null?"":dateVal.toString();
          } else if (!useValue) {
            dateString = request.getParameter(field.getName());
          }
        %>
        Date(YYYY-MM-DD):<input class='editInputBox' type="text" name="<%=field.getName()%>" size="11" value="<%=UtilFormatOut.checkNull(dateString)%>">
        <a href="javascript:call_cal(document.updateForm.<%=field.getName()%>, null);" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
      <%} else if (type.getJavaType().equals("Time") || type.getJavaType().equals("java.sql.Time")) {%>
        <%
          String timeString = null;
          if (value != null && useValue) {
            java.sql.Time timeVal = value.getTime(field.getName());
            timeString = timeVal==null?"":timeVal.toString();
          } else if (!useValue) {
            timeString = request.getParameter(field.getName());
          }
        %>
        Time(HH:mm:SS.sss):<input class='editInputBox' type="text" size="6" maxlength="10" name="<%=field.getName()%>" value="<%=UtilFormatOut.checkNull(timeString)%>">
      <%}else if (type.getJavaType().indexOf("Integer") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(value!=null&&useValue)?UtilFormatOut.safeToString((Integer)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%>">
      <%}else if (type.getJavaType().indexOf("Long") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(value!=null&&useValue)?UtilFormatOut.safeToString((Long)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%>">
      <%}else if (type.getJavaType().indexOf("Double") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(value!=null&&useValue)?UtilFormatOut.safeToString((Double)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%>">
      <%}else if (type.getJavaType().indexOf("Float") >= 0){%>
        <input class='editInputBox' type="text" size="20" name="<%=field.getName()%>" value="<%=(value!=null&&useValue)?UtilFormatOut.safeToString((Float)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%>">
      <%}else if (type.getJavaType().indexOf("String") >= 0){%>
        <%if (type.stringLength() <= 80){%>
        <input class='editInputBox' type="text" size="<%=type.stringLength()%>" maxlength="<%=type.stringLength()%>" name="<%=field.getName()%>" value="<%=(value!=null&&useValue)?UtilFormatOut.checkNull((String)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%>">
        <%} else if (type.stringLength() <= 255){%>
          <input class='editInputBox' type="text" size="80" maxlength="<%=type.stringLength()%>" name="<%=field.getName()%>" value="<%=(value!=null&&useValue)?UtilFormatOut.checkNull((String)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%>">
        <%} else {%>
          <textarea cols="60" rows="3" maxlength="<%=type.stringLength()%>" name="<%=field.getName()%>"><%=(value!=null&&useValue)?UtilFormatOut.checkNull((String)value.get(field.getName())):UtilFormatOut.checkNull(request.getParameter(field.getName()))%></textarea>
        <%}%>
      <%}%>
        &nbsp;</div>
      </td>
    </tr>
    <%}%>

  <%rowClass=(rowClass==rowClass1?rowClass2:rowClass1);%><tr class="<%=rowClass%>">
    <td colspan="2"><input type="submit" name="Update" value="Update"></td>
  </tr>
<%}%>
</table>
</form>
  </div>
<%}%>
<%-- ======================================================================== --%>

<%for (int relIndex = 0; relIndex < entity.getRelationsSize(); relIndex++) {%>
  <%ModelRelation relation = entity.getRelation(relIndex);%>
    <%ModelEntity relatedEntity = reader.getModelEntity(relation.getRelEntityName());%>
    <%if("one".equals(relation.getType()) || "one-nofk".equals(relation.getType())) {%>
<%-- Start ModelRelation for <%=relation.relatedEjbName%>, type: one --%>
<%if (value != null) {%>
  <%if (hasAllView || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_VIEW", session)) {%>
    <%-- GenericValue valueRelated = delegator.findByPrimaryKey(value.get<%=relation.keyMapUpperString("(), " + GenUtil.lowerFirstChar(entity.getEntityName()) + ".get", "()")%>); --%>
    <%Iterator tempIter = UtilMisc.toIterator(value.getRelated(relation.getTitle() + relatedEntity.getEntityName()));%>
    <%GenericValue valueRelated = null;%>
    <%if (tempIter != null && tempIter.hasNext()) valueRelated = (GenericValue) tempIter.next();%>
  <DIV id='area<%=relIndex+3%>' class='topcontainerhidden' width="100%">
    <div class='areaheader'>
     <b><%=relation.getTitle()%></b> Related Entity: <b><%=relatedEntity.getEntityName()%></b> with PK: <%=valueRelated!=null?valueRelated.getPrimaryKey().toString():"entity not found!"%>
    </div>
    <%
      String findString = "entityName=" + relatedEntity.getEntityName();
      for (int knum = 0; knum < relation.getKeyMapsSize(); knum++) {
        ModelKeyMap keyMap = relation.getKeyMap(knum);
        if (value.get(keyMap.getFieldName()) != null) {
          findString += "&" + keyMap.getRelFieldName() + "=" + value.get(keyMap.getFieldName());
        }
      }
    %>
      
    <%if(valueRelated == null){%>
      <%if(hasAllCreate || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_CREATE", session)){%>
        <a href='<ofbiz:url>/ViewGeneric?<%=findString%></ofbiz:url>' class="buttontext">[Create <%=relatedEntity.getEntityName()%>]</a>
      <%}%>
    <%}else{%>
      <a href='<ofbiz:url>/ViewGeneric?<%=findString%></ofbiz:url>' class="buttontext">[View <%=relatedEntity.getEntityName()%>]</a>
    <%}%>
  <div style='width: 100%; overflow: visible; border-style: none;'>
    <table border="0" cellspacing="2" cellpadding="2">
    <%if (valueRelated == null) {%>
      <tr class="<%=rowClass1%>"><td><b>Specified <%=relatedEntity.getEntityName()%> entity was not found.</b></td></tr>
    <%} else {%>
      <%for(int fnum = 0; fnum < relatedEntity.getFieldsSize(); fnum++) {%>
        <%ModelField field = relatedEntity.getField(fnum);%>
        <%ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());%>
      <%rowClass=(rowClass==rowClass1?rowClass2:rowClass1);%><tr class="<%=rowClass%>">
        <td valign="top"><div class="tabletext"><b><%=field.getName()%></b></div></td>
        <td valign="top">
          <div class="tabletext">
        <%if(type.getJavaType().equals("Timestamp") || type.getJavaType().equals("java.sql.Timestamp")){%>
          <%java.sql.Timestamp dtVal = valueRelated.getTimestamp(field.getName());%>
          <%=dtVal==null?"":dtVal.toString()%>
        <%} else if(type.getJavaType().equals("Date") || type.getJavaType().equals("java.sql.Date")){%>
          <%java.sql.Date dateVal = valueRelated.getDate(field.getName());%>
          <%=dateVal==null?"":dateVal.toString()%>
        <%} else if(type.getJavaType().equals("Time") || type.getJavaType().equals("java.sql.Time")){%>
          <%java.sql.Time timeVal = valueRelated.getTime(field.getName());%>
          <%=timeVal==null?"":timeVal.toString()%>
        <%}else if(type.getJavaType().indexOf("Integer") >= 0){%>
          <%=UtilFormatOut.safeToString((Integer)valueRelated.get(field.getName()))%>
        <%}else if(type.getJavaType().indexOf("Long") >= 0){%>
          <%=UtilFormatOut.safeToString((Long)valueRelated.get(field.getName()))%>
        <%}else if(type.getJavaType().indexOf("Double") >= 0){%>
          <%=UtilFormatOut.safeToString((Double)valueRelated.get(field.getName()))%>
        <%}else if(type.getJavaType().indexOf("Float") >= 0){%>
          <%=UtilFormatOut.safeToString((Float)valueRelated.get(field.getName()))%>
        <%}else if(type.getJavaType().indexOf("String") >= 0){%>
          <%=UtilFormatOut.checkNull((String)valueRelated.get(field.getName()))%>
        <%}%>
          &nbsp;</div>
        </td>
      </tr>
    <%}%>
    <%} //end if valueRelated == null %>
    </table>
    </div>
  </div>
  <%}%>
<%}%>
<%-- End ModelRelation for <%=relation.relatedEjbName%>, type: one --%>
  <%}else if(relation.getType().equalsIgnoreCase("many")){%>
<%-- Start ModelRelation for <%=relation.relatedEjbName%>, type: many --%>

<%if(value != null){%>
  <%if(hasAllView || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_VIEW", session)){%>
    <%-- Iterator relatedIterator = UtilMisc.toIterator(value.getRelated(relation.getTitle() + relatedEntity.getEntityName())); --%>
  <DIV id=area<%=relIndex+3%> class='topcontainerhidden' width="100%">
    <div class=areaheader>
      <b><%=relation.getTitle()%></b> Related Entities: <b><%=relatedEntity.getEntityName()%></b> with 
    </div>
    <%boolean relatedCreatePerm = hasAllCreate || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_CREATE", session);%>
    <%boolean relatedUpdatePerm = hasAllUpdate || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_UPDATE", session);%>
    <%boolean relatedDeletePerm = hasAllDelete || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_DELETE", session);%>
    <%
      String rowClassResultHeader = "viewManyHeaderTR";
      String rowClassResult1 = "viewManyTR1";
      String rowClassResult2 = "viewManyTR2"; 
      String rowClassResult = "";
    %>
    <%
      String findString = "entityName=" + relatedEntity.getEntityName();
      for (int knum = 0; knum < relation.getKeyMapsSize(); knum++) {
        ModelKeyMap keyMap = relation.getKeyMap(knum);
        if(value.get(keyMap.getFieldName()) != null) {
          findString += "&" + keyMap.getRelFieldName() + "=" + value.get(keyMap.getFieldName());
        }
      }
    %>
    <%if(relatedCreatePerm){%>
      <a href='<ofbiz:url>/ViewGeneric?<%=UtilFormatOut.encodeQuery(findString)%></ofbiz:url>' class="buttontext">[Create <%=relatedEntity.getEntityName()%>]</a>
    <%}%>    
    <a href='<ofbiz:url>/FindGeneric?find=true&<%=UtilFormatOut.encodeQuery(findString)%></ofbiz:url>' class="buttontext">[Find <%=relatedEntity.getEntityName()%>]</a>
<%--
  <div style='width:100%;overflow:visible;border-style:none;'>
  <table width="100%" cellpadding="2" cellspacing="2" border="0">
    <tr class="<%=rowClassResultHeader%>">
  <%for(i=0;i<relatedEntity.fields.size();i++){%>
      <td><div class="tabletext"><b><nobr><%=((ModelField)relatedEntity.fields.elementAt(i)).columnName%></nobr></b></div></td><%}%>
      <td>&nbsp;</td>
      <%if(relatedDeletePerm){%>
        <td>&nbsp;</td>
      <%}%>
    </tr>
    <%
     int relatedLoopCount = 0;
     if (relatedIterator != null && relatedIterator.hasNext()) {
      while (relatedIterator != null && relatedIterator.hasNext()) {
        relatedLoopCount++; //if(relatedLoopCount > 10) break;
        <%=relatedEntity.getEntityName()%> valueRelated = (<%=relatedEntity.getEntityName()%>)relatedIterator.next();
        if(valueRelated != null) {
    %>
    <%rowClassResult=(rowClassResult==rowClassResult1?rowClassResult2:rowClassResult1);%><tr class="<%=rowClassResult%>">
  <%for (i=0;i<relatedEntity.fields.size();i++) {%>
      <td>
        <div class="tabletext"><%if(((ModelField)relatedEntity.fields.elementAt(i)).javaType.equals("Timestamp") || ((ModelField)relatedEntity.fields.elementAt(i)).javaType.equals("java.sql.Timestamp")){%>
      <%{
        String dateTimeString = null;
        if (valueRelated != null) {
          java.sql.Timestamp timeStamp = valueRelated.get<%=GenUtil.upperFirstChar(((ModelField)relatedEntity.fields.elementAt(i)).getFieldName())%>();
          if (timeStamp  != null) {
            dateTimeString = timeStamp.toString();
          }
        }
      %>
      <%=UtilFormatOut.checkNull(dateString)%>&nbsp;<%=UtilFormatOut.checkNull(timeString)%>
      <%}%><%} else if(((ModelField)relatedEntity.fields.elementAt(i)).javaType.equals("Date") || ((ModelField)relatedEntity.fields.elementAt(i)).javaType.equals("java.util.Date")) {%>
      <%{
        String dateString = null;
        String timeString = null;
        if (valueRelated != null) {
          java.util.Date date = valueRelated.get<%=GenUtil.upperFirstChar(((ModelField)relatedEntity.fields.elementAt(i)).getFieldName())%>();
          if (date  != null) {
            dateString = UtilDateTime.toDateString(date);
            timeString = UtilDateTime.toTimeString(date);
          }
        }
      %>
      <%=UtilFormatOut.checkNull(dateString)%>&nbsp;<%=UtilFormatOut.checkNull(timeString)%>
      <%}%><%}else if(((ModelField)relatedEntity.fields.elementAt(i)).javaType.indexOf("Integer") >= 0 || ((ModelField)relatedEntity.fields.elementAt(i)).javaType.indexOf("Long") >= 0 || ((ModelField)relatedEntity.fields.elementAt(i)).javaType.indexOf("Double") >= 0 || ((ModelField)relatedEntity.fields.elementAt(i)).javaType.indexOf("Float") >= 0){%>
      <%=UtilFormatOut.safeToString(valueRelated.get<%=GenUtil.upperFirstChar(((ModelField)relatedEntity.fields.elementAt(i)).getFieldName())%>())%><%}else{%>
      <%=UtilFormatOut.checkNull(valueRelated.get<%=GenUtil.upperFirstChar(((ModelField)relatedEntity.fields.elementAt(i)).getFieldName())%>())%><%}%>
        &nbsp;</div>
      </td>
  <%}%>
      <td>
        <a href="<%=response.encodeURL(controlPath + "/View<%=relatedEntity.getEntityName()%>?" + <%=relatedEntity.httpArgListFromClass(relatedEntity.pks, "Related")%>)%>" class="buttontext">[View]</a>
      </td>
      <%if (relatedDeletePerm) {%>
        <td>
          <a href="<%=response.encodeURL(controlPath + "/Update<%=relatedEntity.getEntityName()%>?" + <%=relatedEntity.httpArgListFromClass(relatedEntity.pks, "Related")%> + "&" + <%=entity.httpArgList(entity.pks)%> + "&UPDATE_MODE=DELETE")%>" class="buttontext">[Delete]</a>
        </td>
      <%}%>
    </tr>
    <%}%>
  <%}%>
<%} else {%>
<%rowClassResult=(rowClassResult==rowClassResult1?rowClassResult2:rowClassResult1);%><tr class="<%=rowClassResult%>">
<td colspan="<%=relatedEntity.fields.size() + 2%>">
<h3>No <%=relatedEntity.getEntityName()%>s Found.</h3>
</td>
</tr>
<%}%>
    </table>
  </div>
Displaying <%=relatedLoopCount%> entities.
--%>
  </div>
  <%}%>
<%}%>
<%-- End ModelRelation for <%=relation.relatedEjbName%>, type: many --%>
  <%}%>
<%}%>
</div>
<%if ((hasUpdatePermission || hasCreatePermission) && !useValue) {%>
  <script language="JavaScript" type="text/javascript">  
    ShowViewTab("edit");
  </script>
<%}%>
<br/>
<%} else {%>
  <h3>You do not have permission to view this page (<%=entity.getPlainTableName()%>_ADMIN, or <%=entity.getPlainTableName()%>_VIEW needed).</h3>
<%}%>
<%} catch (Exception e) { Debug.log(e); throw e;%><%}%>
