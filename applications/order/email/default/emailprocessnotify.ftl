<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.2
-->

<html>
<head>  
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>

<body>

<h1>Attention!</h1>
<div>&nbsp;</div>

<table width="70%">
  <tr>
    <td align="right"><b>Order #:</b></td>
    <td>${orderId?if_exists}</td>
  </tr>
  <tr>
    <td align="right"><b>Order Date:</b></td>
    <td>${orderDate?if_exists}</td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td align="right"><b>Estimated Start Date:</b></td>
    <td>${estimatedStartDate?if_exists}</td>
  </tr>
  <tr>
    <td align="right"><b>Actual Start Date:</b></td>
    <td>${actualStartDate?if_exists}</td>
  </tr>
  <tr>
    <td align="right"><b>Current State:<b></td>
    <td>${omgStatusId?if_exists} <#--WfUtil.getOMGStatus(request.getParameter("currentStatusId"))--></td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
 
  <#list assignments as assign>
  <tr>
    <td align="right"><b>Assigned Party ID:</b></td>
    <td>${assign.partyId?if_exists}</td>
  </tr>
  <tr>
    <td align="right"><b>Assigned Role Type:</b></td>
    <td>${assign.roleTypeId?if_exists}</td>
  </tr>
  <tr>
    <td align="right"><b>Assignment Status:</b></td>
    <td>${assign.statusId?if_exists}</td>
  </tr>
  </#list>
  
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="2' align="center">
	  <a href="${baseUrl}/ordermgr/control/orderview?orderId=${orderId}">View Order #${orderId}</a>
    </td>
  </tr>
</table>

</body>
</html>
