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
<%@ page contentType="text/plain" %><%@ page import="java.util.*, java.io.*, java.net.*, org.apache.ofbiz.base.config.*, org.apache.ofbiz.base.util.*, org.apache.ofbiz.entity.*, org.apache.ofbiz.entity.config.*, org.apache.ofbiz.entity.model.*" %><jsp:useBean id="delegator" type="org.apache.ofbiz.entity.GenericDelegator" scope="request" /><jsp:useBean id="security" type="org.apache.ofbiz.security.Security" scope="request" /><%

  if("true-not-working".equals(request.getParameter("savetofile"))) {
    if(security.hasPermission("ENTITY_MAINT", session)) {
      //save to the file specified in the ModelReader config
      String controlPath=(String)request.getAttribute("_CONTROL_PATH_");
      String serverRootUrl=(String)request.getAttribute("_SERVER_ROOT_URL_");
      ModelGroupReader modelGroupReader = delegator.getModelGroupReader();

      ResourceHandler resourceHandler = null; //modelGroupReader.entityGroupResourceHandler;

      if (resourceHandler.isFileResource()) {
          String filename = resourceHandler.getFullLocation();

          java.net.URL url = new java.net.URL(serverRootUrl + controlPath + "/view/ModelGroupWriter");
          HashMap params = new HashMap();
          HttpClient httpClient = new HttpClient(url, params);
          InputStream in = httpClient.getStream();

          File newFile = new File(filename);
          FileWriter newFileWriter = new FileWriter(newFile);

          BufferedReader post = new BufferedReader(new InputStreamReader(in));
          String line = null;
          while((line = post.readLine()) != null) {
            newFileWriter.write(line);
            newFileWriter.write("\n");
          }
          newFileWriter.close();
          %>
          If you aren't seeing any exceptions, XML was written successfully to:
          <%=filename%>
          from the URL:
          <%=url.toString()%>
      <%
      } else {
        %>ERROR: This entity group information did not come from a file, so it is not being saved. It came from <%=resourceHandler.toString()%><%
      }
    } else {
      %>ERROR: You do not have permission to use this page (ENTITY_MAINT needed)<%
    }
  } else {
%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE entitygroup PUBLIC "-//OFBiz//DTD Entity Group//EN" "http://ofbiz.apache.org/dtds/entitygroup.dtd">
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
  ModelReader reader = delegator.getModelReader();
  ModelGroupReader groupReader = delegator.getModelGroupReader();

  Map packages = new HashMap();
  TreeSet packageNames = new TreeSet();

  //put the entityNames TreeSets in a HashMap by packageName
  Collection ec = reader.getEntityNames();

  Iterator ecIter = ec.iterator();
  while(ecIter.hasNext()) {
    String eName = (String)ecIter.next();
    ModelEntity ent = reader.getModelEntity(eName);
    TreeSet entities = (TreeSet) packages.get(ent.getPackageName());
    if(entities == null) {
      entities = new TreeSet();
      packages.put(ent.getPackageName(), entities);
      packageNames.add(ent.getPackageName());
    }
    entities.add(eName);
  }%>
<entitygroup><%
  Iterator piter = packageNames.iterator();
  while(piter.hasNext()) {
    String pName = (String) piter.next();
    TreeSet entities = (TreeSet) packages.get(pName);
%>

  <!-- ========================================================= -->
  <!-- <%=pName%> -->
  <!-- ========================================================= -->
<%
    Iterator i = entities.iterator();
    while (i.hasNext()) {
      String entityName = (String)i.next();
      String groupName = groupReader.getEntityGroupName(entityName, delegator.getDelegatorName());
      if (groupName == null) groupName = "";
%>
    <entity-group group="<%=groupName%>" entity="<%=entityName%>" /><%
    }
  }%>
</entitygroup>
<%}%>
