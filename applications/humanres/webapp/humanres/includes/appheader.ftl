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

<#assign selected = page.headerItem?default("void")>

<div id="app-navigation">
  <h2>${uiLabelMap.HumanResManagerApplication}</h2>
  <ul>
    <li<#if selected == "main"> class="selected"</#if>><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonMain}</a></li>
    <li<#if selected == "EmplPosition"> class="selected"</#if>><a href="<@ofbizUrl>findEmplPositions</@ofbizUrl>">${uiLabelMap.HumanResEmployeePosition}</a></li>
    <li<#if selected == "Employment"> class="selected"</#if>><a href="<@ofbizUrl>findEmployments</@ofbizUrl>">${uiLabelMap.HumanResEmployment}</a></li>    
    <li<#if selected == "PayGrade"> class="selected"</#if>><a href="<@ofbizUrl>findPayGrades</@ofbizUrl>">${uiLabelMap.HumanResPayGrade}</a></li>        
    <li<#if selected == "TerminationReason"> class="selected"</#if>><a href="<@ofbizUrl>findTerminationReasons</@ofbizUrl>">${uiLabelMap.HumanResTerminationReason}</a></li>        
    <li<#if selected == "UnemploymentClaim"> class="selected"</#if>><a href="<@ofbizUrl>findUnemploymentClaims</@ofbizUrl>">${uiLabelMap.HumanResUnemploymentClaim}</a></li>        
    <li<#if selected == "EmploymentApp"> class="selected"</#if>><a href="<@ofbizUrl>findEmploymentApps</@ofbizUrl>">${uiLabelMap.HumanResEmploymentApp}</a></li>
    <li<#if selected == "PartySkills"> class="selected"</#if>><a href="<@ofbizUrl>ListPartySkills</@ofbizUrl>">${uiLabelMap.HumanResListPartySkill}</a></li>
    <li<#if selected == "SkillType"> class="selected"</#if>><a href="<@ofbizUrl>findSkillTypes</@ofbizUrl>">${uiLabelMap.HumanResSkillType}</a></li>
    <#if userLogin?has_content>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear"/>
</div>
