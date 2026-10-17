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
<div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.OrderReceiveOfflinePayments}</li>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBack}</a>
      <a href="javascript:document.paysetupform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>

      <form method="post" action="<@ofbizUrl>receiveOfflinePayments/${donePage}</@ofbizUrl>" name="paysetupform">
        <#if requestParameters.workEffortId??>
            <input type="hidden" name="workEffortId" value="${requestParameters.workEffortId}" />
        </#if>
        <input type="hidden" name="partyId" value="${orderRoles[0].partyId}" />

        <#if paymentMethods?has_content>
        <table class="basic-table" cellspacing='0'>
          <tr class="header-row">
            <td width="30%" align="right">${uiLabelMap.PaymentMethod}</td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="1">${uiLabelMap.OrderAmount}</td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="70%">${uiLabelMap.OrderReference}</td>
          </tr>
          <#list paymentMethods as payMethod>
          <tr>
            <td width="30%" align="right">${payMethod.get("description",locale)?default(payMethod.paymentMethodId)}</td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="1"><input type="text" size="7" name="${payMethod.paymentMethodId}_amount" /></td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="70%"><input type="text" size="15" name="${payMethod.paymentMethodId}_reference" /></td>
          </tr>
          </#list>
        </table>
        </#if>
        <br /> <br />
        <#if paymentMethodTypes?has_content>
        <table class="basic-table" cellspacing='0'>
          <tr class="header-row">
            <td width="30%" align="right">${uiLabelMap.OrderPaymentType}</td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="1">${uiLabelMap.OrderAmount}</td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="70%">${uiLabelMap.OrderReference}</td>
          </tr>
          <#list paymentMethodTypes as payType>
          <tr>
            <td width="30%" align="right">${payType.get("description",locale)?default(payType.paymentMethodTypeId)}</td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="1"><input type="text" size="7" name="${payType.paymentMethodTypeId}_amount" /></td>
            <td width="1">&nbsp;&nbsp;&nbsp;</td>
            <td width="70%"><input type="text" size="15" name="${payType.paymentMethodTypeId}_reference" /></td>
          </tr>
          </#list>
        </table>
        </#if>
      </form>

      <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBack}</a>
      <a href="javascript:document.paysetupform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
    </div>
</div>
<br />
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>