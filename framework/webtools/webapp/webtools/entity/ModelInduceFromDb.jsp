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
<%@ page import="java.util.*, java.io.*, java.net.*, java.sql.*, org.ofbiz.base.util.*, org.ofbiz.entity.*, org.ofbiz.entity.model.*, org.ofbiz.entity.datasource.*" %><jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" /><jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" /><%

if(security.hasPermission("ENTITY_MAINT", session)) {
  String helperName = request.getParameter("helperName");
  if(helperName == null || helperName.length() <= 0) {
    response.setContentType("text/html");
%>

<div class='head3'><b>Please specify the helperName to induce from:</b></div>
<form action='' method="post">
    <input type='TEXT' class='inputBox' size='40' name='helperName'>
    <input type=SUBMIT value='Induce!'>
</form>
<%
  } else {
      response.setContentType("text/xml");
      Collection messages = new LinkedList();
      GenericDAO dao = GenericDAO.getGenericDAO(helperName);
      List newEntList = dao.induceModelFromDb(messages);

      if(messages.size() > 0) {
%>
ERRORS:
<%
        Iterator mIter = messages.iterator();
        while(mIter.hasNext()) {
%>
<%=(String)mIter.next()%><%
        }
      }
      if(newEntList != null) {
        String title = "Entity of an Apache Open For Business Project (Apache OFBiz) Component";
        String description = "None";
        String copyright = "Copyright 2001-2006 The Apache Software Foundation";
        String author = "None";
        String version = "1.0";
%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE entitymodel PUBLIC "-//OFBiz//DTD Entity Model//EN" "http://www.ofbiz.org/dtds/entitymodel.dtd">
<!--
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
-->

<entitymodel>
  <!-- ========================================================= -->
  <!-- ======================== Defaults ======================= -->
  <!-- ========================================================= -->
    <title><%=title%></title>
    <description><%=description%></description>
    <copyright><%=copyright%></copyright>
    <author><%=author%></author>
    <version><%=version%></version>

  <!-- ========================================================= -->
  <!-- ======================== Data Model ===================== -->
  <!-- The modules in this file are as follows:                  -->
  <!-- ========================================================= -->

  <!-- ========================================================= -->
  <!-- No Package Name -->
  <!-- ========================================================= -->
<% 
  Iterator ecIter = newEntList.iterator();
  while(ecIter.hasNext()) {
    ModelEntity entity = (ModelEntity) ecIter.next();
%>
    <entity entity-name="<%=entity.getEntityName()%>"<%if(!entity.getEntityName().equals(ModelUtil.dbNameToClassName(entity.getPlainTableName())) || !ModelUtil.javaNameToDbName(entity.getEntityName()).equals(entity.getPlainTableName()) ){
          %> table-name="<%=entity.getPlainTableName()%>"<%}%>
            package-name="<%=entity.getPackageName()%>"<%if(entity.getDependentOn().length() > 0){%>
            dependent-on="<%=entity.getDependentOn()%>"<%}%><%if(!title.equals(entity.getTitle())){%>
            title="<%=entity.getTitle()%>"<%}%><%if(!copyright.equals(entity.getCopyright())){%>
            copyright="<%=entity.getCopyright()%>"<%}%><%if(!author.equals(entity.getAuthor())){%>
            author="<%=entity.getAuthor()%>"<%}%><%if(!version.equals(entity.getVersion())){%>
            version="<%=entity.getVersion()%>"<%}%>><%if(!description.equals(entity.getDescription())){%>
      <description><%=entity.getDescription()%></description><%}%><%
  for (int y = 0; y < entity.getFieldsSize(); y++) {
    ModelField field = entity.getField(y);%>
      <field name="<%=field.getName()%>"<%if(!field.getColName().equals(ModelUtil.javaNameToDbName(field.getName()))){
      %> col-name="<%=field.getColName()%>"<%}%> type="<%=field.getType()%>"><%
    for (int v = 0; v<field.getValidatorsSize(); v++) {
      String valName = (String) field.getValidator(v);
      %><validate name="<%=valName%>"/><%
    }%></field><%
  }
  for (int y = 0; y < entity.getPksSize(); y++) {
    ModelField field = entity.getPk(y);%>
      <prim-key field="<%=field.getName()%>"/><%
  }
  if (entity.getRelationsSize() > 0) {
    for (int r = 0; r < entity.getRelationsSize(); r++) {
      ModelRelation relation = entity.getRelation(r);%>
      <relation type="<%=relation.getType()%>"<%if(relation.getTitle().length() > 0){%> title="<%=relation.getTitle()%>"<%}
              %> rel-entity-name="<%=relation.getRelEntityName()%>"><%for(int km=0; km<relation.getKeyMapsSize(); km++){ ModelKeyMap keyMap = relation.getKeyMap(km);%>
        <key-map field-name="<%=keyMap.getFieldName()%>"<%if(!keyMap.getFieldName().equals(keyMap.getRelFieldName())){%> rel-field-name="<%=keyMap.getRelFieldName()%>"<%}%> /><%}%>
      </relation><%
    }
  }%>
    </entity><%
  }%>
</entitymodel>
<%
      }
    } 
  }
else {
  %>ERROR: You do not have permission to use this page (ENTITY_MAINT needed)<%
}
%>
