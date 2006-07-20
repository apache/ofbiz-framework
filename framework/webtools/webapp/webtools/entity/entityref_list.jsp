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
<%@ page import="org.ofbiz.base.util.*" %>
<%@ page import="org.ofbiz.entity.*" %>
<%@ page import="org.ofbiz.entity.model.*" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<%String controlPath=(String)request.getAttribute("_CONTROL_PATH_");%>

<% 
if(security.hasPermission("ENTITY_MAINT", session)) {
  boolean forstatic = "true".equals(request.getParameter("forstatic"));
  String search = null;
  ModelReader reader = delegator.getModelReader();
  Collection ec = reader.getEntityNames();
  TreeSet entities = new TreeSet(ec);
  search = (String) request.getParameter("search");

  TreeSet packageNames = new TreeSet();

  //put the packageNames in a TreeSet
  Iterator ecIter = ec.iterator();
  while(ecIter.hasNext())
  {
    String eName = (String)ecIter.next();
    ModelEntity ent = reader.getModelEntity(eName);
    packageNames.add(ent.getPackageName());
  }
%>

<html>
<head>
<title>Entity Reference</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<style>
  A.listtext {font-family: Helvetica,sans-serif; font-size: 10pt; font-weight: bold; text-decoration: none; color: blue;}
  A.listtext:hover {color:red;}
</style>

</head>

<body bgcolor="#FFFFFF">
<div align="left">

<%if (!forstatic) {%>
<a href="<%=response.encodeURL(controlPath + "/main")%>" target='main' class='listtext'>Pop up WebTools main</A><BR>
<a href="<%=response.encodeURL(controlPath + "/view/entityref_main")%>" target="entityFrame" class='listtext'>Entity Reference Main Page</A><BR>
<a href="<%=response.encodeURL(controlPath + "/view/checkdb")%>" target="entityFrame" class='listtext'>Check/Update Database</A>
<HR>
<!-- want to leave these out because they are only working so-so, and cause people more problems that they solve, IMHO
<a href="<%=response.encodeURL(controlPath + "/ModelWriter")%>" target='_blank' class='listtext'>Generate Entity Model XML (all in one)</A><BR>
<a href="<%=response.encodeURL(controlPath + "/ModelWriter?savetofile=true")%>" target='_blank' class='listtext'>Save Entity Model XML to Files</A><BR>
-->
<!-- this is not working now anyway...
<a href="<%=response.encodeURL(controlPath + "/ModelGroupWriter")%>" target='_blank' class='listtext'>Generate Entity Group XML</A><BR>
<a href="<%=response.encodeURL(controlPath + "/ModelGroupWriter?savetofile=true")%>" target='_blank' class='listtext'>Save Entity Group XML to File</A><BR>
-->
<a href="<%=response.encodeURL(controlPath + "/view/ModelInduceFromDb")%>" target='_blank' class='listtext'>Induce Model XML from Database</A><BR>
<HR>
<%}%>

<%
  Iterator piter = packageNames.iterator();
  while(piter.hasNext()) {
    String pName = (String)piter.next();
    if (forstatic) {%>
	    <a href="entityref_main.html#<%=pName%>" target="entityFrame" class='listtext'><%=pName%></a><br/>
    <%} else {%>
	    <a href="<%=response.encodeURL(controlPath + "/view/entityref_main#" + pName)%>" target="entityFrame" class='listtext'><%=pName%></a><br/>
    <%}%>
<%}%>

<HR>

<%
	Iterator i = entities.iterator();
	while ( i.hasNext() ) {
		Object o = i.next();
		String entityName = (String) o;
		if ( search == null || entityName.toLowerCase().indexOf(search.toLowerCase()) != -1 ) {						
			String url = search == null ? "entityref_main#"+entityName : "entityref_main#"+entityName+"?search="+search;
%>	
  <%if (forstatic) {%>
	<a href="entityref_main.html#<%=entityName%>" target="entityFrame" class='listtext'><%= entityName %></a>
  <%} else {%>
    <!-- want to leave these out because they are only working so-so, and cause people more problems that they solve, IMHO
	<a href="<%=response.encodeURL(controlPath + "/view/EditEntity?entityName=" + entityName)%>" target="_blank" class='listtext'>[EditDef]</a>
	-->
	<a href="<%=response.encodeURL(controlPath + "/view/" + url)%>" target="entityFrame" class='listtext'><%= entityName %></a>
  <%}%>
<br/>
<%
		}	
	}
%>  
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
