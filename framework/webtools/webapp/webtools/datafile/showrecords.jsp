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
<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.datafile.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<%List records = (List)request.getAttribute("CUR_RECORD_LIST");%>
<%
  for(int r=0; r<records.size(); r++) {
    Record record = (Record)records.get(r);
    ModelRecord modelRecord = record.getModelRecord();
%>
  <%-- if record is different than the last displayed, make a new table and header row --%>
  <%String lastRecordName = (String)request.getAttribute("LAST_RECORD_NAME");%>
  <%if(!modelRecord.name.equals(lastRecordName)) {%>
    <%if(lastRecordName != null) {%>
    </TABLE><BR><%}%>
    <TABLE cellpadding='2' cellspacing='0' border='1'>
      <TR><TD><B>Record: <%=modelRecord.name%></B></TD><%=UtilFormatOut.ifNotEmpty(modelRecord.parentName, "<TD><B>Parent: ", "</B></TD>")%><TD>&nbsp;<%=modelRecord.description%></TD></TR>
    </TABLE>
    <TABLE cellpadding='2' cellspacing='0' border='1'>
      <TR>
        <%for(int f=0;f<modelRecord.fields.size(); f++) {%>
          <%ModelField modelField = (ModelField)modelRecord.fields.get(f);%>
          <TD><B><%=modelField.name%></B></TD>
        <%}%>
      </TR>
    <%request.setAttribute("LAST_RECORD_NAME", modelRecord.name);%>
  <%}%>

      <TR>
        <%for(int f=0;f<modelRecord.fields.size(); f++) {%>
          <%ModelField modelField = (ModelField)modelRecord.fields.get(f);%>
          <%Object value = record.get(modelField.name);%>
          <%if (value == null) value = modelField.defaultValue;%>
          <%if(value instanceof Double) {%>
            <TD align="right"><%=UtilFormatOut.formatPrice((Double)value)%></TD>
          <%}else{%>
            <TD><%=UtilFormatOut.makeString(value)%></TD>
          <%}%>
        <%}%>
      </TR>
  <%if(record.getChildRecords() != null && record.getChildRecords().size() > 0) {
      request.setAttribute("CUR_RECORD_LIST", record.getChildRecords());
      pageContext.include("/datafile/showrecords.jsp");
    }
  }
%>
