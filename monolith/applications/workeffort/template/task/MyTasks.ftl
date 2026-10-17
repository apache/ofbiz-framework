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
<h1>${uiLabelMap.PageTitleViewActivityAndTaskList}</h1>
<div class="button-bar">
  <a href="<@ofbizUrl>EditWorkEffort?workEffortTypeId=TASK&amp;currentStatusId=CAL_NEEDS_ACTION</@ofbizUrl>" class="buttontext create">${uiLabelMap.CommonCreate}</a>
</div>
  <h2>${uiLabelMap.WorkEffortAssignedTasks}</h2>
  <br />
  <table class="basic-table hover-bar" cellspacing="0">
    <tr class="header-row-2">
      <td>${uiLabelMap.CommonStartDateTime}</td>
      <td>${uiLabelMap.WorkEffortTaskName}</td>
      <td>${uiLabelMap.WorkEffortPriority}</td>
      <td>${uiLabelMap.WorkEffortStatus}</td>
    </tr>
    <#assign alt_row = false>
    <#list tasks as workEffort>
      <tr<#if alt_row> class="alternate-row"</#if>>
        <td>${(workEffort.estimatedStartDate)!}</td>
        <td><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></td>
        <td>${workEffort.priority!}</td>
        <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
      </tr>
      <#assign alt_row = !alt_row>
    </#list>
  </table>
  <#if (activities.size() > 0)>
    <h2>${uiLabelMap.WorkEffortWorkflowActivitiesUser}</h2>
    <br />
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row-2">
        <td>${uiLabelMap.CommonStartDateTime}</td>
        <td>${uiLabelMap.WorkEffortPriority}</td>
        <td>${uiLabelMap.WorkEffortActivityStatus}</td>
        <td>${uiLabelMap.WorkEffortMyStatus}</td>
        <td>${uiLabelMap.PartyRole}</td>
        <td>${uiLabelMap.WorkEffortActivityName}</td>
        <td>${uiLabelMap.CommonEdit}</td>
      </tr>
      <#assign alt_row = false>
      <#list activities as workEffort>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${(workEffort.estimatedStartDate)!}</td>
          <td>${workEffort.priority!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId")), true).get("description",locale))!}</td>
          <td>${workEffort.roleTypeId}</td>
          <td><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></td>
          <td class="button-col"><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortId}</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </#if>
  <#if (roleActivities.size() > 0)>
    <h2>${uiLabelMap.WorkEffortWorkflowActivitiesUserRole}</h2>
    <br />
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row-2">
        <td>${uiLabelMap.CommonStartDateTime}</td>
        <td>${uiLabelMap.WorkEffortPriority}</td>
        <td>${uiLabelMap.WorkEffortActivityStatus}</td>
        <td>${uiLabelMap.WorkEffortMyStatus}</td>
        <td>${uiLabelMap.PartyRole}</td>
        <td>${uiLabelMap.WorkEffortActivityName}</td>
        <td>${uiLabelMap.CommonEdit}</td>
      </tr>
      <#assign alt_row = false>
      <#list roleActivities as workEffort>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${(workEffort.estimatedStartDate)!}</td>
          <td>${workEffort.priority!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId")), true).get("description",locale))!}</td>
          <td>${workEffort.roleTypeId}</td>
          <td><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></td>
          <td class="button-col"><a href="<@ofbizUrl>acceptRoleAssignment?workEffortId=${workEffort.workEffortId}&amp;partyId=${workEffort.partyId}&amp;roleTypeId=${workEffort.roleTypeId}&amp;fromDate=${workEffort.fromDate.toString()}</@ofbizUrl>">${uiLabelMap.WorkEffortAcceptAssignment}&nbsp;[${workEffort.workEffortId}]</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </#if>
  <#if (groupActivities.size() > 0)>
    <h2>${uiLabelMap.WorkEffortWorkflowActivitiesUserGroup}</h2>
    <br />
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row-2">
        <td>${uiLabelMap.CommonStartDateTime}</td>
        <td>${uiLabelMap.WorkEffortPriority}</td>
        <td>${uiLabelMap.WorkEffortActivityStatus}</td>
        <td>${uiLabelMap.WorkEffortMyStatus}</td>
        <td>${uiLabelMap.PartyGroupPartyId}</td>
        <td>${uiLabelMap.WorkEffortActivityName}</td>
        <td>${uiLabelMap.CommonEdit}</td>
      </tr>
      <#assign alt_row = false>
      <#list groupActivities as workEffort>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${(workEffort.estimatedStartDate)!}</td>
          <td>${workEffort.priority!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId")), true).get("description",locale))!}</td>
          <td>${workEffort.groupPartyId}</td>
          <td><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></td>
          <td class="button-col"><a href="<@ofbizUrl>acceptassignment?workEffortId=${workEffort.workEffortId}&amp;partyId=${workEffort.partyId}&amp;roleTypeId=${workEffort.roleTypeId}&amp;fromDate=${workEffort.fromDate}</@ofbizUrl>">${uiLabelMap.WorkEffortAcceptAssignment}&nbsp;[${workEffort.workEffortId}]</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </#if>
