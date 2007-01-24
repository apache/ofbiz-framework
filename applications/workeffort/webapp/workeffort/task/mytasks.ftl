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

<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left" width="40%" >
            <div class="boxhead">${uiLabelMap.WorkEffortMyCurrentTaskList}</div>
          </td>
          <td align="right" width="60%">
            <a href="<@ofbizUrl>EditWorkEffort?workEffortTypeId=TASK&amp;currentStatusId=CAL_NEEDS_ACTION</@ofbizUrl>" class="submenutextright">${uiLabelMap.WorkEffortNewTask}</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td>
              <div class="head3">${uiLabelMap.WorkEffortAssignedTasks}</div>
              <table width="100%" cellpadding="2" cellspacing="0" border="0">
                <tr>
                  <td><div class="tabletext"><b>${uiLabelMap.CommonStartDateTime}</b></div></td>
                  <td><div class="tabletext"><b>${uiLabelMap.WorkEffortPriority}</b></div></td>
                  <td><div class="tabletext"><b>${uiLabelMap.WorkEffortStatus}</b></div></td>
                  <td><div class="tabletext"><b>${uiLabelMap.WorkEffortTaskName}</b></div></td>
                  <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonEdit}</b></div></td>
                </tr>
                <tr><td colspan="5"><HR class="sepbar"></td></tr>
                <#list tasks as workEffort>
                  <tr>
                    <td><div class="tabletext">${(workEffort.estimatedStartDate.toString())?if_exists}</div></td>
                    <td><div class="tabletext">${workEffort.priority?if_exists}</div></td>
                    <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId"))).get("description",locale))?if_exists}</div></td>
                    <td><A class="linktext" href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></div></td>
                    <td align="right" width="1%"><A class="buttontext" href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortId}</a></div></td>
                  </tr>
                </#list>
              </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <#if (activities.size() > 0)>
      <tr>
        <td width="100%">
          <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
            <tr>
              <td>
                  <div class="head3">${uiLabelMap.WorkEffortWorkflowActivitiesUser}</div>
                  <table width="100%" cellpadding="2" cellspacing="0" border="0">
                    <tr>
                      <td><div class="tabletext"><b>${uiLabelMap.CommonStartDateTime}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortPriority}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortActivityStatus}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortMyStatus}</b></div></td>
                      <#-- <td><div class="tabletext"><b>${uiLabelMap.PartyPartyId}</b></div></td> -->
                      <td><div class="tabletext"><b>${uiLabelMap.PartyRoleId}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortActivityName}</b></div></td>
                      <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonEdit}</b></div></td>
                    </tr>
                    <tr><td colspan="8"><HR class="sepbar"></td></tr>
                    <#list activities as workEffort>
                      <tr>
                        <td><div class="tabletext">${(workEffort.estimatedStartDate.toString())?if_exists}</div></td>
                        <td><div class="tabletext">${workEffort.priority?if_exists}</div></td>
                        <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId"))).get("description",locale))?if_exists}</div></td>
                        <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId"))).get("description",locale))?if_exists}</div></td>
                        <#-- <td><div class="tabletext">${workEffort.partyId}</div></td> -->
                        <td><div class="tabletext">${workEffort.roleTypeId}</div></td>
                        <td><A class="linktext" href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></div></td>
                        <td align="right"><A class="buttontext" href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortId}</a></div></td>
                      </tr>
                    </#list>
                  </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>
  </#if>
  <#if (roleActivities.size() > 0)>
      <tr>
        <td width="100%">
          <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
            <tr>
              <td>
                  <div class="head3">${uiLabelMap.WorkEffortWorkflowActivitiesUserRole}</div>
                  <table width="100%" cellpadding="2" cellspacing="0" border="0">
                    <tr>
                      <td><div class="tabletext"><b>${uiLabelMap.CommonStartDateTime}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortPriority}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortActivityStatus}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortMyStatus}</b></div></td>
                      <#-- <td><div class="tabletext"><b>${uiLabelMap.PartyPartyId}</b></div></td> -->
                      <td><div class="tabletext"><b>${uiLabelMap.PartyRoleId}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortActivityName}</b></div></td>
                      <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonEdit}</b></div></td>
                    </tr>
                    <tr><td colspan="8"><HR class="sepbar"></td></tr>
                    <#list roleActivities as workEffort>
                      <tr>
                        <td><div class="tabletext">${(workEffort.estimatedStartDate.toString())?if_exists}</div></td>
                        <td><div class="tabletext">${workEffort.priority?if_exists}</div></td>
                        <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId"))).get("description",locale))?if_exists}</div></td>
                        <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId"))).get("description",locale))?if_exists}</div></td>
                        <#-- <td><div class="tabletext">${workEffort.partyId}</div></td> -->
                        <td><div class="tabletext">${workEffort.roleTypeId}</div></td>
                        <td><A class="buttontext" href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></div></td>
                        <td align="right"><A class="buttontext" href="<@ofbizUrl>acceptRoleAssignment?workEffortId=${workEffort.workEffortId}&partyId=${workEffort.partyId}&roleTypeId=${workEffort.roleTypeId}&fromDate=${workEffort.fromDate.toString()}</@ofbizUrl>">${uiLabelMap.WorkEffortAcceptAssignment}&nbsp;[${workEffort.workEffortId}]</a></div></td>
                      </tr>
                    </#list>
                  </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>
  </#if>
  <#if (groupActivities.size() > 0)>
      <tr>
        <td width="100%">
          <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
            <tr>
              <td>
                  <div class="head3">${uiLabelMap.WorkEffortWorkflowActivitiesUserGroup}</div>
                  <table width="100%" cellpadding="2" cellspacing="0" border="0">
                    <tr>
                      <td><div class="tabletext"><b>${uiLabelMap.CommonStartDateTime}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortPriority}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortActivityStatus}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortMyStatus}</b></div></td>
                      <td><div class="tabletext"><b>${uiLabelMap.PartyGroupPartyId}</b></div></td>
                      <#-- <td><div class="tabletext"><b>${uiLabelMap.PartyRoleId}</b></div></td> -->
                      <td><div class="tabletext"><b>${uiLabelMap.WorkEffortActivityName}</b></div></td>
                      <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonEdit}</b></div></td>
                    </tr>
                    <tr><td colspan="8"><HR class="sepbar"></td></tr>
                    <#list groupActivities as workEffort>
                      <tr>
                        <td><div class="tabletext">${(workEffort.estimatedStartDate.toString())?if_exists}</div></td>
                        <td><div class="tabletext">${workEffort.priority?if_exists}</div></td>
                        <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId"))).get("description",locale))?if_exists}</div></td>
                        <td><div class="tabletext">${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("statusId"))).get("description",locale))?if_exists}</div></td>
                        <td><div class="tabletext">${workEffort.groupPartyId}</div></td>
                        <#-- <td><div class="tabletext">${workEffort.roleTypeId}</div></td> -->
                        <td><A class="buttontext" href="<@ofbizUrl>WorkEffortSummary?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">
                            ${workEffort.workEffortName}</a></div></td>
                        <td align="right"><A class="buttontext" href="<@ofbizUrl>acceptassignment?workEffortId=${workEffort.workEffortId}&partyId=${workEffort.partyId}&roleTypeId=${workEffort.roleTypeId}&fromDate=${workEffort.fromDate}</@ofbizUrl>">
                            ${uiLabelMap.WorkEffortAcceptAssignment}&nbsp;[${workEffort.workEffortId}]</a></div></td>
                      </tr>
                    </#list>
                  </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>
  </#if>
</table>
