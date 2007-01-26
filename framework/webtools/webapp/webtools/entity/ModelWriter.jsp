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
<%@ page contentType="text/plain" %><%@ page import="java.util.*, java.io.*, java.net.*, org.ofbiz.base.config.*, org.ofbiz.base.util.*, org.ofbiz.entity.*, org.ofbiz.entity.config.*, org.ofbiz.entity.model.*" %><jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" /><jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" /><%
try {
if (security.hasPermission("ENTITY_MAINT", session) || request.getParameter("originalLoaderName") != null) {
  if ("true".equals(request.getParameter("savetofile"))) {
    //save to the file specified in the ModelReader config
    String controlPath = (String) request.getAttribute("_CONTROL_PATH_");
    String serverRootUrl = (String) request.getAttribute("_SERVER_ROOT_URL_");
    ModelReader modelReader = delegator.getModelReader();

    Iterator handlerIter = modelReader.getResourceHandlerEntitiesKeyIterator();
    while (handlerIter.hasNext()) {
      ResourceHandler resourceHandler = (ResourceHandler) handlerIter.next();
      if (resourceHandler.isFileResource()) {
          String filename = resourceHandler.getFullLocation();

          java.net.URL url = new java.net.URL(serverRootUrl + controlPath + "/ModelWriter");
          HashMap params = new HashMap();
          params.put("originalLoaderName", resourceHandler.getLoaderName());
          params.put("originalLocation", resourceHandler.getLocation());
          HttpClient httpClient = new HttpClient(url, params);
          InputStream in = httpClient.getStream();

          File newFile = new File(filename);
          FileWriter newFileWriter = new FileWriter(newFile);

          BufferedReader post = new BufferedReader(new InputStreamReader(in));
          String line = null;
          while ((line = post.readLine()) != null) {
            newFileWriter.write(line);
            newFileWriter.write("\n");
          }
          newFileWriter.close();
          %>
              If you aren't seeing any exceptions, XML was written successfully to:
              <%=filename%>
              from the URL:
              <%=url.toString()%>?originalLoaderName=<%=resourceHandler.getLoaderName()%>&originalLocation=<%=resourceHandler.getLocation()%>
          <%
      } else {
          %>Cannot write to location <%=resourceHandler.getLocation()%> from 
          loader <%=resourceHandler.getLoaderName()%>, it is not a file.<%
      }
    }
  } else {
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
<% 
  //GenericDelegator delegator = GenericHelperFactory.getDefaultHelper();
  ModelReader reader = delegator.getModelReader();
  Map packages = new HashMap();
  TreeSet packageNames = new TreeSet();

  // ignore fields names
  List ignoredFields = UtilMisc.toList("lastUpdatedStamp", "lastUpdatedTxStamp", "createdStamp", "createdTxStamp");
  //put the entityNames TreeSets in a HashMap by packageName
  Collection ec = null;

  String originalLoaderName = request.getParameter("originalLoaderName");
  String originalLocation = request.getParameter("originalLocation");
  if (originalLoaderName != null && originalLocation != null) {
    ec = reader.getResourceHandlerEntities(new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, originalLoaderName, originalLocation));
  } else {
    ec = reader.getEntityNames();
  }

  Iterator ecIter = ec.iterator();
  while(ecIter.hasNext()) {
    String eName = (String) ecIter.next();
    ModelEntity ent = reader.getModelEntity(eName);
    TreeSet entities = (TreeSet) packages.get(ent.getPackageName());
    if (entities == null) {
      entities = new TreeSet();
      packages.put(ent.getPackageName(), entities);
      packageNames.add(ent.getPackageName());
    }
    entities.add(eName);
  }%>
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
  <!-- The modules in this file are as follows:                  --><%
  Iterator packageNameIter = packageNames.iterator();
  while(packageNameIter.hasNext()) {
    String pName = (String)packageNameIter.next();%>
  <!--  - <%=pName%> --><%
  }%>
  <!-- ========================================================= -->
<%
  Iterator piter = packageNames.iterator();
  while(piter.hasNext()) {
    String pName = (String)piter.next();
    TreeSet entities = (TreeSet)packages.get(pName);
%>

  <!-- ========================================================= -->
  <!-- <%=pName%> -->
  <!-- ========================================================= -->
<%
    Iterator i = entities.iterator();
    while (i.hasNext()) {
      String entityName = (String)i.next();
      ModelEntity entity = reader.getModelEntity(entityName);
      if (entity instanceof ModelViewEntity) {
        ModelViewEntity viewEntity = (ModelViewEntity)entity;
%>
    <view-entity entity-name="<%=entity.getEntityName()%>"
            package-name="<%=entity.getPackageName()%>"<%if (entity.getDependentOn().length() > 0) {%>
            dependent-on="<%=entity.getDependentOn()%>"<%}%><%if (entity.getNeverCache()) {%>
            never-cache="true"<%}%><%if (!title.equals(entity.getTitle())) {%>
            title="<%=entity.getTitle()%>"<%}%><%if (!copyright.equals(entity.getCopyright())) {%>
            copyright="<%=entity.getCopyright()%>"<%}%><%if (!author.equals(entity.getAuthor())) {%>
            author="<%=entity.getAuthor()%>"<%}%><%if (!version.equals(entity.getVersion())) {%>
            version="<%=entity.getVersion()%>"<%}%>><%if (!description.equals(entity.getDescription())) {%>
      <description><%=entity.getDescription()%></description><%}%><%
  Iterator meIter = viewEntity.getAllModelMemberEntities().iterator();
  while(meIter.hasNext()) {
    ModelViewEntity.ModelMemberEntity modelMemberEntity = (ModelViewEntity.ModelMemberEntity) meIter.next();
%>
      <member-entity entity-alias="<%=modelMemberEntity.getEntityAlias()%>" entity-name="<%=modelMemberEntity.getEntityName()%>"/><%
  }
  for (int y = 0; y < viewEntity.getAliasesSize(); y++) {
    ModelViewEntity.ModelAlias alias = viewEntity.getAlias(y);%>
      <alias entity-alias="<%=alias.getEntityAlias()%>" name="<%=alias.getName()%>"<%if (!alias.getName().equals(alias.getField())) {
      %> field="<%=alias.getField()%>"<%}%><%if (alias.getIsPk() != null) {
      %> prim-key="<%=alias.getIsPk().toString()%>"<%}%><%if (alias.getGroupBy()) {
      %> group-by="true"<%}%><%if (UtilValidate.isNotEmpty(alias.getFunction())) {
      %> function="<%=alias.getFunction()%>"<%}%>/><%
  }
  for (int r = 0; r < viewEntity.getViewLinksSize(); r++) {
    ModelViewEntity.ModelViewLink viewLink = viewEntity.getViewLink(r);%>
      <view-link entity-alias="<%=viewLink.getEntityAlias()%>" rel-entity-alias="<%=viewLink.getRelEntityAlias()%>"<%
          if (viewLink.isRelOptional()) {%> rel-optional="true"<%}%>><%for (int km = 0; km < viewLink.getKeyMapsSize(); km++){ ModelKeyMap keyMap = viewLink.getKeyMap(km);%>
        <key-map field-name="<%=keyMap.getFieldName()%>"<%if (!keyMap.getFieldName().equals(keyMap.getRelFieldName())) {%> rel-field-name="<%=keyMap.getRelFieldName()%>"<%}%>/><%}%>
      </view-link><%
  }
  if (entity.getRelationsSize() > 0) {
    for (int r = 0; r < entity.getRelationsSize(); r++) {
      ModelRelation relation = entity.getRelation(r);%>
      <relation type="<%=relation.getType()%>"<%if (relation.getTitle().length() > 0) {%> title="<%=relation.getTitle()%>"<%}
              %> rel-entity-name="<%=relation.getRelEntityName()%>"><%for (int km = 0; km < relation.getKeyMapsSize(); km++){ ModelKeyMap keyMap = relation.getKeyMap(km);%>
        <key-map field-name="<%=keyMap.getFieldName()%>"<%if (!keyMap.getFieldName().equals(keyMap.getRelFieldName())) {%> rel-field-name="<%=keyMap.getRelFieldName()%>"<%}%>/><%}%>
      </relation><%
    }
  }%>
    </view-entity><%
      }
      else {
%>
    <entity entity-name="<%=entity.getEntityName()%>"<%if (!entity.getEntityName().equals(ModelUtil.dbNameToClassName(entity.getPlainTableName()))){
          %> table-name="<%=entity.getPlainTableName()%>"<%}%>
            package-name="<%=entity.getPackageName()%>"<%if (entity.getDependentOn().length() > 0) {%>
            dependent-on="<%=entity.getDependentOn()%>"<%}%><%if (entity.getDoLock()) {%>
            enable-lock="true"<%}%><%if (entity.getNeverCache()) {%>
            never-cache="true"<%}%><%if (!title.equals(entity.getTitle())) {%>
            title="<%=entity.getTitle()%>"<%}%><%if (!copyright.equals(entity.getCopyright())) {%>
            copyright="<%=entity.getCopyright()%>"<%}%><%if (!author.equals(entity.getAuthor())) {%>
            author="<%=entity.getAuthor()%>"<%}%><%if (!version.equals(entity.getVersion())) {%>
            version="<%=entity.getVersion()%>"<%}%>><%if (!description.equals(entity.getDescription())) {%>
      <description><%=entity.getDescription()%></description><%}%><%
  for (int y = 0; y < entity.getFieldsSize(); y++) {
    ModelField field = entity.getField(y);
    if (!ignoredFields.contains(field.getName())) {%>
      <field name="<%=field.getName()%>"<%if (!field.getName().equals(ModelUtil.dbNameToVarName(field.getColName()))){
      %> col-name="<%=field.getColName()%>"<%}%> type="<%=field.getType()%>"><%
    for (int v = 0; v<field.getValidatorsSize(); v++) {
      String valName = field.getValidator(v);
      %><validate name="<%=valName%>"/><%
    }%></field><%
    }}
  for (int y = 0; y < entity.getPksSize(); y++) {
    ModelField field = entity.getPk(y);%>
      <prim-key field="<%=field.getName()%>"/><%
  }
  if (entity.getRelationsSize() > 0) {
    for (int r=0; r<entity.getRelationsSize(); r++) {
      ModelRelation relation = entity.getRelation(r);%>
      <relation type="<%=relation.getType()%>"<%if (relation.getFkName().length() > 0) {%> fk-name="<%=relation.getFkName()%>"<%}
              %><%if (relation.getTitle().length() > 0) {%> title="<%=relation.getTitle()%>"<%}
              %> rel-entity-name="<%=relation.getRelEntityName()%>"><%for (int km = 0; km < relation.getKeyMapsSize(); km++){ ModelKeyMap keyMap = relation.getKeyMap(km);%>
        <key-map field-name="<%=keyMap.getFieldName()%>"<%if (!keyMap.getFieldName().equals(keyMap.getRelFieldName())) {%> rel-field-name="<%=keyMap.getRelFieldName()%>"<%}%>/><%}%>
      </relation><%
    }
  }%>
    </entity><%
      }
    }
  }%>
</entitymodel>
<%
  }
} else {
  %>ERROR: You do not have permission to use this page (ENTITY_MAINT needed)<%
}
} catch (Exception e) {
    Debug.log(e);
}
%>
