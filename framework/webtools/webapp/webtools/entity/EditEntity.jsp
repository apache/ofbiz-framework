<%@page contentType="text/html"%>
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

<%@ page import="java.util.*" %>
<%@ page import="org.ofbiz.base.util.*, org.ofbiz.base.config.*" %>
<%@ page import="org.ofbiz.entity.*" %>
<%@ page import="org.ofbiz.entity.model.*" %>
<%@ page import="org.ofbiz.entity.util.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<%
if (security.hasPermission("ENTITY_MAINT", session)) {
  String entityName = request.getParameter("entityName");
  ModelReader reader = delegator.getModelReader();
  String event = request.getParameter("event");
  ModelEntity entity = null;
  if (UtilValidate.isNotEmpty(entityName) && !"addEntity".equals(event)) entity = reader.getModelEntity(entityName);
  ModelViewEntity modelViewEntity = null;
  if (entity instanceof ModelViewEntity) modelViewEntity = (ModelViewEntity)entity;
  TreeSet entSet = new TreeSet(reader.getEntityNames());
  String errorMsg = "";
  ResourceHandler entityResourceHandler = delegator.getModelReader().getEntityResourceHandler(entityName);

  if ("addEntity".equals(event)) {
    if (entity == null) {
      entity = new ModelEntity();
      entity.setEntityName(request.getParameter("entityName"));
      entity.setTableName(ModelUtil.javaNameToDbName(entity.getEntityName()));
      reader.getEntityCache().put(entity.getEntityName(), entity);
      entityName = entity.getEntityName();
      
      String entityGroup = request.getParameter("entityGroup");
      delegator.getModelGroupReader().getGroupCache().put(entityName, entityGroup);
    }
  } else if ("updateEntity".equals(event)) {
    entity.setTableName(request.getParameter("tableName"));
    entity.setPackageName(request.getParameter("packageName"));
    entity.setDependentOn(request.getParameter("dependentOn"));
    entity.setTitle(request.getParameter("title"));
    entity.setDescription(request.getParameter("description"));
    entity.setCopyright(request.getParameter("copyright"));
    entity.setAuthor(request.getParameter("author"));
    entity.setVersion(request.getParameter("version"));

    String entityGroup = request.getParameter("entityGroup");
    delegator.getModelGroupReader().getGroupCache().put(entityName, entityGroup);

    delegator.getModelReader().addEntityToResourceHandler(entityName, request.getParameter("loaderName"), request.getParameter("location"));
    delegator.getModelReader().rebuildResourceHandlerEntities();
  } else if ("removeField".equals(event)) {
    String fieldName = request.getParameter("fieldName");
    entity.removeField(fieldName);
  } else if ("updateField".equals(event)) {
    String fieldName = request.getParameter("fieldName");
    String fieldType = request.getParameter("fieldType");
    String primaryKey = request.getParameter("primaryKey");
    ModelField field = entity.getField(fieldName);
    field.setType(fieldType);
    if (primaryKey != null) field.setIsPk(true);
    else field.setIsPk(false);
    entity.updatePkLists();
  } else if ("addField".equals(event)) {
    ModelField field = new ModelField();
    field.setName(request.getParameter("name"));
    field.setColName(ModelUtil.javaNameToDbName(field.getName()));
    field.setType(request.getParameter("fieldType"));
    entity.addField(field);
  } else if ("addRelation".equals(event)) {
    String relEntityName = request.getParameter("relEntityName");
    String type = request.getParameter("type");
    String title = request.getParameter("title");
    String fkName = request.getParameter("fkName");
    ModelRelation relation = new ModelRelation();

    ModelEntity relEntity = reader.getModelEntity(relEntityName);
    if (relEntity == null) {
        errorMsg = errorMsg + "<li> Related Entity \"" + relEntityName + "\" not found, not adding.";
    } else {
      relation.setRelEntityName(relEntityName);
      relation.setType(type);
      relation.setTitle(title);
      relation.setFkName(fkName);
      relation.setMainEntity(entity);
      entity.addRelation(relation);
      if ("one".equals(type) || "one-nofk".equals(type)) {
        for (int pk = 0; pk < relEntity.getPksSize(); pk++) {
          ModelField pkf = relEntity.getPk(pk);
          ModelKeyMap keyMap = new ModelKeyMap();
          keyMap.setFieldName(pkf.getName());
          keyMap.setRelFieldName(pkf.getName());
          relation.addKeyMap(keyMap);
        }
      }
    }
  } else if ("updateRelation".equals(event)) {
    int relNum = Integer.parseInt(request.getParameter("relNum"));
    String type = request.getParameter("type");
    String title = request.getParameter("title");
    String fkName = request.getParameter("fkName");

    ModelRelation relation = entity.getRelation(relNum);
    relation.setType(type);
    relation.setTitle(title);
    relation.setFkName(fkName);
  } else if ("removeRelation".equals(event)) {
    int relNum = Integer.parseInt(request.getParameter("relNum"));
    if (relNum < entity.getRelationsSize() && relNum >= 0) entity.removeRelation(relNum);
    else errorMsg = errorMsg + "<li> Relation number " + relNum + " is out of bounds.";
  } else if ("updateKeyMap".equals(event)) {
    int relNum = Integer.parseInt(request.getParameter("relNum"));
    int kmNum = Integer.parseInt(request.getParameter("kmNum"));
    String fieldName = request.getParameter("fieldName");
    String relFieldName = request.getParameter("relFieldName");
    
    ModelRelation relation = entity.getRelation(relNum);
    ModelEntity relEntity = reader.getModelEntity(relation.getRelEntityName());
    ModelKeyMap keyMap = relation.getKeyMap(kmNum);
    ModelField field = entity.getField(fieldName);
    ModelField relField = relEntity.getField(relFieldName);

    keyMap.setFieldName(field.getName());
    keyMap.setRelFieldName(relField.getName());
  } else if ("removeKeyMap".equals(event)) {
    int relNum = Integer.parseInt(request.getParameter("relNum"));
    int kmNum = Integer.parseInt(request.getParameter("kmNum"));

    ModelRelation relation = entity.getRelation(relNum);
    relation.removeKeyMap(kmNum);
  } else if ("addKeyMap".equals(event)) {
    int relNum = Integer.parseInt(request.getParameter("relNum"));

    ModelRelation relation = entity.getRelation(relNum);
    ModelKeyMap keyMap = new ModelKeyMap();
    relation.addKeyMap(keyMap);
  } else if ("addReverse".equals(event)) {
    int relNum = Integer.parseInt(request.getParameter("relNum"));

    ModelRelation relation = entity.getRelation(relNum);
    ModelEntity relatedEnt = reader.getModelEntity(relation.getRelEntityName());
    if (relatedEnt != null) {
      if (relatedEnt.getRelation(relation.getTitle() + entity.getEntityName()) == null) {
        ModelRelation newRel = new ModelRelation();
        relatedEnt.addRelation(newRel);

        newRel.setRelEntityName(entity.getEntityName());
        newRel.setTitle(relation.getTitle());
        if ("one".equals(relation.getType()) || "one-nofk".equals(relation.getType())) newRel.setType("many");
        else newRel.setType("one");

        for (int kmn = 0; kmn < relation.getKeyMapsSize(); kmn++) {
          ModelKeyMap curkm = relation.getKeyMap(kmn);
          ModelKeyMap newkm = new ModelKeyMap();
          newRel.addKeyMap(newkm);
          newkm.setFieldName(curkm.getRelFieldName());
          newkm.setRelFieldName(curkm.getFieldName());
        }

        newRel.setMainEntity(relatedEnt);
      } else {
        errorMsg = errorMsg + "<li> Related entity already has a relation with name " + relation.getTitle() + entity.getEntityName() + ", no reverse relation added.";
      }
    } else {
      errorMsg = errorMsg + "<li> Could not find related entity " + relation.getRelEntityName() + ", no reverse relation added.";
    }
  } else if ("addMemberEntity".equals(event)) {
    String alias = request.getParameter("alias");
    String aliasedEntityName = request.getParameter("aliasedEntityName");

    if (UtilValidate.isEmpty(alias) || UtilValidate.isEmpty(alias)) {
      errorMsg = errorMsg + "<li> You must specify an Entity Alias and an Entity Name for addMemberEntity.";
    }

    if (modelViewEntity != null) {
      ModelViewEntity.ModelMemberEntity modelMemberEntity = new ModelViewEntity.ModelMemberEntity(alias, aliasedEntityName);
      modelViewEntity.addMemberModelMemberEntity(modelMemberEntity);
    } else {
      errorMsg = errorMsg + "<li> Cannot run addMemberEntity on a non view-entity.";
    }
  } else if ("removeMemberEntity".equals(event)) {
    String alias = request.getParameter("alias");
    if (modelViewEntity != null) {
      modelViewEntity.removeMemberModelMemberEntity(alias);
    } else {
      errorMsg = errorMsg + "<li> Cannot run removeMemberEntity on a non view-entity.";
    }
  } else if (event != null) {
    errorMsg = errorMsg + "<li> The operation specified is not valid: [" + event + "]";
  }

  Collection typesCol = delegator.getEntityFieldTypeNames(entity);
  TreeSet types = null;
  if (typesCol != null) types = new TreeSet(typesCol);
%>

<style>
A.listtext {font-family: Helvetica,sans-serif; font-size: 10pt; font-weight: bold; text-decoration: none; color: blue;}
A.listtext:hover {color:red;}
</style>
<H3>Entity Editor</H3>

<%if (errorMsg.length() > 0) {%>
<span style='color: red;'>The following errors occurred:
<ul><%=errorMsg%></ul>
</span>
<br/>
<%}%>

<FORM method="post" action='<ofbiz:url>/view/EditEntity</ofbiz:url>' style='margin: 0;'>
  <INPUT type=TEXT class='inputBox' size='30' name='entityName'>
  <INPUT type=SUBMIT value='Edit Specified Entity'>
</FORM>
<FORM method="post" action='<ofbiz:url>/view/EditEntity</ofbiz:url>' style='margin: 0;'>
  <SELECT name='entityName' class='selectBox'>
    <OPTION selected>&nbsp;</OPTION>
    <%Iterator entIter1 = entSet.iterator();%>
    <%while (entIter1.hasNext()) {%>
      <OPTION><%=(String)entIter1.next()%></OPTION>
    <%}%>
  </SELECT>
  <INPUT type=SUBMIT value='Edit Specified Entity'>
</FORM>
<hr/>
<FORM method="post" action='<ofbiz:url>/view/EditEntity?event=addEntity</ofbiz:url>' style='margin: 0;'>
  Entity Name (Java style): <INPUT type=TEXT class='inputBox' size='60' name='entityName'><br/>
  Entity Group: <INPUT type=TEXT size='60' class='inputBox' name='entityGroup' value='org.ofbiz'>
  <INPUT type=SUBMIT value='Create Entity'>
</FORM>
<hr/>
<%if (entity == null) {%>
  <H4>Entity not found with name "<%=UtilFormatOut.checkNull(entityName)%>"</H4>
<%}else{%>

<BR>
<A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%></ofbiz:url>'>Reload Current Entity: <%=entityName%></A><BR>
<BR>

<FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&event=updateEntity</ofbiz:url>' style='margin: 0;'>
  <TABLE>
  <TR>
    <TD>Entity Name</TD>
    <TD><%=entityName%></TD>
  </TR>
  <TR>
    <TD>Table Name</TD>
    <TD><%=(modelViewEntity == null) ? entity.getPlainTableName() : "What table name? This is a VIEW Entity."%></TD>
  </TR>
  <%if (modelViewEntity == null) {%>
    <TR>
      <TD>Table Name</TD>
      <TD><INPUT type="text" class='inputBox' size='60' name='tableName' value='<%=UtilFormatOut.checkNull(entity.getPlainTableName())%>'></TD>
    </TR>
  <%}%>
  <TR>
    <TD>Package Name</TD>
    <TD><INPUT type="text" class='inputBox' size='60' name='packageName' value='<%=entity.getPackageName()%>'></TD>
  </TR>
  <TR>
    <TD>Dependent On Entity</TD>
    <TD>
      <SELECT name='dependentOn' class='selectBox'>
        <OPTION selected><%=entity.getDependentOn()%></OPTION>
        <OPTION></OPTION>
        <%Iterator depIter = entSet.iterator();%>
        <%while (depIter.hasNext()) {%>
          <OPTION><%=(String)depIter.next()%></OPTION>
        <%}%>
      </SELECT>
    </TD>
  </TR>
  <TR>
    <TD>Title</TD>
    <TD><INPUT type="text" class='inputBox' size='60' name='title' value='<%=entity.getTitle()%>'></TD>
  </TR>
  <TR>
    <TD>Description</TD>
    <TD><TEXTAREA cols='60' class='textAreaBox' rows='5' name='description'><%=entity.getDescription()%></TEXTAREA></TD>
  </TR>
  <TR>
    <TD>Copyright</TD>
    <TD><INPUT type="text" class='inputBox' size='60' name='copyright' value='<%=entity.getCopyright()%>'></TD>
  </TR>
  <TR>
    <TD>Author</TD>
    <TD><INPUT type="text" class='inputBox' size='60' name='author' value='<%=entity.getAuthor()%>'></TD>
  </TR>
  <TR>
    <TD>Version</TD>
    <TD><INPUT type="text" class='inputBox' size='60' name='version' value='<%=entity.getVersion()%>'></TD>
  </TR>
  <TR>
    <TD>Group</TD>
    <TD>
      <INPUT type="text" class='inputBox' size='60' name='entityGroup' value='<%=UtilFormatOut.checkNull(delegator.getModelGroupReader().getEntityGroupName(entityName))%>'>
      <BR>(This group is for the "<%=delegator.getDelegatorName()%>" delegator)
    </TD>
  </TR>
  <%boolean isFile = entityResourceHandler == null ? true : entityResourceHandler.isFileResource();%>
  <TR>
    <TD>Resource Loader</TD>
    <TD><INPUT type="text" class='inputBox' size='20' name='loaderName' value='<%=entityResourceHandler == null ? "" : UtilFormatOut.checkNull((String) entityResourceHandler.getLoaderName())%>'<%if(!isFile){%> disabled<%}%>></TD>
  </TR>
  <TR>
    <TD>Location</TD>
    <TD><INPUT type="text" class='inputBox' size='60' name='location' value='<%=entityResourceHandler == null ? "" : UtilFormatOut.checkNull((String) entityResourceHandler.getLocation())%>'<%if(!isFile){%> disabled<%}%>></TD>
  </TR>
  </TABLE>
  <INPUT type="submit" value='Update Entity'>
</FORM>

<hr/>
<%if (modelViewEntity == null) {%>
<B>FIELDS</B>
  <TABLE border='1' cellpadding='2' cellspacing='0'>
    <TR><TD>Field Name</TD><TD>Column Name (Length)</TD><TD>Field Type</TD><TD>&nbsp;</TD><TD>&nbsp;</TD></TR>
    <%for (int f = 0; f < entity.getFieldsSize(); f++) {%>
      <%ModelField field = entity.getField(f);%>
      <TR>
        <TD><%=field.getIsPk()?"<B>":""%><%=field.getName()%><%=field.getIsPk()?"</B>":""%></TD>
        <TD><%=field.getColName()%> (<%=field.getColName().length()%>)</TD>
        <TD><%=field.getType()%></TD>
        <TD>
          <FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&fieldName=<%=field.getName()%>&event=updateField</ofbiz:url>' style='margin: 0;'>
            <INPUT type="checkbox" name='primaryKey'<%=field.getIsPk()?" checked":""%>>
            <SELECT name='fieldType' class='selectBox'>
              <OPTION selected><%=field.getType()%></OPTION>
              <%Iterator iter = UtilMisc.toIterator(types);%>
              <%while (iter != null && iter.hasNext()){ String typeName = (String)iter.next();%>
                <OPTION><%=typeName%></OPTION>
              <%}%>
            </SELECT>
            <INPUT type="submit" value='Set'>
          </FORM>
        </TD>
        <TD><A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&fieldName=<%=field.getName()%>&event=removeField</ofbiz:url>'>Remove</A></TD>
      </TR>
    <%}%>
  </TABLE>

<FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&event=addField</ofbiz:url>'>
  Add new field with <u>Field Name (Java style)</u> and field type.<BR>
  <INPUT type="text" class='inputBox' size='40' maxlength='30' name='name'>
  <SELECT name='fieldType' class='selectBox'>
    <%Iterator iter = UtilMisc.toIterator(types);%>
    <%while (iter != null && iter.hasNext()){ String typeName = (String)iter.next();%>
      <OPTION><%=typeName%></OPTION>
    <%}%>
  </SELECT>
  <INPUT type="submit" value="Create">
</FORM>
<%} else {%>

<B>VIEW MEMBER ENTITIES</B>
  <TABLE border='1' cellpadding='2' cellspacing='0'>
    <TR><TD>Entity Alias</TD><TD>Entity Name</TD><TD>&nbsp;</TD></TR>
    <%Iterator memberEntityNamesIter = UtilMisc.toIterator(modelViewEntity.getMemberModelMemberEntities().entrySet());%>
    <%while (memberEntityNamesIter != null && memberEntityNamesIter.hasNext()) {%>
      <%Map.Entry aliasEntry = (Map.Entry) memberEntityNamesIter.next();%>
      <%ModelViewEntity.ModelMemberEntity modelMemberEntity = (ModelViewEntity.ModelMemberEntity) aliasEntry.getValue();%>
      <TR>
        <TD><%=modelMemberEntity.getEntityAlias()%></TD>
        <TD><%=modelMemberEntity.getEntityName()%></TD>
        <TD><A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&alias=<%=(String) aliasEntry.getKey()%>&event=removeMemberEntity</ofbiz:url>'>Remove</A></TD>
      </TR>
    <%}%>
  </TABLE>

<FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&event=addMemberEntity</ofbiz:url>'>
  Add new member entity with <u>Entity Alias*</u> and <u>Entity Name*</u>.<BR>
  <INPUT type="text" class='inputBox' size='10' name='alias'>
  <SELECT name='aliasedEntityName' class='selectBox'>
    <OPTION selected>&nbsp;</OPTION>
    <%Iterator entIter = entSet.iterator();%>
    <%while (entIter.hasNext()) {%>
      <OPTION><%=(String)entIter.next()%></OPTION>
    <%}%>
  </SELECT>
  <INPUT type="submit" value='Add'>
</FORM>

<hr/>
<B>VIEW ALIASES</B>

<hr/>
<B>VIEW LINKS</B>

<div>NOTE: Editing not yet completed for view entities, try again later (or just edit the XML by hand, and not at the same time you are editing here...)</div>
<%--
    <!ELEMENT view-entity ( description?, member-entity+, alias+, view-link+, relation* )>
    <!ELEMENT member-entity EMPTY>
    <!ATTLIST member-entity
	entity-alias CDATA #REQUIRED
	entity-name CDATA #REQUIRED >
    <!ELEMENT alias EMPTY>
    <!ATTLIST alias
	entity-alias CDATA #REQUIRED
	name CDATA #REQUIRED
	field CDATA #IMPLIED
	prim-key CDATA #IMPLIED
	group-by ( true | false ) "false"
	function ( min | max | sum | avg | count | count-distinct | upper | lower ) #IMPLIED>
    <!ELEMENT view-link ( key-map+ )>
    <!ATTLIST view-link
	entity-alias CDATA #REQUIRED
	rel-entity-alias CDATA #REQUIRED >
--%>

<%}%>
<hr/>

<B>RELATIONSHIPS</B>
  <TABLE border='1' cellpadding='2' cellspacing='0'>
  <%for (int r = 0; r < entity.getRelationsSize(); r++) {%>
    <%ModelRelation relation = entity.getRelation(r);%>
    <%ModelEntity relEntity = reader.getModelEntity(relation.getRelEntityName());%>
    <tr bgcolor='#CCCCFF'>
      <FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&event=updateRelation&relNum=<%=r%></ofbiz:url>'>
        <td align="left"><%=relation.getTitle()%><A class='listtext' href='<ofbiz:url>/view/EditEntity?entityName=<%=relation.getRelEntityName()%></ofbiz:url>'><%=relation.getRelEntityName()%></A></td>
        <td>
          <INPUT type=TEXT class='inputBox' name='title' value='<%=relation.getTitle()%>'>
          <INPUT type=TEXT class='inputBox' name='fkName' value='<%=relation.getFkName()%>' size='18' maxlength='18'>
          <SELECT name='type' class='selectBox'>
            <OPTION selected><%=relation.getType()%></OPTION>
            <OPTION>&nbsp;</OPTION>
            <OPTION>one</OPTION>
            <OPTION>one-nofk</OPTION>
            <OPTION>many</OPTION>
          </SELECT>
        </td>
        <td>
          <INPUT type=SUBMIT value='Set'>
        </td>
        <TD><A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&relNum=<%=r%>&event=removeRelation</ofbiz:url>'>Remove</A></TD>
        <TD><A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&relNum=<%=r%>&event=addKeyMap</ofbiz:url>'>Add&nbsp;KeyMap</A></TD>
        <TD><A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&relNum=<%=r%>&event=addReverse</ofbiz:url>'>Add&nbsp;Reverse</A></TD>
      </FORM>
    </tr>
    <%for (int km=0; km<relation.getKeyMapsSize(); km++){ ModelKeyMap keyMap = (ModelKeyMap)relation.getKeyMap(km);%>
      <tr>
        <FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&event=updateKeyMap&relNum=<%=r%>&kmNum=<%=km%></ofbiz:url>'>
          <td></td>
          <td colspan='2'>
            Main:
            <SELECT name='fieldName' class='selectBox'>
              <OPTION selected><%=keyMap.getFieldName()%></OPTION>
              <OPTION>&nbsp;</OPTION>
              <%for (int fld=0; fld<entity.getFieldsSize(); fld++) {%>
                <OPTION><%=entity.getField(fld).getName()%></OPTION>
              <%}%>
            </SELECT>
            Related:
            <SELECT name='relFieldName' class='selectBox'>
              <OPTION selected><%=keyMap.getRelFieldName()%></OPTION>
              <OPTION>&nbsp;</OPTION>
              <%for (int fld=0; fld<relEntity.getFieldsSize(); fld++) {%>
                <OPTION><%=relEntity.getField(fld).getName()%></OPTION>
              <%}%>
            </SELECT>
          </td>
          <td>
            <INPUT type=SUBMIT value='Set'>
          </td>          
          <TD><A href='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&relNum=<%=r%>&kmNum=<%=km%>&event=removeKeyMap</ofbiz:url>'>Remove</A></TD>
        </FORM>
      </tr>
    <%}%>			
  <%}%>
  </TABLE>

<FORM method="post" action='<ofbiz:url>/view/EditEntity?entityName=<%=entityName%>&event=addRelation</ofbiz:url>'>
  Add new relation with <u>Title</u>, <u>FK Name</u>, <u>Related Entity Name*</u> and <u>Relation Type*</u>.<BR>
  <INPUT type="text" class='inputBox' size='30' maxlength='30' name='title'>
  <INPUT type="text" class='inputBox' size='20' maxlength='18' name='fkName'>
  <%-- <INPUT type="text" size='40' maxlength='30' name='relEntityName'> --%>
  <SELECT name='relEntityName' class='selectBox'>
    <OPTION selected>&nbsp;</OPTION>
    <%Iterator entIter = entSet.iterator();%>
    <%while (entIter.hasNext()) {%>
      <OPTION><%=(String)entIter.next()%></OPTION>
    <%}%>
  </SELECT>
  <SELECT name='type' class='selectBox'>
    <OPTION>one</OPTION>
    <OPTION>one-nofk</OPTION>
    <OPTION>many</OPTION>
  </SELECT>
  <INPUT type="submit" value='Create'>
</FORM>

<%}%>
<%} else {%>
<H3>Entity Editor</H3>

ERROR: You do not have permission to use this page (ENTITY_MAINT needed)
<%}%>
