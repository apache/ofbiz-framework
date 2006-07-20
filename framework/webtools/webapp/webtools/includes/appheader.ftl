<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {(page.headerItem)?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {(page.headerItem)?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">${uiLabelMap.FrameworkWebTools}</div>
<div class="row">
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="headerButtonLeft">${uiLabelMap.CommonMain}</a></div>
  <#--
  <div class="col"><a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="headerButtonLeft">Cache</a></div>  
  <div class="col"><a href="<@ofbizUrl>entitymaint</@ofbizUrl>" class="headerButtonLeft">Entity</a></div>  
  <div class="col"><a href="<@ofbizUrl>availableServices</@ofbizUrl>" class="headerButtonLeft">Service</a></div>  
  <div class="col"><a href="<@ofbizUrl>workflowMonitor</@ofbizUrl>" class="headerButtonLeft">Workflow</a></div>  
  <div class="col"><a href="<@ofbizUrl>viewdatafile</@ofbizUrl>" class="headerButtonLeft">Data</a></div>  
  <div class="col"><a href="<@ofbizUrl>EditCustomTimePeriod</@ofbizUrl>" class="headerButtonLeft">Misc</a></div>  
  <div class="col"><a href="<@ofbizUrl>StatsSinceStart</@ofbizUrl>" class="headerButtonLeft">Statistics</a></div>  
  -->
  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="headerButtonRight">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='headerButtonRight'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>
