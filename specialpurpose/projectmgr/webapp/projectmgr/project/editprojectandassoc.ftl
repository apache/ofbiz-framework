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
        <li class="head3">&nbsp;${uiLabelMap.PageTitleEditSubProject}&nbsp;#${project.workEffortName?if_exists} ${uiLabelMap.CommonInformation}</li>
      <#else>
        <li class="head3">&nbsp;${uiLabelMap.PageTitleAddSubProject}</li>
      </#if>
    </ul>
    <br class="clear" />
  </div> 
  <div class="screenlet-body">
    <#assign workEffortIdFrom = parameters.workEffortIdFrom> 
    <#if project?has_content>
      <form name="addProjectAndAssocForm" method="get" action="<@ofbizUrl>updateProjectAndAssoc</@ofbizUrl>">
    <#else>
      <br/>  
      <form name="addProjectAndAssocForm" method="get" action="<@ofbizUrl>createProjectAndAssoc</@ofbizUrl>">
    </#if>
        <table width="100%" cellpadding="2" cellspacing="0">
          <tr>
            <#if !(project?exists)>
              <td><input type="hidden" name="workEffortTypeId" value="${parameters.workEffortTypeId?if_exists}"/></td>
            <#else>
              <td><input type="hidden" name="workEffortTypeId" value="${project.workEffortTypeId?if_exists}"/></td>
              <td><input type="hidden" name="workEffortId" value="${project.workEffortId?if_exists}"/></td>
              <td><input type="hidden" name="workEffortName" value="${project.workEffortName?if_exists}"/></td> 
            </#if>
          </tr>
          <tr>
            <td class="label" >${uiLabelMap.ProjectMgrWorkEffortIdFrom}</td>
            <#assign workEffort=delegator.findByPrimaryKey("WorkEffort", Static["org.ofbiz.base.util.UtilMisc"].toMap("workEffortId", workEffortIdFrom?if_exists ))>                     
            <td>${(workEffort.workEffortName)?if_exists} [${(workEffort.workEffortId)?if_exists}]<span class="tooltip">${uiLabelMap.CommonNotModifRecreat}</td>
            <td><input type="hidden" name="workEffortIdFrom" value="${workEffortIdFrom?if_exists}"/>
            <td><input type="hidden" name="workEffortParentId" value="${workEffortIdFrom?if_exists}"/></td>
            <td><input type="hidden" name="workEffortAssocTypeId" value="WORK_EFF_BREAKDOWN"/>
          </tr>
          <tr>
            <td width="20%">
              ${uiLabelMap.ProjectMgrSubProjectDetails}
            </td> 
          </tr>
          <tr>    
            <#if project?exists>
              <td class="label" >${uiLabelMap.ProjectMgrWorkEffortId}</td>    
              <td>${project.workEffortId?if_exists}</td>    
            </#if>
          </tr>
          <tr>    
            <td class="label" >${uiLabelMap.CommonName}*</td>
            <#if project?exists>
              <td>${project.workEffortName?if_exists} <span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            <#else>
              <td><input type="text" name="workEffortName" value=""/> <span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
            </#if>
          </tr>
          <tr>    
            <td class="label" >${uiLabelMap.CommonDescription}</td>
            <#if project?exists>
              <td><input type="text" name="description" value="${project.description?if_exists}"/></td>
            <#else>
              <td><input type="text" name="description" value=""/></td>
            </#if>
          </tr>   
          <tr>    
            <td class="label" >${uiLabelMap.CommonStatus}</td>
            <td>    
              <select name="currentStatusId" class="selectBox">
                <#if project?exists>
                  <#assign currentStatusId = project.currentStatusId?if_exists>
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
            <td class="label">${uiLabelMap.CommonPriority}</td>
            <td>
              <#if project?has_content>
                <#assign priority = project.priority?if_exists>      
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
                <#if project?exists>
                  <#assign scopeEnumId = project.scopeEnumId?if_exists>            
                  <#list enumerations as enumeration>                    
                    <option <#if "${enumeration.enumId}" == scopeEnumId?if_exists>selected="selected"</#if>>${enumeration.description}</option>
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
              <#if project?exists>
                 <input type="text" name="estimatedStartDate" value="${project.estimatedStartDate?if_exists}"/>
              <#else>
                <input type="text" name="estimatedStartDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addProjectAndAssocForm.estimatedStartDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr>    
          <tr>
            <td class="label">${uiLabelMap.WorkEffortEstimatedCompletionDate}</td>
            <td>
              <#if project?exists>
                <input type="text" name="estimatedCompletionDate" value="${project.estimatedCompletionDate?if_exists}"/>
              <#else>
                <input type="text" name="estimatedCompletionDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addProjectAndAssocForm.estimatedCompletionDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td> 
          </tr>
          <tr>
            <td class="label">${uiLabelMap.FormFieldTitle_actualStartDate}</td>
            <td>
              <#if project?exists>
                <input type="text" name="actualStartDate" value="${project.actualStartDate?if_exists}"/>
              <#else>
                <input type="text" name="actualStartDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addProjectAndAssocForm.actualStartDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
            </td>
          </tr> 
          <tr>
            <td class="label">${uiLabelMap.FormFieldTitle_actualCompletionDate}</td>
            <td>
              <#if project?exists>
                <input type="text" name="actualCompletionDate" value="${project.actualCompletionDate?if_exists}"/>
              <#else>
                <input type="text" name="actualCompletionDate" value=""/>
              </#if>
              <a href="javascript:call_cal(document.addProjectAndAssocForm.actualCompletionDate,'${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
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
