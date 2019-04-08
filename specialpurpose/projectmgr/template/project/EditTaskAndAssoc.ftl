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

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <#if task?has_content>
        <li class="h3">&nbsp;${uiLabelMap.PageTitleEditTask}&nbsp;#${project.workEffortId!} ${uiLabelMap.CommonInformation}</li>
      <#else>
        <li class="h3">&nbsp;${uiLabelMap.PageTitleAddTask}</li>
      </#if>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#assign workEffortIdFrom = parameters.workEffortIdFrom>
    <#if task?has_content>
      <form name="addTaskAndAssocForm" method="get" action="<@ofbizUrl>updateTaskAndAssoc</@ofbizUrl>">
    <#else>
      <br />
      <form name="addTaskAndAssocForm" method="get" action="<@ofbizUrl>createTaskAndAssoc</@ofbizUrl>">
    </#if>
        <table width="100%" cellpadding="2" cellspacing="0">
        <tr>
          <#if !(task??)>
            <td><input type="hidden" name="workEffortTypeId" value="${parameters.workEffortTypeId!}"/></td>
          <#else>
            <td><input type="hidden" name="workEffortTypeId" value="${task.workEffortTypeId!}"/></td>
            <td><input type="hidden" name="workEffortId" value="${task.workEffortId!}"/></td>
            <td><input type="hidden" name="workEffortName" value="${task.workEffortName!}"/></td>
          </#if>
        </tr>
        <tr>
            <td><input type="hidden" name="workEffortIdFrom" value="${workEffortIdFrom!}"/></td>
            <td><input type="hidden" name="workEffortParentId" value="${workEffortIdFrom!}"/></td>
            <td><input type="hidden" name="workEffortAssocTypeId" value="WORK_EFF_BREAKDOWN"/>
        </tr>
        <tr>
          <td width="20%">
            ${uiLabelMap.ProjectMgrTaskDetails}
          </td>
        </tr>
        <tr>
          <td class="label" >${uiLabelMap.ProjectMgrQuickAssignPartyId}</td>
          <td>
            <@htmlTemplate.lookupField formName="addTaskAndAssocForm" name="quickAssignPartyId" id="quickAssignPartyId" fieldFormName="LookupPartyName"/>
          </td>
        </tr>
        <tr>
          <#if task??>
            <td class="label" >${uiLabelMap.ProjectMgrWorkEffortId}</td>
            <td>${task.workEffortId!}</td>
          </#if>
        </tr>
        <tr>
          <td class="label" >${uiLabelMap.CommonName}*</td>
            <#if task??>
              <td>${task.workEffortName!}<span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            <#else>
              <td><input type="text" name="workEffortName" value=""/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            </#if>
        </tr>
        <tr>
          <td class="label" >${uiLabelMap.CommonDescription}</td>
            <#if task??>
              <td><input type="text" name="description" value="${task.description!}"/></td>
            <#else>
              <td><input type="text" name="description" value=""/></td>
          </#if>
        </tr>
        <tr>
          <td class="label" >${uiLabelMap.CommonStatus}</td>
          <td>
            <select name="currentStatusId">
              <#if task??>
                <#assign currentStatus = task.geRelatedOne("CurrentStatusItem")!>
                <option selected="selected" value="${currentStatus.currentStatusId}">${currentStatus.description}</option>
                <#assign statusValidChangeToDetailList = delegator.findByAnd("StatusValidChangeToDetail", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", currentStatus.currentStatusId), null, false)>
                <#list statusValidChangeToDetailList as statusValidChangeToDetail>
                  <option value=${statusValidChangeToDetail.statusId}>[${uiLabelMap.WorkEffortGeneral}]${statusValidChangeToDetail.description}</option>
                </#list>
              <#else>
                <#assign statusItemGenrals = delegator.findByAnd("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusTypeId", "CALENDAR_STATUS"), null, false)>
                <#assign statusItemTasks = delegator.findByAnd("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusTypeId", "TASK_STATUS"), null, false)>
                <#assign statusItemEvents = delegator.findByAnd("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusTypeId", "EVENT_STATUS"), null, false)>
                <#list statusItemGenrals as statusItem>
                  <option value="${statusItem.statusId!}">[${uiLabelMap.WorkEffortGeneral}]${statusItem.description}</option>
                </#list>
                <#list statusItemTasks as statusItem>
                  <option value="${statusItem.statusId!}">[${uiLabelMap.WorkEffortTask}]${statusItem.description}</option>
                </#list>
                <#list statusItemEvents as statusItem>
                  <option value="${statusItem.statusId!}">[${uiLabelMap.WorkEffortEvent}]${statusItem.description}</option>
                </#list>
              </#if>
            </select>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonPriority}</td>
          <td>
            <#if task?has_content>
              <#assign priority = task.priority!>
            </#if>
            <select name="priority" size="1">
              <#if priority??>
                <option selected="selected" value="${priority}">${priority}</option>
                <option></option>
                <option value=1>${uiLabelMap.WorkEffortPriorityOne}</option>
                <option value=2>${uiLabelMap.WorkEffortPriorityTwo}</option>
                <option value=3>${uiLabelMap.WorkEffortPriorityThree}</option>
                <option value=4>${uiLabelMap.WorkEffortPriorityFour}</option>
                <option value=5>${uiLabelMap.WorkEffortPriorityFive}</option>
                <option value=6>${uiLabelMap.WorkEffortPrioritySix}</option>
                <option value=7>${uiLabelMap.WorkEffortPrioritySeventh}</option>
                <option value=8>${uiLabelMap.WorkEffortPriorityEight}</option>
                <option value=9>${uiLabelMap.WorkEffortPriorityNine}</option>
              <#else>
                <option></option>
                <option value=1>${uiLabelMap.WorkEffortPriorityOne}</option>
                <option value=2>${uiLabelMap.WorkEffortPriorityTwo}</option>
                <option value=3>${uiLabelMap.WorkEffortPriorityThree}</option>
                <option value=4>${uiLabelMap.WorkEffortPriorityFour}</option>
                <option value=5>${uiLabelMap.WorkEffortPriorityFive}</option>
                <option value=6>${uiLabelMap.WorkEffortPrioritySix}</option>
                <option value=7>${uiLabelMap.WorkEffortPrioritySeventh}</option>
                <option value=8>${uiLabelMap.WorkEffortPriorityEight}</option>
                <option value=9>${uiLabelMap.WorkEffortPriorityNine}</option>
              </#if>
            </select>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.ProjectMgrWorkEffortScopeEnumId}</td>
          <td>
            <#assign enumerations = delegator.findByAnd("Enumeration", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("enumTypeId", "WORK_EFF_SCOPE"), null, false)>
            <select name="scopeEnumId">
              <#if task??>
                <#assign scopeEnumId = task.scopeEnumId!>
                <#list enumerations as enumeration>
                  <option <#if "${enumeration.enumId}" == scopeEnumId!>selected="selected"</#if>>${enumeration.description}</option>
                </#list>
              <#else>
                <#list enumerations as enumeration>
                  <option value="${enumeration.enumId}">${enumeration.description}</option>
                </#list>
              </#if>
            </select>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.WorkEffortEstimatedStartDate}</td>
          <td>
            <#if task??>
              <@htmlTemplate.renderDateTimeField name="estimatedStartDate" className="" event="" action="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${task.estimatedStartDate!}" size="25" maxlength="30" id="estimatedStartDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
            <#else>
              <@htmlTemplate.renderDateTimeField name="estimatedStartDate" className="" event="" action="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="estimatedStartDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
            </#if>
          </td>
         </tr>
         <tr>
           <td class="label">${uiLabelMap.WorkEffortEstimatedCompletionDate}</td>
           <td>
             <#if task??>
               <@htmlTemplate.renderDateTimeField name="estimatedCompletionDate" className="" event="" action="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${task.estimatedCompletionDate!}" size="25" maxlength="30" id="estimatedCompletionDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
             <#else>
               <@htmlTemplate.renderDateTimeField name="estimatedCompletionDate" className="" event="" action="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="estimatedCompletionDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
             </#if>
           </td>
         </tr>
         <tr>
           <td class="label">${uiLabelMap.FormFieldTitle_actualStartDate}</td>
           <td>


             <#if task??>
               <@htmlTemplate.renderDateTimeField name="actualStartDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${task.actualStartDate!}" size="25" maxlength="30" id="actualStartDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
             <#else>
               <@htmlTemplate.renderDateTimeField name="actualStartDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="actualStartDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
             </#if>
           </td>
         </tr>
         <tr>
           <td class="label">${uiLabelMap.FormFieldTitle_actualCompletionDate}</td>
           <td>

             <#if task??>
               <@htmlTemplate.renderDateTimeField name="actualCompletionDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${task.actualCompletionDate!}" size="25" maxlength="30" id="actualCompletionDate2" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
             <#else>
               <@htmlTemplate.renderDateTimeField name="actualCompletionDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="actualCompletionDate2" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
             </#if>
           </td>
         </tr>
         <tr>
           <td>&nbsp;</td>
           <td>
             <input type="submit" name="submit" value="${uiLabelMap.CommonSave}"/>
           </td>
         </tr>
       </table>
     </form>
   </div>
</div>
