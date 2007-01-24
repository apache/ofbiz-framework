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

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {page.headerItem?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {page.headerItem?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">&nbsp;${uiLabelMap.OrderOrderManagerApplication}&nbsp;</div>
<div class="row">
  <#-- Just goes to Find Orders for now <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.CommonMain}</a></div> -->
  <#if (security.hasEntityPermission("ORDERMGR", "_VIEW", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", session))>
  <div class="col"><a href="<@ofbizUrl>FindRequest</@ofbizUrl>" class="${selectedLeftClassMap.request?default(unselectedLeftClassName)}">${uiLabelMap.OrderRequests}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindQuote</@ofbizUrl>" class="${selectedLeftClassMap.quote?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderQuotes}</a></div>
  </#if>
  <#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
  <div class="col"><a href="<@ofbizUrl>orderlist</@ofbizUrl>" class="${selectedLeftClassMap.orderlist?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderList}</a></div>  
  </#if>
  <#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
  <div class="col"><a href="<@ofbizUrl>findorders</@ofbizUrl>" class="${selectedLeftClassMap.findorders?default(unselectedLeftClassName)}">${uiLabelMap.OrderFindOrder}</a></div>  
  </#if>
  <#if (security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session))>
  <div class="col"><a href="<@ofbizUrl>orderentry</@ofbizUrl>" class="${selectedLeftClassMap.orderentry?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderEntry}</a></div>  
  </#if>
  <#if security.hasEntityPermission("ORDERMGR", "_RETURN", session)>
  <div class="col"><a href="<@ofbizUrl>findreturn</@ofbizUrl>" class="${selectedLeftClassMap.return?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderReturns}</a></div>
  </#if>
  <#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session) || security.hasRolePermission("ORDERMGR_ROLE", "_VIEW", "", "", session)>
  <div class="col"><a href="<@ofbizUrl>FindRequirements</@ofbizUrl>" class="${selectedLeftClassMap.requirement?default(unselectedLeftClassName)}">${uiLabelMap.OrderRequirements}</a></div>
  <div class="col"><a href="<@ofbizUrl>tasklist</@ofbizUrl>" class="${selectedLeftClassMap.tasklist?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderTasks}</a></div>
  </#if>

  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.logout?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>  
  <div class="col-right"><a href='<@ofbizUrl>OrderPurchaseReportOptions</@ofbizUrl>' class="${selectedRightClassMap.reports?default(unselectedRightClassName)}">${uiLabelMap.CommonReports}</a></div>
  <div class="col-right"><a href='<@ofbizUrl>orderstats</@ofbizUrl>' class="${selectedRightClassMap.stats?default(unselectedRightClassName)}">${uiLabelMap.CommonStats}</a></div>
  <div class="col-fill">&nbsp;</div>
</div>
