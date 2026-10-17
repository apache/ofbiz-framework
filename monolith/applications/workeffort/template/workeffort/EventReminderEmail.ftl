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
<#assign docLangAttr = locale.toString()?replace("_", "-")>
<#assign langDir = "ltr">
<#if "ar.iw"?contains(docLangAttr?substring(0, 2))>
    <#assign langDir = "rtl">
</#if>
<html lang="${docLangAttr}" dir="${langDir}" xmlns="http://www.w3.org/1999/xhtml">
  <head/>
  <body>
    <style type="text/css">
    .label {
      font-weight: bold;
    <#if "ltr" == langDir>
      padding-right: 10px;
      text-align: right;
    <#else>
      padding-left: 10px;
      text-align: left;
    </#if>
    }
    div {
      padding: 10px 0 10px 0;
    }
    </style>
    <table cellspacing=0>
      <#-- Work Effort Info -->
      <tr><td class="label">${uiLabelMap.CommonDate}</td><td>${parameters.eventDateTime?default("&nbsp;")}</td></tr>
      <tr><td class="label">${uiLabelMap.CommonName}</td><td>${workEffort.workEffortName?default("&nbsp;")}</td></tr>
      <tr><td class="label">${uiLabelMap.CommonDescription}</td><td>${workEffort.description?default("&nbsp;")}</td></tr>
      <tr><td class="label">${uiLabelMap.CommonType}</td><td>${(workEffortType.description)?default("&nbsp;")}</td></tr>
      <tr><td class="label">${uiLabelMap.CommonPurpose}</td><td>${(workEffortPurposeType.description)?default("&nbsp;")}</td></tr>
      <tr><td class="label">${uiLabelMap.CommonStatus}</td><td>${(currentStatusItem.description)?default("&nbsp;")}</td></tr>
      <tr><td colspan="2"><hr /></td>
    </table>
    <#if partyAssignments?has_content>
      <div><b>${uiLabelMap.PageTitleListWorkEffortPartyAssigns}</b></div>
      <table cellspacing=0 cellpadding=2 border=1>
        <thead><tr>
          <th>${uiLabelMap.PartyParty}</th>
          <th>${uiLabelMap.PartyRole}</th>
          <th>${uiLabelMap.CommonFromDate}</th>
          <th>${uiLabelMap.CommonThruDate}</th>
          <th>${uiLabelMap.CommonStatus}</th>
          <th>${uiLabelMap.WorkEffortDelegateReason}</th>
        </tr></thead>
        <tbody>
          <#list partyAssignments as wepa>
            <tr>
              <td>${wepa.groupName!}${wepa.firstName!} ${wepa.lastName!}</td>
              <td>${(wepa.getRelatedOne("RoleType", false).description)?default("&nbsp;")}</td>
              <td>${wepa.fromDate?default("&nbsp;")}</td>
              <td>${wepa.thruDate?default("&nbsp;")}</td>
              <td>${(wepa.getRelatedOne("AssignmentStatusItem", false).description)?default("&nbsp;")}</td>
              <td>${(wepa.getRelatedOne("DelegateReasonEnumeration", false).description)?default("&nbsp;")}</td>
            </tr>
          </#list>
        </tbody>
      </table>
    </#if>
    <#if fixedAssetAssignments?has_content>
      <div><b>${uiLabelMap.PageTitleListWorkEffortFixedAssetAssigns}</b></div>
      <table cellspacing=0 cellpadding=2 border=1>
        <thead><tr>
          <th>${uiLabelMap.AccountingFixedAsset}</th>
          <th>${uiLabelMap.CommonFromDate}</th>
          <th>${uiLabelMap.CommonThruDate}</th>
          <th>${uiLabelMap.CommonStatus}</th>
          <th>${uiLabelMap.FormFieldTitle_availabilityStatusId}</th>
          <th>${uiLabelMap.FormFieldTitle_allocatedCost}</th>
          <th>${uiLabelMap.CommonComments}</th>
        </tr></thead>
        <tbody>
          <#list fixedAssetAssignments as wefa>
            <tr>
              <td>${wefa.fixedAssetName?default("&nbsp;")}</td>
              <td>${wefa.fromDate?default("&nbsp;")}</td>
              <td>${wefa.thruDate?default("&nbsp;")}</td>
              <td>${(wefa.getRelatedOne("StatusItem", false).description)?default("&nbsp;")}</td>
              <td>${(wefa.getRelatedOne("AvailabilityStatusItem", false).description)?default("&nbsp;")}</td>
              <td>${wefa.allocatedCost?default("&nbsp;")}</td>
              <td>${wefa.comments?default("&nbsp;")}</td>
            </tr>
          </#list>
        </tbody>
      </table>
    </#if>
  </body>
</html>
