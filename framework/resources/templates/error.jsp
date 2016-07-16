<%@ page import="org.apache.ofbiz.base.util.*" %>
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