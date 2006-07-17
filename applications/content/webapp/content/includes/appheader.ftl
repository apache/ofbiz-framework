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
<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {page.headerItem?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {page.headerItem?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">&nbsp;${uiLabelMap.ContentContentManagerApplication}&nbsp;</div>
<div class="row">
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.ContentMain}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindWebSite</@ofbizUrl>" class="${selectedLeftClassMap.WebSite?default(unselectedLeftClassName)}">${uiLabelMap.ContentWebSites}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindSurvey</@ofbizUrl>" class="${selectedLeftClassMap.Survey?default(unselectedLeftClassName)}">${uiLabelMap.ContentSurvey}</a></div>
  <div class="col"><a href="<@ofbizUrl>ContentMenu</@ofbizUrl>" class="${selectedLeftClassMap.Content?default(unselectedLeftClassName)}">${uiLabelMap.ContentContent}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindDataResource</@ofbizUrl>" class="${selectedLeftClassMap.DataResource?default(unselectedLeftClassName)}">${uiLabelMap.ContentDataResource}</a></div>

  <div class="col"><a href="<@ofbizUrl>ContentSetupMenu</@ofbizUrl>" class="${selectedLeftClassMap.ContentSetupMenu?default(unselectedLeftClassName)}">${uiLabelMap.ContentContentSetup}</a></div>
  <div class="col"><a href="<@ofbizUrl>DataSetupMenu</@ofbizUrl>" class="${selectedLeftClassMap.DataResourceSetupMenu?default(unselectedLeftClassName)}">${uiLabelMap.ContentDataSetup}</a></div>

  <div class="col"><a href="<@ofbizUrl>LayoutMenu</@ofbizUrl>" class="${selectedLeftClassMap.Layout?default(unselectedLeftClassName)}">${uiLabelMap.ContentTemplate}</a></div>
  <#assign cmsTarget="CMSContentFind"/>
  <#if menuContext?has_content && menuContext.cmsRequestName?has_content>
     <#assign cmsTarget=menuContext.cms.cmsRequestName/>
  </#if>
  <div class="col"><a href="<@ofbizUrl>${cmsTarget}</@ofbizUrl>" class="${selectedLeftClassMap.CMS?default(unselectedLeftClassName)}">${uiLabelMap.ContentCMS}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindCompDoc</@ofbizUrl>" class="${selectedLeftClassMap.CompDoc?default(unselectedLeftClassName)}">${uiLabelMap.ContentCompDoc}</a></div>

  <#if requestAttributes.userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.login?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>
