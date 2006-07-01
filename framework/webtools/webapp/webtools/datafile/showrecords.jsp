<%--
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
 
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 *
 * @author David E. Jones (jonesde@ofbiz.org)
 * @version 1.0
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
