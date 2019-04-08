<#--
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

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
</head>

<body>

<h1>Attention!</h1>
<div>&nbsp;</div>

<table width="70%">
  <tr>
    <td align="right"><b>Order #:</b></td>
    <td>${orderId!}</td>
  </tr>
  <tr>
    <td align="right"><b>Order Date:</b></td>
    <td>${orderDate!}</td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td align="right"><b>Estimated Start Date:</b></td>
    <td>${estimatedStartDate!}</td>
  </tr>
  <tr>
    <td align="right"><b>Actual Start Date:</b></td>
    <td>${actualStartDate!}</td>
  </tr>
  <tr>
    <td align="right"><b>Current State:<b></td>
    <td>${omgStatusId!} <#--WfUtil.getOMGStatus(request.getParameter("currentStatusId"))--></td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <#list assignments as assign>
  <tr>
    <td align="right"><b>Assigned Party ID:</b></td>
    <td>${assign.partyId!}</td>
  </tr>
  <tr>
    <td align="right"><b>Assigned Role Type:</b></td>
    <td>${assign.roleTypeId!}</td>
  </tr>
  <tr>
    <td align="right"><b>Assignment Status:</b></td>
    <td>${assign.statusId!}</td>
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
