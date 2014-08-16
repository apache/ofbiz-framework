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

<#macro showMessage communicationEvent isSentMessage index>
  <#if communicationEvent.partyIdFrom?has_content>
    <#assign partyNameFrom = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdFrom, true)>
  <#else/>
    <#assign partyNameFrom = "${uiLabelMap.CommonNA}">
  </#if>
  <#if communicationEvent.partyIdTo?has_content>
    <#assign partyNameTo = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdTo, true)>
  <#else/>
    <#assign partyNameTo = "${uiLabelMap.CommonNA}">
  </#if>
              <tr>
                <td><div>${partyNameFrom}</div></td>
                <td><div>${partyNameTo}</div></td>
                <td><div>${communicationEvent.subject?default("")}</div></td>
                <td><div>${communicationEvent.entryDate}</div></td>
                <td align="right">
                  <form method="post" action="<@ofbizUrl>readmessage</@ofbizUrl>" name="ecomm_read_mess${index}">
                    <input name="communicationEventId" value="${communicationEvent.communicationEventId}" type="hidden"/>
                  </form>
                  <a href="javascript:document.ecomm_read_mess${index}.submit()">${uiLabelMap.EcommerceRead}</a>
                  
                  <#if isSentMessage>
                  <form method="post" action="<@ofbizUrl>newmessage</@ofbizUrl>" name="ecomm_sent_mess${index}">
                    <input name="communicationEventId" value="${communicationEvent.communicationEventId}" type="hidden"/>
                  </form>
                  <a href="javascript:document.ecomm_sent_mess${index}.submit()">${uiLabelMap.PartyReply}</a>
                  </#if>
                </td>
              </tr>
</#macro>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="boxlink">
            <#if parameters.showSent! == "true">
              <a href="<@ofbizUrl>messagelist</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceViewReceivedOnly}</a>
            <#else>
              <a href="<@ofbizUrl>messagelist?showSent=true</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceViewSent}</a>
            </#if>
        </div>
        <div class="h3">${uiLabelMap.CommonMessages}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <#if (!receivedCommunicationEvents?has_content && !sentCommunicationEvents?has_content)>
            <tr><td><div>${uiLabelMap.EcommerceNoMessages}.</div></td></tr>
          <#else/>
            <tr>
              <td><div class="tableheadtext">${uiLabelMap.CommonFrom}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.CommonTo}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.EcommerceSubject}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.EcommerceSentDate}</div></td>
              <td>&nbsp;</td>
            </tr>
            <tr><td colspan="5"><hr /></td></tr>
            <#list receivedCommunicationEvents! as receivedCommunicationEvent>
              <@showMessage communicationEvent=receivedCommunicationEvent isSentMessage=false index=receivedCommunicationEvent_index/>
            </#list>
            <#list sentCommunicationEvents! as sentCommunicationEvent>
              <@showMessage communicationEvent=sentCommunicationEvent isSentMessage=true index=sentCommunicationEvent_index/>
            </#list>
          </#if>
        </table>
    </div>
</div>
