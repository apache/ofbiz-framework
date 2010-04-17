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

<div class='tabletext'>NOTE: These report are for demonstration purposes only.
They use the JasperReports reporting tool. They have not been polished yet, but
they are good examples of creating detailed reports that you have a lot of
control over. special thanks for Britton LaRoche for creating the first pass of
these reports and helping to improve them.</div>
<br />

<form method="post" name="orderreportform" action="<@ofbizUrl>orderreportjasper.pdf</@ofbizUrl>" target="OrderReport">
<table>
<tr>
<td><div>From Date:</div></td>
<td><input type="text" name="fromDate" tabindex="10"  size="22" maxlength="25" align="middle">
 <a tabindex="10" target="_self" href="javascript:call_cal(document.orderreportform.fromDate, '${fromStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar' />
 </a>
</td>
</tr>
<tr>
<td><div>To Date:</div></td>
<td><input type="text" name="toDate" tabindex="12"  size="22" maxlength="25" align="middle">
 <a tabindex="12" target="_self" href="javascript:call_cal(document.orderreportform.toDate, '${toStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar' />
 </a>
</td>
</tr>
<#--
<tr>
<td><div>Report:</div></td>
<td>
   <select name="groupName" tabindex="14"  CLASS="stateSelectBox">
     <option value="orderStatus"></option>
     <option value="orderStatus">Orders by Order Status</option>
     <option value="ship">Orders by Ship Method</option>
     <option value="payment">Orders by Payment Method</option>
     <option value="adjustment">Order Items by Adjustment</option>
     <option value="itemStatus">Order Items by Status</option>
     <option value="product">Order Items by Product</option>
   </select>
</td>
</tr>
-->
</table>
 <input type="submit" tabindex="16" class="button" name="GoReport" value="Order Report">
</form>

<form method="post" name="itemreportform" action="<@ofbizUrl>orderitemreportjasper.pdf</@ofbizUrl>" target="OrderReport">
<table>
<tr>
<td><div>From Date:</div></td>
<td><input type="text" name="fromDate" tabindex="10"  size="22" maxlength="25" align="middle">
 <a tabindex="10" target="_self" href="javascript:call_cal(document.itemreportform.fromDate, '${fromStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar' />
 </a>
</td>
</tr>
<tr>
<td><div>To Date:</div></td>
<td><input type="text" name="toDate" tabindex="12"  size="22" maxlength="25" align="middle">
 <a tabindex="12" target="_self" href="javascript:call_cal(document.itemreportform.toDate, '${toStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar' />
 </a>
</td>
</tr>
<#--
<tr>
<td><div>Report:</div></td>
<td>
   <select name="groupName" tabindex="14"  class="stateSelectBox">
     <option value="orderStatus"></option>
     <option value="orderStatus">Orders by Order Status</option>
     <option value="ship">Orders by Ship Method</option>
     <option value="payment">Orders by Payment Method</option>
     <option value="adjustment">Order Items by Adjustment</option>
     <option value="itemStatus">Order Items by Status</option>
     <option value="product">Order Items by Product</option>
   </select>
</td>
</tr>
-->
</table>
 <input type="submit" tabindex="16" class="button" name="GoReport" value="Item Report">
</form>
