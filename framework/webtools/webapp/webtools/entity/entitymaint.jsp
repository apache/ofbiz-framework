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
 *@author     David E. Jones
--%>

<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.entity.model.*" %>
<%@ page import="org.ofbiz.base.util.collections.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<% ResourceBundleMapWrapper uiLabelMap = (ResourceBundleMapWrapper)request.getAttribute("uiLabelMap"); %>

<%
  ModelReader reader = delegator.getModelReader();
  Collection ec = reader.getEntityNames();
  TreeSet entities = new TreeSet(ec);
  Iterator classNamesIterator = entities.iterator();
%>
<h3 style='margin:0;'><%=uiLabelMap.get("WebtoolsEntityDataMaintenance")%></h3>
<%if (security.hasPermission("ENTITY_MAINT", session)) {%>

<%
  String rowColor1 = "viewManyTR2";
  String rowColor2 = "viewManyTR1";
  String rowColor = "";
%>
<table cellpadding='1' cellspacing='1' border='0'>
  <tr>
    <td valign="top">
        <table cellpadding='1' cellspacing='1' border='0'>
          <tr class='viewOneTR1'>
            <td>Entity Name</td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
          </tr>
        
        <%int colSize = entities.size()/3 + 1;%>
        <%int kIdx = 0;%>
        <%while (classNamesIterator != null && classNamesIterator.hasNext()) { ModelEntity entity = reader.getModelEntity((String)classNamesIterator.next());%>
            <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr class="<%=rowColor%>">
              <td><div class='tabletext' style='FONT-SIZE: xx-small;'><%=entity.getEntityName()%></div></td>
              <%if (entity instanceof ModelViewEntity) {%>
                  <%if (security.hasEntityPermission("ENTITY_DATA", "_VIEW", session) || security.hasEntityPermission(entity.getPlainTableName(), "_VIEW", session)) {%>
                    <td colspan='2' align="center"><div class='tabletext' style='FONT-SIZE: xx-small;'>View Entity</div></td>
                    <td><a href='<ofbiz:url>/FindGeneric?entityName=<%=entity.getEntityName()%>&find=true&VIEW_SIZE=50&VIEW_INDEX=0</ofbiz:url>' class="buttontext" style='FONT-SIZE: xx-small;'>All</a></td>
                  <%} else {%>
                    <td colspan='3' align="center"><div class='tabletext' style='FONT-SIZE: xx-small;'>View Entity</div></td>
                  <%}%>
              <%} else {%>
                  <%if (security.hasEntityPermission("ENTITY_DATA", "_CREATE", session) || security.hasEntityPermission(entity.getPlainTableName(), "_CREATE", session)) {%>
                    <td><a href='<ofbiz:url>/ViewGeneric?entityName=<%=entity.getEntityName()%></ofbiz:url>' class="buttontext" style='FONT-SIZE: xx-small;'>Crt</a></td>
                  <%} else {%>
                    <td><div class='tabletext' style='FONT-SIZE: xx-small;'>NP</div></td>
                  <%}%>
                  <%if (security.hasEntityPermission("ENTITY_DATA", "_VIEW", session) || security.hasEntityPermission(entity.getPlainTableName(), "_VIEW", session)) {%>
                    <td><a href='<ofbiz:url>/FindGeneric?entityName=<%=entity.getEntityName()%></ofbiz:url>' class="buttontext" style='FONT-SIZE: xx-small;'>Fnd</a></td>
                    <td><a href='<ofbiz:url>/FindGeneric?entityName=<%=entity.getEntityName()%>&find=true&VIEW_SIZE=50&VIEW_INDEX=0</ofbiz:url>' class="buttontext" style='FONT-SIZE: xx-small;'>All</a></td>
                  <%} else {%>
                    <td><div class='tabletext' style='FONT-SIZE: xx-small;'>NP</div></td>
                    <td><div class='tabletext' style='FONT-SIZE: xx-small;'>NP</div></td>
                  <%}%>
              <%}%>
            </tr>
        
            <%kIdx++;%>
            <%if(kIdx >= colSize) {%>
              <%colSize += colSize;%>
              </table>
            </td>
            <td valign="top">
              <table cellpadding='1' cellspacing='1' border='0'>
              <%rowColor = "";%>
              <tr class='viewOneTR1'>
                <td>Entity&nbsp;Name</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
              </tr>
            <%}%>
        <%}%>
          </tr>
        </table>
    </td>
  </tr>
</table>
<%}else{%>
  <h3>You do not have permission to view this page (ENTITY_MAINT needed).</h3>
<%}%>
