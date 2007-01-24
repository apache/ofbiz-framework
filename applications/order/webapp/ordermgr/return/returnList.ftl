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

<div class="head1">Current Returns</div>
<div><a href="<@ofbizUrl>returnMain</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderReturnCreate}</a></div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.OrderReturnId} #</div></td>
    <td><div class="tableheadtext">${uiLabelMap.FormFieldTitle_entryDate}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.PartyParty}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.FacilityFacility}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
  </tr> 
  <tr><td colspan="5"><hr class="sepbar"></td></tr>
  <#list returnList as returnHeader>
  <#assign statusItem = returnHeader.getRelatedOne("StatusItem")>
  <#if returnHeader.destinationFacilityId?exists>
    <#assign facility = returnHeader.getRelatedOne("Facility")>
  </#if>
  <tr>
    <td><a href="<@ofbizUrl>returnMain?returnId=${returnHeader.returnId}</@ofbizUrl>" class="buttontext">${returnHeader.returnId}</a></td>
    <td><div class="tabletext">${returnHeader.entryDate.toString()}</div></td>
    <td>
      <#if returnHeader.fromPartyId?exists>
        <a href="${customerDetailLink}${returnHeader.fromPartyId}${externalKeyParam}" class='buttontext'>${returnHeader.fromPartyId}</a>
      <#else>
        <span class="tabletext">${uiLabelMap.CommonNA}</span>
      </#if>
    </td>
    <td><div class="tabletext"><#if facility?exists>${facility.facilityName?default(facility.facilityId)}<#else>${uiLabelMap.CommonNone}</#if></div></td>
    <td><div class="tabletext">${statusItem.get("description",locale)}</div></td>   
  </tr>
  </#list>
</table>
