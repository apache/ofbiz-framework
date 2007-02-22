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

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if webSite?has_content>
    <div class='tabContainer'>
        <a href="<@ofbizUrl>EditWebSite?webSiteId=${webSite.webSiteId}</@ofbizUrl>" class="${selectedClassMap.EditWebSite?default(unselectedClassName)}">${uiLabelMap.ContentWebSite}</a>
        <a href="<@ofbizUrl>ListWebSiteContent?webSiteId=${webSite.webSiteId}</@ofbizUrl>" class="${selectedClassMap.ListWebSiteContent?default(unselectedClassName)}">Content</a>
        <a href="<@ofbizUrl>EditWebSiteParties?webSiteId=${webSite.webSiteId}</@ofbizUrl>" class="${selectedClassMap.EditWebSiteParties?default(unselectedClassName)}">${uiLabelMap.PartyParties}</a>
         <a href="<@ofbizUrl>WebSiteCms?webSiteId=${webSite.webSiteId}</@ofbizUrl>" class="${selectedClassMap.WebSiteCMS?default(unselectedClassName)}">CMS</a>
    </div>
    <div><span class="head1">${uiLabelMap.ContentWebSite}&nbsp;</span><span class="head2"><#if (webSite.siteName)?has_content>"${webSite.siteName}"</#if> [${uiLabelMap.CommonId}:${webSite.webSiteId}]</span></div>
</#if>
