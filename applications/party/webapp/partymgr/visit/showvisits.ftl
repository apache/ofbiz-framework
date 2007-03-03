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
    <#assign title = uiLabelMap.PartyParty>
  <#else>
    <#assign title = uiLabelMap.PartyActive>
  </#if>
  <h1>${title}&nbsp;${uiLabelMap.PartyVisitListing}</h1>
  <#if !partyId?exists && showAll?lower_case == "true">
    <a href="<@ofbizUrl>showvisits?showAll=false</@ofbizUrl>" class="smallSubmit">[${uiLabelMap.PartyShowActive}]</a>
  <#elseif !partyId?exists>
    <a href="<@ofbizUrl>showvisits?showAll=true</@ofbizUrl>" class="smallSubmit">[${uiLabelMap.PartyShowAll}]</a>
  </#if>
  <br/>
  <#if visitList?has_content>
    <div class="align-float">
      <b>
        <#if 0 < viewIndex>
          <a href="<@ofbizUrl>showvisits?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}<#if sort?has_content>&sort=${sort}</#if><#if partyId?has_content>&partyId=${partyId}</#if>&showAll=${showAll}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize>
          ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${visitSize}
        </#if>
        <#if highIndex < listSize>
          | <a href="<@ofbizUrl>showvisits?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}<#if sort?has_content>&sort=${sort}</#if><#if partyId?has_content>&partyId=${partyId}</#if>&showAll=${showAll}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonNext}</a>              
        </#if>
      </b>
    </div>
    <br class="clear" />
  </#if>
  <br/>
  
  <table class="basic-table" cellspacing="0">
    <tr class="header-row">
      <td><a href="<@ofbizUrl>showvisits?sort=visitId&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.PartyVisitId}</a></td>      
      <td><a href="<@ofbizUrl>showvisits?sort=visitorId&showAll=${showAll}<#if visitorId?has_content>&visitorId=${visitorId}</#if></@ofbizUrl>">${uiLabelMap.PartyVisitorId}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=partyId&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.PartyPartyId}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=userLoginId&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.PartyUserLoginId}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=-userCreated&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.PartyNewUser}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=webappName&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.PartyWebApp}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=clientIpAddress&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.PartyClientIP}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=fromDate&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.CommonFromDate}</a></td>
      <td><a href="<@ofbizUrl>showvisits?sort=thruDate&showAll=${showAll}<#if partyId?has_content>&partyId=${partyId}</#if></@ofbizUrl>">${uiLabelMap.CommonThruDate}</a></td>
    </tr>
    <#-- set initial row color -->
    <#assign rowClass = "2">
    <#list visitList as visitObj>
      <tr<#if rowClass == "1"> class="alternate-row"</#if>>
        <td class="button-col"><a href="<@ofbizUrl>visitdetail?visitId=${visitObj.visitId}</@ofbizUrl>">${visitObj.visitId}</a></td>       
        <td>${visitObj.visitorId?if_exists}</td>
        <td class="button-col"><a href="<@ofbizUrl>viewprofile?partyId=${visitObj.partyId?if_exists}</@ofbizUrl>">${visitObj.partyId?if_exists}</a></td>
        <td>${visitObj.userLoginId?if_exists}</td>
        <td>${visitObj.userCreated?if_exists}</td>
        <td>${visitObj.webappName?if_exists}</td>
        <td>${visitObj.clientIpAddress?if_exists}</td>
        <td>${(visitObj.fromDate?string)?if_exists}</td>
        <td>${(visitObj.thruDate?string)?if_exists}</td>
      </tr>
      <#-- toggle the row color -->
      <#if rowClass == "2">
        <#assign rowClass = "1">
      <#else>
        <#assign rowClass = "2">
      </#if>
    </#list>
  </table>

  <#if visitList?has_content>
    <br />
    <div class="align-float">
      <b>
        <#if 0 < viewIndex>
          <a href="<@ofbizUrl>showvisits?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}<#if sort?has_content>&sort=${sort}</#if><#if partyId?has_content>&partyId=${partyId}</#if>&showAll=${showAll}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize>
          ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${visitSize}
        </#if>
        <#if highIndex < listSize>
          | <a href="<@ofbizUrl>showvisits?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}<#if sort?has_content>&sort=${sort}</#if><#if partyId?has_content>&partyId=${partyId}</#if>&showAll=${showAll}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonNext}</a>              
        </#if>
      </b>
    </div>
    <br class="clear" />
  </#if>  
