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
        <li class="head3">&nbsp;${uiLabelMap.PageTitleEditSubTask}&nbsp;#${project.workEffortId?if_exists} ${uiLabelMap.CommonInformation}</li>
      <#else>
        <li class="head3">&nbsp;${uiLabelMap.PageTitleAddSubTask}</li>
      </#if>
    </ul>
    <br class="clear" />
  </div>  
  <div class="screenlet-body">
    <#if task?has_content>
      <form name="addTaskAndAssocForm" method="get" action="<@ofbizUrl>updateTaskListAndAssoc</@ofbizUrl>">
    <#else>
      <form name="addTaskAndAssocForm" method="get" action="<@ofbizUrl>createTaskListAndAssoc</@ofbizUrl>">
    </#if>
        <table width="100%" cellpadding="2" cellspacing="0">
          <tr>
            <#if !(task?exists)>
              <td><input type="hidden" name="workEffortTypeId" value="${parameters.workEffortTypeId?if_exists}"/></td>
            <#else>
              <td><input type="hidden" name="workEffortTypeId" value="${task.workEffortTypeId?if_exists}"/></td>
              <td><input type="hidden" name="workEffortId" value="${task.workEffortId?if_exists}"/></td>
              <td><input type="hidden" name="workEffortName" value="${task.workEffortName?if_exists}"/></td> 
            </#if>
          </tr>
          <tr>
            <td><input type="hidden" name="workEffortIdFrom" value="${workEffortIdFrom?if_exists}"/></td>
            <td><input type="hidden" name="workEffortAssocTypeId" value="WORK_EFF_BREAKDOWN"/></td>
            <td><input type="hidden" name="workEffortParentId" value="${workEffortIdFrom?if_exists}"/></td> 
          </tr>
          <tr>
            <td width="20%">
              Enter the WorkEffort Details below
            </td> 
          </tr>
          <tr>    
            <#if task?exists>
              <td class="label" >${uiLabelMap.ProjectMgrWorkEffortId}</td>    
              <td>${task.workEffortId?if_exists}</td>
            </#if>
          </tr>    
          <tr>
            <td class="label" >${uiLabelMap.CommonName}*</td>
            <#if task?exists>
              <td>${task.workEffortName?if_exists}</td>
            <#else>
              <td><input type="text" name="workEffortName" value=""/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            </#if>
          </tr>
          <tr>    
            <td class="label" >${uiLabelMap.CommonDescription}</td>
            <#if task?exists>
              <td><input type="text" name="description" value="${task.description?if_exists}"/></td>
            <#else>
              <td><input type="text" name="description" value=""/></td>
            </#if>
          </tr>   
          <tr>    
            <td class="label" >${uiLabelMap.CommonStatus}</td>
            <td>    
              <select name="currentStatusId" class="selectBox">
                <#if task?exists>
                  <#assign currentStatus = task.geRelatedOne("CurrentStatusItem")?if_exists>
                  <option SELECTED value="${currentStatus.currentStatusId}">${currentStatus.description}</option>
                  <#assign statusValidChangeToDetailList = delegator.findByAnd("StatusValidChangeToDetail", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", currentStatus.currentStatusId))>
                  <#list statusValidChangeToDetailList as statusValidChangeToDetail> 
                    <option value=${statusValidChangeToDetail.statusId}>[${uiLabelMap.WorkEffortGeneral}]${statusValidChangeToDetail.description}</option>
                  </#list>
                <#else>
                  <#assign statusItemGenrals = delegator.findByAnd("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusTypeId", "CALENDAR_STATUS"))>
                  <#assign statusItemTasks = delegator.findByAnd("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusTypeId", "TASK_STATUS"))>
                  <#assign statusItemEvents = delegator.findByAnd("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusTypeId", "EVENT_STATUS"))>
                  <#list statusItemGenrals as statusItem> 
                    <option value="${statusItem.statusId?if_exists}">[${uiLabelMap.WorkEffortGeneral}]${statusItem.description}</option>
                  </#list>                
                  <#list statusItemTasks as statusItem>
                    <option value="${statusItem.statusId?if_exists}">[${uiLabelMap.WorkEffortTask}]${statusItem.description}</option>
                  </#list>                
                  <#list statusItemEvents as statusItem>
                    <option value="${statusItem.statusId?if_exists}">[${uiLabelMap.WorkEffortEvent}]${statusItem.description}</option>
                  </#list>
                </#if>        
              </select>
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.CommonPriority}</td>
            <td>
              <#if task?has_content>
                <#assign priority = task.priority?if_exists>      
              </#if>
              <select name="priority" class="selectBox" size="1">
                <#if priority?exists>          
                  <option SELECTED value="${priority}">${priority}</option>       
                  <option></option>
                  <option value=1>1 (${uiLabelMap.WorkEffortPriorityHigh})</option>
                  <option value=2>2</option>
                  <option value=3>3</option>
                  <option value=4>4</option>
                  <option value=5>5</option>
                  <option value=6>6</option>
                  <option value=7>7</option>
                  <option value=8>8</option>
                  <option value=9>9 (${uiLabelMap.WorkEffortPriorityLow})</option>
                <#else>
                  <option></option>
                  <option value=1>1 (${uiLabelMap.WorkEffortPriorityHigh})</option>
                  <option value=2>2</option>
                  <option value=3>3</option>
                  <option value=4>4</option>
                  <option value=5>5</option>
                  <option value=6>6</option>
                  <option value=7>7</option>
                  <option value=8>8</option>
                  <option value=9>9 (${uiLabelMap.WorkEffortPriorityLow})</option>
                </#if>
              </select>
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.ProjectMgrWorkEffortScopeEnumId}</td>
            <td>             
              <#assign enumerations = delegator.findByAnd("Enumeration", Static["org.ofbiz.base.util.UtilMisc"].toMap("enumTypeId", "WORK_EFF_SCOPE"))>
              <select name="scopeEnumId" class="selectBox">
                <#if task?exists>
                  <#assign currentScope = task.getRelatedOne("ScopeEnumeration")?if_exists>
                  <option value="${currentScope.enumId}">${currentScope.description}</option>
                  <option>--</option>
                  <#list enumerations as enumeration>                    
                    <option value="${enumeration.enumId}">${enumeration.description}</option>
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
              <#if task?exists>
                <input type="text" name="estimatedStartDate" value="${task.estimatedStartDate?if_exists}"/>
              <#else>
                <input type="text" name="estimatedStartDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addTaskAndAssocForm.estimatedStartDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>    
          <tr>
            <td class="label">${uiLabelMap.WorkEffortEstimatedCompletionDate}</td>
            <td>
              <#if task?exists>
                <input type="text" name="estimatedCompletionDate" value="${task.estimatedCompletionDate?if_exists}"/>
              <#else>
                <input type="text" name="estimatedCompletionDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addTaskAndAssocForm.estimatedCompletionDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.FormFieldTitle_actualStartDate}</td>
            <td>
              <#if task?exists>
                <input type="text" name="actualStartDate" value="${task.actualStartDate?if_exists}"/>
              <#else>
                <input type="text" name="actualStartDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addTaskAndAssocForm.actualStartDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.FormFieldTitle_actualCompletionDate}</td>
            <td>
              <#if task?exists>
                <input type="text" name="actualCompletionDate" value="${task.actualCompletionDate?if_exists}"/>
              <#else>
                <input type="text" name="actualCompletionDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addTaskAndAssocForm.actualCompletionDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
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
