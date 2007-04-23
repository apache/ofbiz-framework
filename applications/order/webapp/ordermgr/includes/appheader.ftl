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

<#assign selected = headerItem?default("void")>

<div id="app-navigation">
  <h2>${uiLabelMap.OrderOrderManagerApplication}</h2>
  <ul>
    <#if (security.hasEntityPermission("ORDERMGR", "_VIEW", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", session))>
    <li<#if selected = "request"> class="selected"</#if>><a href="<@ofbizUrl>FindRequest</@ofbizUrl>">${uiLabelMap.OrderRequests}</a></li>
    <li<#if selected = "quote"> class="selected"</#if>><a href="<@ofbizUrl>FindQuote</@ofbizUrl>">${uiLabelMap.OrderOrderQuotes}</a></li>
    </#if>
    <#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
    <li<#if selected = "orderlist"> class="selected"</#if>><a href="<@ofbizUrl>orderlist</@ofbizUrl>">${uiLabelMap.OrderOrderList}</a></li>
    </#if>
    <#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
    <li<#if selected = "findorders"> class="selected"</#if>><a href="<@ofbizUrl>findorders</@ofbizUrl>">${uiLabelMap.OrderFindOrder}</a></li>
    </#if>
    <#if (security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session))>
    <li<#if selected = "orderentry"> class="selected"</#if>><a href="<@ofbizUrl>orderentry</@ofbizUrl>">${uiLabelMap.OrderOrderEntry}</a></li>
    </#if>
    <#if security.hasEntityPermission("ORDERMGR", "_RETURN", session)>
    <li<#if selected = "return"> class="selected"</#if>><a href="<@ofbizUrl>findreturn</@ofbizUrl>">${uiLabelMap.OrderOrderReturns}</a></li>
    </#if>
    <#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session) || security.hasRolePermission("ORDERMGR_ROLE", "_VIEW", "", "", session)>
    <li<#if selected = "requirement"> class="selected"</#if>><a href="<@ofbizUrl>FindRequirements</@ofbizUrl>">${uiLabelMap.OrderRequirements}</a></li>
    <li<#if selected = "tasklist"> class="selected"</#if>><a href="<@ofbizUrl>tasklist</@ofbizUrl>">${uiLabelMap.OrderOrderTasks}</a></li>
    </#if>
    <li<#if selected = "reports"> class="selected"</#if>><a href="<@ofbizUrl>OrderPurchaseReportOptions</@ofbizUrl>">${uiLabelMap.CommonReports}</a></li>
    <li<#if selected = "stats"> class="selected"</#if>><a href="<@ofbizUrl>orderstats</@ofbizUrl>">${uiLabelMap.CommonStats}</a></li>

    <#if userLogin?has_content>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear" />
</div>
