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

<%@ page import="java.util.*" %>
<%@ page import="org.ofbiz.entity.*, org.ofbiz.entity.model.*, org.ofbiz.base.util.*" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<%String controlPath=(String)request.getAttribute("_CONTROL_PATH_");%>

<% 
if(security.hasPermission("ENTITY_MAINT", session)) {
  boolean forstatic = "true".equals(request.getParameter("forstatic"));
  String search = null;
  //GenericDelegator delegator = GenericHelperFactory.getDefaultHelper();
  ModelReader reader = delegator.getModelReader();
  Map packages = new HashMap();
  TreeSet packageNames = new TreeSet();
  TreeSet tableNames = new TreeSet();

  //put the entityNames TreeSets in a HashMap by packageName
  Collection ec = reader.getEntityNames();
  TreeSet entityNames = new TreeSet(ec);
  Iterator ecIter = ec.iterator();
  while(ecIter.hasNext()) {
    String eName = (String)ecIter.next();
    ModelEntity ent = reader.getModelEntity(eName);

    //make sure the table name is in the list of all table names, if not null
    if (UtilValidate.isNotEmpty(ent.getPlainTableName())) tableNames.add(ent.getPlainTableName());

    TreeSet entities = (TreeSet)packages.get(ent.getPackageName());
    if(entities == null) {
      entities = new TreeSet();
      packages.put(ent.getPackageName(), entities);
      packageNames.add(ent.getPackageName());
    }
    entities.add(eName);
  }
  int numberOfEntities = ec.size();
  int numberShowed = 0;
  search = (String) request.getParameter("search");
%>

<html>
<head>
<title>Entity Reference</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<style>
  .packagetext {font-family: Helvetica,sans-serif; font-size: 18pt; font-weight: bold; text-decoration: none; color: black;}
  .toptext {font-family: Helvetica,sans-serif; font-size: 16pt; font-weight: bold; text-decoration: none; color: black;}
  .titletext {font-family: Helvetica,sans-serif; font-size: 12pt; font-weight: bold; text-decoration: none; color: blue;}
  .headertext {font-family: Helvetica,sans-serif; font-size: 8pt; font-weight: bold; text-decoration: none; background-color: blue; color: white;}
  .enametext {font-family: Helvetica,sans-serif; font-size: 8pt; font-weight: bold; text-decoration: none; color: black;}
  .entitytext {font-family: Helvetica,sans-serif; font-size: 8pt; text-decoration: none; color: black;}
  .relationtext {font-family: Helvetica,sans-serif; font-size: 8pt; text-decoration: none; color: black;}
  A.rlinktext {font-family: Helvetica,sans-serif; font-size: 8pt; font-weight: bold; text-decoration: none; color: blue;}
  A.rlinktext:hover {color:red;}
</style>
</head>

<body bgcolor="#FFFFFF">
<div align="center">

  <DIV class='toptext'>Entity Reference Chart<br/>
    <%=numberOfEntities%> Total Entities
    </DIV>
<%
  Iterator piter = packageNames.iterator();
  while (piter.hasNext()) {
    String pName = (String) piter.next();
    TreeSet entities = (TreeSet) packages.get(pName);
%><A name='<%=pName%>'></A><HR><DIV class='packagetext'><%=pName%></DIV><HR><%
    Iterator i = entities.iterator();
    while (i.hasNext()) {
      String entityName = (String)i.next();
      String helperName = delegator.getEntityHelperName(entityName);
      String groupName = delegator.getEntityGroupName(entityName);
      if (search == null || entityName.toLowerCase().indexOf(search.toLowerCase()) != -1) {
        ModelEntity entity = reader.getModelEntity(entityName);
%>	
  <a name="<%= entityName %>"></a>
  <table width="95%" border="1" cellpadding='2' cellspacing='0'>
    <tr bgcolor="#CCCCCC"> 
      <td colspan="5"> 
        <div align="center" class="titletext">ENTITY: <%=entityName%> | TABLE: <%=entity.getPlainTableName()%></div>
        <div align="center" class="entitytext"><b><%=entity.getTitle()%></b>&nbsp;
            <%if (!forstatic) {%><a target='main' href="<%=response.encodeURL(controlPath + "/FindGeneric?entityName=" + entityName + "&find=true&VIEW_SIZE=50&VIEW_INDEX=0")%>">[view data]</a><%}%></div>
        <%if (entity.getDescription() != null && !entity.getDescription().equalsIgnoreCase("NONE") && !entity.getDescription().equalsIgnoreCase("")) {%>
        <div align="center" class="entitytext"><%=entity.getDescription()%></div>
        <%}%>
      </td>
    </tr>
    <tr class='headertext'>
      <td width="30%" align="center">Java Name</td>
      <td width="30%" align="center">DB Name</td>
      <td width="10%" align="center">Field-Type</td>
      <td width="15%" align="center">Java-Type</td>
      <td width="15%" align="center" nowrap>SQL-Type</td>
    </tr>
	
<%
  TreeSet ufields = new TreeSet();
  for (int y = 0; y < entity.getFieldsSize(); y++) {
    ModelField field = entity.getField(y);	
    ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());
    String javaName = null;
    javaName = field.getIsPk() ? "<span style=\"color: red;\">" + field.getName() + "</span>" : field.getName();
%>	
    <tr bgcolor="#EFFFFF">
      <td><div align="left" class='enametext'><%=javaName%></div></td>
      <td><div align="left" class='entitytext'><%=field.getColName()%></div></td>
      <td><div align="left" class='entitytext'><%=field.getType()%></div></td>
    <%if(type != null){%>
      <td><div align="left" class='entitytext'><%=type.getJavaType()%></div></td>
      <td><div align="left" class='entitytext'><%=type.getSqlType()%></div></td>
    <%}else{%>
      <td><div align="left" class='entitytext'>NOT FOUND</div></td>
      <td><div align="left" class='entitytext'>NOT FOUND</div></td>
    <%}%>
    </tr>
<%	
			}
			if (entity.getRelationsSize() > 0) {
%>
	<tr bgcolor="#FFCCCC">
	  <td colspan="5"><hr></td>
	</tr>
    <tr class='headertext'> 
      <td align="center">Relation</td>
      <td align="center" colspan='4'>Type</td>	  
      
    </tr>
<%
  TreeSet relations = new TreeSet();
  for (int r = 0; r < entity.getRelationsSize(); r++) {
    ModelRelation relation = entity.getRelation(r);
%>
    <tr bgcolor="#FEEEEE"> 
      <td> 
        <div align="left" class='relationtext'>
          <b><%=relation.getTitle()%></b><A href='#<%=relation.getRelEntityName()%>' class='rlinktext'><%=relation.getRelEntityName()%></A>
        </div>
          <%if (relation.getFkName().length() > 0) {%><div class='relationtext'>fk-name: <%=relation.getFkName()%></div><%}%>
      </td>
      <td width="60%" colspan='4'><div align="left" class='relationtext'>
        <%=relation.getType()%>:<%if(relation.getType().length()==3){%>&nbsp;<%}%>
        <%for (int km = 0; km < relation.getKeyMapsSize(); km++){ ModelKeyMap keyMap = relation.getKeyMap(km);%>
          <br/>&nbsp;&nbsp;<%=km+1%>)&nbsp;
          <%if(keyMap.getFieldName().equals(keyMap.getRelFieldName())){%><%=keyMap.getFieldName()%>
          <%}else{%><%=keyMap.getFieldName()%> : <%=keyMap.getRelFieldName()%><%}%>
        <%}%>
      </div></td>
    </tr>				
<%
				}
			}
%>
    <tr bgcolor="#CCCCCC">
	  <td colspan="5">&nbsp;</td>
	</tr>
  </table>
  <br/>
<%
      numberShowed++;
      }
    }
  }
%>  
  <br/><br/>
  <p align="center">Displayed: <%= numberShowed %></p>
</div>

</body>
</html>
<%}else{%>
<html>
<head>
  <title>Entity Editor</title>
</head>
<body>

<H3>Entity Editor</H3>

ERROR: You do not have permission to use this page (ENTITY_MAINT needed)

</body>
</html>
<%}%>
