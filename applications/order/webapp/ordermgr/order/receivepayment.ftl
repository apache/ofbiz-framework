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

<#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
  <p class="head1">${uiLabelMap.OrderReceiveOfflinePayments}</p>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonBack}]</a>
  &nbsp;<a href="javascript:document.paysetupform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>

  <form method="post" action="<@ofbizUrl>receiveOfflinePayments/${donePage}</@ofbizUrl>" name="paysetupform">    
    <#if requestParameters.workEffortId?exists>
    	<input type="hidden" name="workEffortId" value="${requestParameters.workEffortId}">
    </#if>
    <table width="100%" cellpadding="1" cellspacing="0" border="0">
      <tr>
        <td width="30%" align="right"><div class="tableheadtext"><u>${uiLabelMap.OrderPaymentType}</u></div></td>
        <td width="1">&nbsp;&nbsp;&nbsp;</td>
        <td width="1" align="left"><div class="tableheadtext"><u>${uiLabelMap.OrderAmount}</u></div></td>
        <td width="1">&nbsp;&nbsp;&nbsp;</td>
        <td width="70%" align="left"><div class="tableheadtext"><u>${uiLabelMap.OrderReference}</u></div></td>
      </tr>    
      <#list paymentMethodTypes as payType>
      <tr>
        <td width="30%" align="right"><div class="tabletext">${payType.get("description",locale)?default(payType.paymentMethodTypeId)}</div></td>
        <td width="1">&nbsp;&nbsp;&nbsp;</td>
        <td width="1"><input type="text" size="7" name="${payType.paymentMethodTypeId}_amount" class="inputBox"></td>
        <td width="1">&nbsp;&nbsp;&nbsp;</td>
        <td width="70%"><input type="text" size="15" name="${payType.paymentMethodTypeId}_reference" class="inputBox"></td>
      </tr>
      </#list>
    </table>
  </form>
  
  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonBack}]</a>
  &nbsp;<a href="javascript:document.paysetupform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
   
<br/>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
