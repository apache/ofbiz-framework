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

<#if partyId?exists>
<div style="text-align: right;">
    <a href="<@ofbizUrl>viewCommunicationEvent?partyIdFrom=${partyId}&partyId=${partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyNewCommunication}</a>
</div>
</#if>

<br/>
<table class="basic-table" cellspacing="0">
  <#assign target = requestAttributes.targetRequestUri>
  <#if partyId?exists>
    <#assign target = target + "?partyId=" + partyId + "&previousSort=" + previousSort>
  <#else>
    <#assign target = target + "?previousSort=" + previousSort>
  </#if>

  <#if eventList?has_content>
    <tr>
      <td colspan="9" width="50%">
        <div align='right'>
          <#if (eventListSize > 0)>
            <#if (viewIndex > 1)>
              <a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>
            <#else>
              &nbsp;
            </#if>
            <#if (eventListSize > 0)>
              <span class="label">${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${eventListSize}</span>
            </#if>
            <#if (eventListSize > highIndex)>
              <a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
            <#else>
              &nbsp;
            </#if>
          </#if>
          &nbsp;
        </div>
      </td>
    </tr>    
    <tr><td colspan="9">&nbsp;</td></tr>
  </#if>

  <tr>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=communicationEventId</@ofbizUrl>">${uiLabelMap.PartyCommEvent} #</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=communicationEventTypeId</@ofbizUrl>">${uiLabelMap.PartyType}</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=contactMechTypeId</@ofbizUrl>">${uiLabelMap.PartyContactType}</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=statusId</@ofbizUrl>">${uiLabelMap.PartyStatus}</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=subject</@ofbizUrl>">${uiLabelMap.PartySubject}</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=partyIdFrom</@ofbizUrl>">${uiLabelMap.PartyPartyFrom}</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=partyIdTo</@ofbizUrl>">${uiLabelMap.PartyPartyTo}</a></td>
    <td><a href="<@ofbizUrl>${target}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&sort=entryDate</@ofbizUrl>">${uiLabelMap.PartyEnteredDate}</a></td>
  </tr> 
  <tr><td colspan="9"><hr></td></tr>
  <#if eventList?has_content>
    <#list eventList as event>
      <#assign eventType = event.getRelatedOne("CommunicationEventType")?if_exists>
      <#assign contactMechType = event.getRelatedOne("ContactMechType")?if_exists>
      <#if event.statusId?exists>
        <#assign statusItem = event.getRelatedOne("StatusItem")>
      </#if>
      <#if !partyId?exists>
        <#assign partyId = event.partyIdFrom>
      </#if>
      <tr>
        <td>${event.communicationEventId?if_exists}</td>
        <td>${(eventType.get("description",locale))?default(uiLabelMap.CommonNA)}</td>
        <td>${(contactMechType.get("description",locale))?default(uiLabelMap.CommonNA)}</td>
        <td>${(statusItem.get("description",locale))?default(uiLabelMap.CommonNA)}</td>
        <td>${event.subject?if_exists}</td>
        <#if event.partyIdFrom?has_content>
          <td><a href="<@ofbizUrl>viewprofile?partyId=${event.partyIdFrom}</@ofbizUrl>" class="buttontext">${event.partyIdFrom}</a></td>
        <#else>
          <td>${uiLabelMap.CommonNA}</td>
        </#if>
        <#if event.partyIdTo?has_content>
          <td><a href="<@ofbizUrl>viewprofile?partyId=${event.partyIdTo}</@ofbizUrl>" class="buttontext">${event.partyIdTo}</a></td>
        <#else>
          <td>${uiLabelMap.CommonNA}</td>
        </#if>
        <td>${(event.entryDate?string)?if_exists}</td>
        <td align="right"><a href="<@ofbizUrl>viewCommunicationEvent?partyId=${event.partyIdFrom?if_exists}&communicationEventId=${event.communicationEventId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
      </tr>
    </#list>
  <#else>
    <tr>
      <td colspan="8">${uiLabelMap.PartyNoCommunicationFound}</td>
    </tr>
  </#if>
</table>