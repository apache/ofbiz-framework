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
<%@ page contentType="text/plain" %><%@ page import="java.util.*" %><%@ page import="org.apache.ofbiz.entity.*" %><%@ page import="org.apache.ofbiz.entity.model.*" %><jsp:useBean id="delegator" type="org.apache.ofbiz.entity.GenericDelegator" scope="request" /><jsp:useBean id="security" type="org.apache.ofbiz.security.Security" scope="request" /><%
if(security.hasPermission("ENTITY_MAINT", session)) {
  ModelReader reader = delegator.getModelReader();
  Collection<String> ec = reader.getEntityNames();
  TreeSet<String> entities = new TreeSet<String>(ec);
  Iterator<String> classNamesIterator = entities.iterator();
  while(classNamesIterator != null && classNamesIterator.hasNext()) { ModelEntity entity = reader.getModelEntity((String)classNamesIterator.next());%>
CREATE TABLE <%=entity.getPlainTableName()%> (<%for (String fieldName : entity.getAllFieldNames()){ModelField field=entity.getField(fieldName); ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());%><%if(field.getIsPk()){%>
  <%=field.getColName()%> <%=type.getSqlType()%> NOT NULL,<%}else{%>
  <%=field.getColName()%> <%=type.getSqlType()%>,<%}%><%}%>
  CONSTRAINT PK_<%=entity.getPlainTableName()%> PRIMARY KEY (<%=entity.colNameString(entity.getPkFieldsUnmodifiable())%>));
<%}%>
<%
}
else {
  %>ERROR: You do not have permission to use this page (ENTITY_MAINT needed)<%
}
%>
