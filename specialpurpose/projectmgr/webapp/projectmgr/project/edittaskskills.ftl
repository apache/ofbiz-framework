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
      <li class="h3">&nbsp;${uiLabelMap.PageTitleAddSkill}</li>      
    </ul>
    <br class="clear" />
  </div>
  <div class="screenlet-body">  
<form name="editTaskSkillForm" action="<@ofbizUrl>createTaskSkillStandard</@ofbizUrl>">
  <table width="100%" cellpadding="2" cellspacing="0" border="1">
    <tr>
      <td><input type="hidden" name="workEffortId" value="${parameters.workEffortId?if_exists}"/></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.ProjectMgrSkillType}</td>
      <td>             
        <select name="skillTypeId">  
          <#assign skillTypes = delegator.findAll("SkillType")>
          <#list skillTypes as skillType>                    
            <option value="${skillType.skillTypeId}">${skillType.description}</option>
          </#list>    
        </select><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
      </td>
    </tr>
    <tr>    
      <td class="label" >${uiLabelMap.ProjectMgrEstimatedNumPeople}</td>
      <td><input type="text" name="estimatedNumPeople" value=""/></td>
    </tr> 
    <tr>    
      <td class="label" >${uiLabelMap.ProjectMgrEstimatedDuration}</td>
      <td><input type="text" name="estimatedDuration" value=""/></td>
    </tr> 
    <tr>    
      <td class="label" >${uiLabelMap.ProjectMgrEstimatedCost}</td>
      <td><input type="text" name="estimatedCost" value=""/></td>
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
