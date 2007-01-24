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
<br/>

<FORM METHOD="post" NAME="orderreportform" ACTION="<@ofbizUrl>orderreportjasper.pdf</@ofbizUrl>" TARGET="OrderReport">   
<Table>
<TR>
<TD><div class="tableheadtext">From Date:</div></td>
<td><INPUT TYPE="TEXT" NAME="fromDate" TABINDEX="10"  SIZE="22" MAXLENGTH="25" ALIGN="MIDDLE">
 <A TABINDEX="10" TARGET="_self" HREF="javascript:call_cal(document.orderreportform.fromDate, '${fromStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <IMG SRC='/images/cal.gif' WIDTH='16' HEIGHT='16' BORDER='0' ALT='Click here For Calendar'>
 </A>
</TD>
</TR>
<TR>
<TD><div class="tableheadtext">To Date:</div></td>
<td><INPUT TYPE="TEXT" NAME="toDate" TABINDEX="12"  SIZE="22" MAXLENGTH="25" ALIGN="MIDDLE">
 <A TABINDEX="12" TARGET="_self" HREF="javascript:call_cal(document.orderreportform.toDate, '${toStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <IMG SRC='/images/cal.gif' WIDTH='16' HEIGHT='16' BORDER='0' ALT='Click here For Calendar'>
 </A>
</TD>
</TR>
<#--
<tr>
<td><div class="tableheadtext">Report:</div></td>
<td>
   <SELECT NAME="groupName" tabindex="14"  CLASS="stateSelectBox">
	 <OPTION VALUE="orderStatus"></OPTION>
	 <OPTION VALUE="orderStatus">Orders by Order Status</OPTION>
	 <OPTION VALUE="ship">Orders by Ship Method</OPTION>
	 <OPTION VALUE="payment">Orders by Payment Method</OPTION>
	 <OPTION VALUE="adjustment">Order Items by Adjustment</OPTION>
	 <OPTION VALUE="itemStatus">Order Items by Status</OPTION>
	 <OPTION VALUE="product">Order Items by Product</OPTION>
   </SELECT>
</td>
</tr>
-->
</table>
 <INPUT TYPE="submit" TABINDEX="16" CLASS="button" NAME="GoReport" VALUE="Order Report">
</form>

<FORM METHOD="post" NAME="itemreportform" ACTION="<@ofbizUrl>orderitemreportjasper.pdf</@ofbizUrl>" TARGET="OrderReport">   
<Table>
<TR>
<TD><div class="tableheadtext">From Date:</div></td>
<td><INPUT TYPE="TEXT" NAME="fromDate" TABINDEX="10"  SIZE="22" MAXLENGTH="25" ALIGN="MIDDLE">
 <A TABINDEX="10" TARGET="_self" HREF="javascript:call_cal(document.itemreportform.fromDate, '${fromStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <IMG SRC='/images/cal.gif' WIDTH='16' HEIGHT='16' BORDER='0' ALT='Click here For Calendar'>
 </A>
</TD>
</TR>
<TR>
<TD><div class="tableheadtext">To Date:</div></td>
<td><INPUT TYPE="TEXT" NAME="toDate" TABINDEX="12"  SIZE="22" MAXLENGTH="25" ALIGN="MIDDLE">
 <A TABINDEX="12" TARGET="_self" HREF="javascript:call_cal(document.itemreportform.toDate, '${toStr}');" onfocus="checkForChanges = true;" onblur="checkForChanges = true;">
  <IMG SRC='/images/cal.gif' WIDTH='16' HEIGHT='16' BORDER='0' ALT='Click here For Calendar'>
 </A>
</TD>
</TR>
<#--
<tr>
<td><div class="tableheadtext">Report:</div></td>
<td>
   <SELECT NAME="groupName" tabindex="14"  CLASS="stateSelectBox">
	 <OPTION VALUE="orderStatus"></OPTION>
	 <OPTION VALUE="orderStatus">Orders by Order Status</OPTION>
	 <OPTION VALUE="ship">Orders by Ship Method</OPTION>
	 <OPTION VALUE="payment">Orders by Payment Method</OPTION>
	 <OPTION VALUE="adjustment">Order Items by Adjustment</OPTION>
	 <OPTION VALUE="itemStatus">Order Items by Status</OPTION>
	 <OPTION VALUE="product">Order Items by Product</OPTION>
   </SELECT>
</td>
</tr>
-->
</table>
 <INPUT TYPE="submit" TABINDEX="16" CLASS="button" NAME="GoReport" VALUE="Item Report">
</form>
