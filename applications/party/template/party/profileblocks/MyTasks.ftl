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

<#setting locale = locale.toString()>
<#setting time_zone = timeZone.getID()>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.WorkEffortMyCurrentTaskList}</li>
      <li><a href="<@ofbizContentUrl>/workeffort/control/EditWorkEffort?workEffortTypeId=TASK&amp;currentStatusId=CAL_NEEDS_ACTION</@ofbizContentUrl>">${uiLabelMap.WorkEffortNewTask}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <h3>${uiLabelMap.WorkEffortAssignedTasks}</h3>
  <table class="basic-table hover-bar" cellspacing="0">
    <tr class="header-row">
      <td>${uiLabelMap.CommonStartDateTime}</td>
      <td>${uiLabelMap.WorkEffortPriority}</td>
      <td>${uiLabelMap.WorkEffortStatus}</td>
      <td>${uiLabelMap.WorkEffortTaskName}</td>
      <td>${uiLabelMap.CommonEdit}</td>
    </tr>
    <#assign alt_row = false>
    <#list tasks as workEffort>
      <tr<#if alt_row> class="alternate-row"</#if>>
        <td>${(workEffort.estimatedStartDate.toString())!}</td>
        <td>${workEffort.priority!}</td>
        <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
        <td><a href="<@ofbizContentUrl>/workeffort/control/WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizContentUrl>">${workEffort.workEffortName}</a></td>
        <td class="button-col"><a href="<@ofbizContentUrl>/workeffort/control/WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizContentUrl>">${workEffort.workEffortId}</a></td>
      </tr>
      <#assign alt_row = !alt_row>
    </#list>
  </table>
  <#if (activities.size() > 0)>
    <h3>${uiLabelMap.WorkEffortWorkflowActivitiesUser}</h3>
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.CommonStartDateTime}</td>
        <td>${uiLabelMap.WorkEffortPriority}</td>
        <td>${uiLabelMap.WorkEffortActivityStatus}</td>
        <td>${uiLabelMap.WorkEffortMyStatus}</td>
        <#-- <td>${uiLabelMap.PartyPartyId}</td> -->
        <td>${uiLabelMap.PartyRole}</td>
        <td>${uiLabelMap.WorkEffortActivityName}</td>
        <td>${uiLabelMap.CommonEdit}</td>
      </tr>
      <#assign alt_row = false>
      <#list activities as workEffort>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${(workEffort.estimatedStartDate.toString())!}</td>
          <td>${workEffort.priority!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId")), true).get("description",locale))!}</td>
          <#-- <td>${workEffort.partyId}</td> -->
          <td>${workEffort.roleTypeId}</td>
          <td><a href="<@ofbizContentUrl>/workeffort/control/WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizContentUrl>">${workEffort.workEffortName}</a></td>
          <td class="button-col"><a href="<@ofbizContentUrl>/workeffort/control/WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizContentUrl>">${workEffort.workEffortId}</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </#if>
  <#if (roleActivities.size() > 0)>
    <h3>${uiLabelMap.WorkEffortWorkflowActivitiesUserRole}</h3>
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.CommonStartDateTime}</td>
        <td>${uiLabelMap.WorkEffortPriority}</td>
        <td>${uiLabelMap.WorkEffortActivityStatus}</td>
        <td>${uiLabelMap.WorkEffortMyStatus}</td>
        <#-- <td>${uiLabelMap.PartyPartyId}</td> -->
        <td>${uiLabelMap.PartyRole}</td>
        <td>${uiLabelMap.WorkEffortActivityName}</td>
        <td>${uiLabelMap.CommonEdit}</td>
      </tr>
      <#assign alt_row = false>
      <#list roleActivities as workEffort>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${(workEffort.estimatedStartDate.toString())!}</td>
          <td>${workEffort.priority!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId")), true).get("description",locale))!}</td>
          <#-- <td>${workEffort.partyId}</td> -->
          <td>${workEffort.roleTypeId}</td>
          <td><a href="<@ofbizContentUrl>/workeffort/control/WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizContentUrl>">${workEffort.workEffortName}</a></td>
          <td class="button-col"><a href="<@ofbizContentUrl>/workeffort/control/acceptRoleAssignment?workEffortId=${workEffort.workEffortId}&amp;partyId=${workEffort.partyId}&amp;roleTypeId=${workEffort.roleTypeId}&amp;fromDate=${workEffort.fromDate.toString()}</@ofbizContentUrl>">${uiLabelMap.WorkEffortAcceptAssignment}&nbsp;[${workEffort.workEffortId}]</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </#if>
  <#if (groupActivities.size() > 0)>
    <h3>${uiLabelMap.WorkEffortWorkflowActivitiesUserGroup}</h3>
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.CommonStartDateTime}</td>
        <td>${uiLabelMap.WorkEffortPriority}</td>
        <td>${uiLabelMap.WorkEffortActivityStatus}</td>
        <td>${uiLabelMap.WorkEffortMyStatus}</td>
        <td>${uiLabelMap.PartyGroupPartyId}</td>
        <#-- <td>${uiLabelMap.PartyRole}</td> -->
        <td>${uiLabelMap.WorkEffortActivityName}</td>
        <td>${uiLabelMap.CommonEdit}</td>
      </tr>
      <#assign alt_row = false>
      <#list groupActivities as workEffort>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${(workEffort.estimatedStartDate.toString())!}</td>
          <td>${workEffort.priority!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}</td>
          <td>${(delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId")), true).get("description",locale))!}</td>
          <td>${workEffort.groupPartyId}</td>
          <#-- <td>${workEffort.roleTypeId}</td> -->
          <td><a href="<@ofbizContentUrl>/workeffort/control/WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizContentUrl>">${workEffort.workEffortName}</a></td>
          <td class="button-col"><a href="<@ofbizContentUrl>/workeffort/control/acceptassignment?workEffortId=${workEffort.workEffortId}&amp;partyId=${workEffort.partyId}&amp;roleTypeId=${workEffort.roleTypeId}&amp;fromDate=${workEffort.fromDate}</@ofbizContentUrl>">${uiLabelMap.WorkEffortAcceptAssignment}&nbsp;[${workEffort.workEffortId}]</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </#if>
</div>
