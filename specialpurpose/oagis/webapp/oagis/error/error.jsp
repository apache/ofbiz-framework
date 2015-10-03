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
<%@ page import="org.ofbiz.base.util.*" %>
<html>
<head>
<title>OFBiz Message</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>

<% String errorMsg = (String) request.getAttribute("_ERROR_MESSAGE_"); %>

<body bgcolor="#FFFFFF">
<div align="center">
  <br/>
  <table width="100%" border="1" height="200">
    <tr>
      <td>
        <table width="100%" border="0" height="200">
          <tr bgcolor="#CC6666">
            <td height="45">
              <div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="4" color="#FFFFFF"><b>:ERROR MESSAGE:</b></font></div>
            </td>
          </tr>
          <tr>
            <td>
              <div align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%=UtilFormatOut.replaceString(errorMsg, "\n", "<br/>")%></font></div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</div>
<div align="center"></div>
</body>
</html>
