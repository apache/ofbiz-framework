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
        <li class="head3">&nbsp;${uiLabelMap.ProjectMgrProjectName}&nbsp;#${project.workEffortId?if_exists} ${uiLabelMap.CommonInformation}</li>
      <#else>
        <li class="head3">&nbsp;${uiLabelMap.ProjectMgrTaskName}&nbsp;#${task.workEffortId?if_exists} ${uiLabelMap.CommonInformation}</li>
      </#if>
    </ul>
    <br class="clear" />
  </div>  
  <div class="screenlet-body">    
    <#if project?has_content>
    <table width="100%" border="0" cellpadding="1" cellspacing="0">  
      <tr>        
        <td class="label" >${uiLabelMap.ProjectMgrWorkEffortId}</td>
        <td>${project.workEffortId?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>    
        <td class="label" >${uiLabelMap.CommonName}</td>    
        <td>${project.workEffortName?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>    
        <td class="label" >${uiLabelMap.CommonDescription}</td>
        <td>${project.description?if_exists}</td>
      </tr>   
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>    
        <td class="label" >${uiLabelMap.CommonStatus}</td>
        <td>
          <#assign currentStatus = project.getRelatedOne("CurrentStatusItem")?if_exists>
          ${currentStatus.description?if_exists}
        </td>
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.CommonPriority}</td>
        <td>${project.priority?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.ProjectMgrWorkEffortScopeEnumId}</td>
        <td>
          <#assign currentScope = project.getRelatedOne("ScopeEnumeration")?if_exists>
          ${currentScope.description?if_exists}
        </td>
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.WorkEffortEstimatedStartDate}</td>
        <td>${project.estimatedStartDate?if_exists}</td>        
      </tr>    
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.WorkEffortEstimatedCompletionDate}</td>
        <td>${project.estimatedCompletionDate?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.FormFieldTitle_actualStartDate}</td>
        <td>${project.actualStartDate?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.FormFieldTitle_actualCompletionDate}</td>
        <td>${project.actualCompletionDate?if_exists}</td>    
      </tr>
    </table>    
    <#else>
      <table width="100%" border="0" cellpadding="1" cellspacing="0">  
      <tr>        
        <td class="label" >${uiLabelMap.ProjectMgrWorkEffortId}</td>
        <td>${task.workEffortId?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>    
        <td class="label" >${uiLabelMap.CommonName}</td>    
        <td>${task.workEffortName?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>    
        <td class="label" >${uiLabelMap.CommonDescription}</td>
        <td>${task.description?if_exists}</td>
      </tr>   
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>    
        <td class="label" >${uiLabelMap.CommonStatus}</td>
        <td>
          <#assign currentStatus = task.getRelatedOne("CurrentStatusItem")?if_exists>
          ${currentStatus.description?if_exists}
        </td>
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.CommonPriority}</td>
        <td>${task.priority?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.ProjectMgrWorkEffortScopeEnumId}</td>
        <td>
          <#assign currentScope = task.getRelatedOne("ScopeEnumeration")?if_exists>
          ${currentScope.description?if_exists}
        </td>
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.WorkEffortEstimatedStartDate}</td>
        <td>${task.estimatedStartDate?if_exists}</td>        
      </tr>    
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.WorkEffortEstimatedCompletionDate}</td>
        <td>${task.estimatedCompletionDate?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.FormFieldTitle_actualStartDate}</td>
        <td>${task.actualStartDate?if_exists}</td>    
      </tr>
      <tr><td colspan="3"><hr class="sepbar"/></td></tr>
      <tr>
        <td class="label">${uiLabelMap.FormFieldTitle_actualCompletionDate}</td>
        <td>${task.actualCompletionDate?if_exists}</td>    
      </tr>
      </table>    
    </#if>    
  </div>
</div>  
