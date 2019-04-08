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

<#assign delegator = requestAttributes.delegator>
<#if communicationEvent.partyIdFrom??>
  <#assign fromName =
      Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdFrom, true)>
</#if>
<#if communicationEvent.partyIdTo??>
  <#assign toName =
      Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdTo, true)>
</#if>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <div class="boxlink">
    <#if (communicationEvent.partyIdFrom! != (userLogin.partyId)!)>
      <a href="<@ofbizUrl>newmessage?communicationEventId=${communicationEvent.communicationEventId}</@ofbizUrl>"
          class="submenutext">${uiLabelMap.PartyReply}
      </a>
    </#if>
      <a href="<@ofbizUrl>messagelist</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceViewList}</a>
    </div>
    <div class="h3">${uiLabelMap.EcommerceReadMessage}</div>
  </div>
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="1">
      <tr>
        <td>&nbsp;</td>
      </tr>
      <tr>
        <td align="right">
          <div class="tableheadtext">${uiLabelMap.CommonFrom}:</div>
        </td>
        <td>
          <div>${fromName!}</div>
        </td>
      </tr>
      <tr>
        <td align="right">
          <div class="tableheadtext">${uiLabelMap.CommonTo}:</div>
        </td>
        <td>
          <div>${toName!}</div>
        </td>
      </tr>
      <tr>
        <td align="right">
          <div class="tableheadtext">${uiLabelMap.CommonDate}:</div>
        </td>
        <td>
          <div>${communicationEvent.entryDate}</div>
        </td>
      </tr>
      <tr>
        <td align="right">
          <div class="tableheadtext">${uiLabelMap.EcommerceSubject}:</div>
        </td>
        <td>
          <div>&nbsp;${(communicationEvent.subject)?default("[${uiLabelMap.EcommerceNoSubject}]")}</div>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>
          <div>${StringUtil.wrapString(communicationEvent.content)?default("[${uiLabelMap.EcommerceEmptyBody}]")}</div>
        </td>
      </tr>
    </table>
  </div>
</div>
