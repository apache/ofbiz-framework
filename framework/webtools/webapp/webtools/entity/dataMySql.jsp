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
<%@ page contentType="text/plain" %><%@ page import="java.util.*" %><%@ page import="org.ofbiz.entity.*" %><%@ page import="org.ofbiz.entity.model.*" %><jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" /><jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" /><%
if(security.hasPermission("ENTITY_MAINT", session)) {
  ModelReader reader = delegator.getModelReader();
  Collection ec = reader.getEntityNames();
  TreeSet entities = new TreeSet(ec);
  Iterator classNamesIterator = entities.iterator();
  while(classNamesIterator != null && classNamesIterator.hasNext()) { ModelEntity entity = reader.getModelEntity((String)classNamesIterator.next());%>
INSERT INTO SECURITY_PERMISSION (PERMISSION_ID,DESCRIPTION) VALUES ('<%=entity.getPlainTableName()%>_ADMIN','Permission to Administer a <%=entity.getEntityName()%> entity.');
INSERT INTO SECURITY_PERMISSION (PERMISSION_ID,DESCRIPTION) VALUES ('<%=entity.getPlainTableName()%>_VIEW','Permission to View a <%=entity.getEntityName()%> entity.');
INSERT INTO SECURITY_PERMISSION (PERMISSION_ID,DESCRIPTION) VALUES ('<%=entity.getPlainTableName()%>_CREATE','Permission to Create a <%=entity.getEntityName()%> entity.');
INSERT INTO SECURITY_PERMISSION (PERMISSION_ID,DESCRIPTION) VALUES ('<%=entity.getPlainTableName()%>_UPDATE','Permission to Update a <%=entity.getEntityName()%> entity.');
INSERT INTO SECURITY_PERMISSION (PERMISSION_ID,DESCRIPTION) VALUES ('<%=entity.getPlainTableName()%>_DELETE','Permission to Delete a <%=entity.getEntityName()%> entity.');

INSERT INTO SECURITY_GROUP_PERMISSION (GROUP_ID,PERMISSION_ID) VALUES ('FULLADMIN','<%=entity.getPlainTableName()%>_ADMIN');
INSERT INTO SECURITY_GROUP_PERMISSION (GROUP_ID,PERMISSION_ID) VALUES ('FLEXADMIN','<%=entity.getPlainTableName()%>_VIEW');
INSERT INTO SECURITY_GROUP_PERMISSION (GROUP_ID,PERMISSION_ID) VALUES ('FLEXADMIN','<%=entity.getPlainTableName()%>_CREATE');
INSERT INTO SECURITY_GROUP_PERMISSION (GROUP_ID,PERMISSION_ID) VALUES ('FLEXADMIN','<%=entity.getPlainTableName()%>_UPDATE');
INSERT INTO SECURITY_GROUP_PERMISSION (GROUP_ID,PERMISSION_ID) VALUES ('FLEXADMIN','<%=entity.getPlainTableName()%>_DELETE');
<%
  }
} 
else {
  %>ERROR: You do not have permission to use this page (ENTITY_MAINT needed)<%
}
%>
