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

<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.datafile.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<%
  List messages = new LinkedList();

  String dataFileSave = request.getParameter("DATAFILE_SAVE");

  String entityXmlFileSave = request.getParameter("ENTITYXML_FILE_SAVE");

  String dataFileLoc = request.getParameter("DATAFILE_LOCATION");
  String definitionLoc = request.getParameter("DEFINITION_LOCATION");
  String definitionName = request.getParameter("DEFINITION_NAME");
  boolean dataFileIsUrl = request.getParameter("DATAFILE_IS_URL")!=null?true:false;
  boolean definitionIsUrl = request.getParameter("DEFINITION_IS_URL")!=null?true:false;

  URL dataFileUrl = null;
  try { dataFileUrl = dataFileIsUrl?new URL(dataFileLoc):UtilURL.fromFilename(dataFileLoc); }
  catch (java.net.MalformedURLException e) { messages.add(e.getMessage()); }

  URL definitionUrl = null;
  try { definitionUrl = definitionIsUrl?new URL(definitionLoc):UtilURL.fromFilename(definitionLoc); }
  catch (java.net.MalformedURLException e) { messages.add(e.getMessage()); }

  Iterator definitionNames = null;
  if (definitionUrl != null) {
    ModelDataFileReader reader = ModelDataFileReader.getModelDataFileReader(definitionUrl);
    if (reader != null) {
        definitionNames = ((Collection)reader.getDataFileNames()).iterator();
    }
  }
  
  DataFile dataFile = null;
  if (dataFileUrl != null && definitionUrl != null && definitionName != null && definitionName.length() > 0) {
    try { dataFile = DataFile.readFile(dataFileUrl, definitionUrl, definitionName); }
    catch (Exception e) { messages.add(e.toString()); Debug.log(e); }
  }

  ModelDataFile modelDataFile = null;
  if (dataFile != null) modelDataFile = dataFile.getModelDataFile();

  if (dataFile != null && dataFileSave != null && dataFileSave.length() > 0) {
    try {
      dataFile.writeDataFile(dataFileSave);
      messages.add("Data File saved to: " + dataFileSave);
    } catch (Exception e) { messages.add(e.getMessage()); }
  }

  if (dataFile != null && entityXmlFileSave != null && entityXmlFileSave.length() > 0) {
    try {
      //dataFile.writeDataFile(entityXmlFileSave);
      DataFile2EntityXml.writeToEntityXml(entityXmlFileSave, dataFile);
      messages.add("Entity File saved to: " + entityXmlFileSave);
    } catch (Exception e) { messages.add(e.getMessage()); }
  }

%>
<div class="head1">Work With Data Files</div>
<div class="tabletext">This page is used to view and export data from data files parsed by the configurable data file parser.</div>
<hr/>
<%if(security.hasPermission("DATAFILE_MAINT", session)) {%>
  <form method="post" action="<ofbiz:url>/viewdatafile</ofbiz:url>">
    <div class="tabletext">Definition Filename or URL: <input name="DEFINITION_LOCATION" class="inputBox" type="text" size="60" value="<%=UtilFormatOut.checkNull(definitionLoc)%>"> Is URL?:<INPUT type="checkbox" name="DEFINITION_IS_URL" <%=definitionIsUrl?"checked":""%>></div>
    <div class="tabletext">Data File Definition Name: 
    <% if (definitionNames != null) {
        %><select name="DEFINITION_NAME" class="selectBox">
          <option value=""></option>
        <%
        while (definitionNames.hasNext()) {
            String oneDefinitionName = (String)definitionNames.next();
            boolean isSelected = definitionName != null && definitionName.equals(oneDefinitionName);
            %><option value="<%=oneDefinitionName%>" <%=(isSelected? "selected": "")%>><%=oneDefinitionName%></option><%
        }
        %></select><%
    } else {%>
    <input name="DEFINITION_NAME" type="text" class="inputBox" size="30" value="<%=UtilFormatOut.checkNull(definitionName)%>"></div>
    <% } %>
    <div class="tabletext">Data Filename or URL: <input name="DATAFILE_LOCATION" type="text" class="inputBox" size="60" value="<%=UtilFormatOut.checkNull(dataFileLoc)%>"> Is URL?:<INPUT type="checkbox" name="DATAFILE_IS_URL" <%=dataFileIsUrl?"checked":""%>></div>
    <div class="tabletext">Save to file: <input name="DATAFILE_SAVE" type="text" class="inputBox" size="60" value="<%=UtilFormatOut.checkNull(dataFileSave)%>"/></div>
    <div class="tabletext">Save to entity xml file: <input name="ENTITYXML_FILE_SAVE" type="text" class="inputBox" size="60" value="<%=UtilFormatOut.checkNull(entityXmlFileSave)%>"></div>
    <div><input type="submit" value="Run"></div>
  </form>

  <hr/>

  <%if (messages.size() > 0) {%>
    <div class="head1">The following occurred:</div>
    <ul>
    <%Iterator errMsgIter = messages.iterator();%>
    <%while (errMsgIter.hasNext()) {%>
      <li><%=errMsgIter.next()%>
    <%}%>
    </ul>
  <%}%>

  <%if (dataFile != null && modelDataFile != null && (entityXmlFileSave == null || entityXmlFileSave.length() == 0) && (dataFileSave == null || dataFileSave.length() == 0)) {%>

    <table cellpadding="2" cellspacing="0" border="1">
      <tr>
        <td><B>Name</B></td>
        <td><B>Type-Code</B></td>
        <td><B>Sender</B></td>
        <td><B>Receiver</B></td>
        <td><B>Record Length</B></td>
        <td><B>Separator Style</B></td>
      </tr>
      <tr>
        <td><%=modelDataFile.name%></td>
        <td><%=modelDataFile.typeCode%></td>
        <td><%=modelDataFile.sender%></td>
        <td><%=modelDataFile.receiver%></td>
        <td><%=modelDataFile.recordLength%></td>
        <td><%=modelDataFile.separatorStyle%></td>
      </tr>
      <tr>
        <td colspan="6">Description: <%=modelDataFile.description%></td>
      </tr>
    </table>
    <BR>
    <%request.setAttribute("CUR_RECORD_LIST", dataFile.getRecords());%>
    <%pageContext.include("/datafile/showrecords.jsp");%>
  <%}%>

<%}else{%>
  <hr/>
  <div class="tabletext">You do not have permission to use this page (DATAFILE_MAINT needed)</div>
<%}%>
