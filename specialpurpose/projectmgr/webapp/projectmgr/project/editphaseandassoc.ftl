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
      <#if project?has_content>
        <li class="head3">&nbsp;${uiLabelMap.PageTitleEditPhase}&nbsp;#${project.workEffortName?if_exists} ${uiLabelMap.CommonInformation}</li>
      <#else>
        <li class="head3">&nbsp;${uiLabelMap.PageTitleAddPhase}</li>
      </#if>
    </ul>
    <br class="clear" />
  </div> 
  <div class="screenlet-body">
    <#assign workEffortIdFrom = parameters.workEffortIdFrom>
    <#if phase?has_content>
      <form name="addPhaseAndAssocForm" method="get" action="<@ofbizUrl>updatePhaseAndAssoc</@ofbizUrl>">
    <#else>  
      <br/>
      <form name="addPhaseAndAssocForm" method="get" action="<@ofbizUrl>createPhaseAndAssoc</@ofbizUrl>">
    </#if>
        <table width="100%" cellpadding="2" cellspacing="0">
          <tr>
            <#if !(phase?exists)>
              <td><input type="hidden" name="workEffortTypeId" value="${parameters.workEffortTypeId?if_exists}"/></td>
            <#else>
              <td><input type="hidden" name="workEffortTypeId" value="${phase.workEffortTypeId?if_exists}"/></td>
              <td><input type="hidden" name="workEffortId" value="${phase.workEffortId?if_exists}"/></td>
              <td><input type="hidden" name="workEffortName" value="${phase.workEffortName?if_exists}"/></td> 
            </#if>
          </tr>    
          <tr>
            <td class="label" >${uiLabelMap.ProjectMgrWorkEffortIdFrom}</td>
            <#assign workEffort=delegator.findByPrimaryKey("WorkEffort", Static["org.ofbiz.base.util.UtilMisc"].toMap("workEffortId", workEffortIdFrom?if_exists ))>                     
            <td>${(workEffort.workEffortName)?if_exists} [${(workEffort.workEffortId)?if_exists}]<span class="tooltip">${uiLabelMap.CommonNotModifRecreat}</td>
            <td><input type="hidden" name="workEffortIdFrom" value="${workEffortIdFrom?if_exists}"/></td>
            <td><input type="hidden" name="workEffortParentId" value="${workEffortIdFrom?if_exists}"/></td> 
            <td><input type="hidden" name="workEffortAssocTypeId" value="WORK_EFF_BREAKDOWN"/>
          </tr>
          <tr>
            <td width="20%">
              ${uiLabelMap.ProjectMgrPhaseDetails}
            </td> 
          </tr>
          <tr>    
            <#if phase?exists>
              <td class="label" >${uiLabelMap.ProjectMgrWorkEffortId}</td>    
              <td>${phase.workEffortId?if_exists}</td>    
            </#if>
          </tr>    
          <tr>
            <td class="label" >${uiLabelMap.CommonName}*</td>
            <#if phase?exists>
              <td>${phase.workEffortName?if_exists}<span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            <#else>
              <td><input type="text" name="workEffortName" value=""/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            </#if>
          </tr>
          <tr>    
            <td class="label" >${uiLabelMap.CommonDescription}</td>
            <#if phase?exists>
              <td><input type="text" name="description" value="${phase.description?if_exists}"/></td>
            <#else>
              <td><input type="text" name="description" value=""/></td>
            </#if>
          </tr>   
          <tr>    
            <td class="label" >${uiLabelMap.CommonStatus}</td>
            <td>    
              <select name="currentStatusId" class="selectBox">
                <#if phase?exists>
                  <#assign currentStatusId = phase.currentStatusId?if_exists>
                  <#assign statusValidChangeToDetailList = delegator.findByAnd("StatusValidChangeToDetail", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", currentStatusId))>
                  <#list statusValidChangeToDetailList as statusValidChangeToDetail> 
                    <option SELECTED value="${currentStatusId}">${currentStatusId}</option>  
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
            <td class="label">${uiLabelMap.WorkEffortEstimatedStartDate}</td>
            <td>
              <#if phase?exists>
                <input type="text" name="estimatedStartDate" value="${phase.estimatedStartDate?if_exists}"/>
              <#else>
                <input type="text" name="estimatedStartDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addPhaseAndAssocForm.estimatedStartDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>    
          <tr>
            <td class="label">${uiLabelMap.WorkEffortEstimatedCompletionDate}</td>
            <td>
              <#if phase?exists>
                <input type="text" name="estimatedCompletionDate" value="${phase.estimatedCompletionDate?if_exists}"/>
              <#else>
                <input type="text" name="estimatedCompletionDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addPhaseAndAssocForm.estimatedCompletionDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.FormFieldTitle_actualStartDate}</td>
            <td>
              <#if phase?exists>
                <input type="text" name="actualStartDate" value="${phase.actualStartDate?if_exists}"/>
              <#else>
                <input type="text" name="actualStartDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addPhaseAndAssocForm.actualStartDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.FormFieldTitle_actualCompletionDate}</td>
            <td>
              <#if phase?exists>
                <input type="text" name="actualCompletionDate" value="${phase.actualCompletionDate?if_exists}"/>
              <#else>
                <input type="text" name="actualCompletionDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addPhaseAndAssocForm.actualCompletionDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
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
