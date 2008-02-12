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

<#assign selected = tabButtonItem?default("void")>

<#if webSite?has_content>
<div class="button-bar tab-bar">
    <ul>
      <li<#if selected == "EditWebSite"> class="selected"</#if>><a href="<@ofbizUrl>EditWebSite?webSiteId=${webSite.webSiteId}</@ofbizUrl>">${uiLabelMap.ContentWebSite}</a></li>
      <li<#if selected == "ListWebSiteContent"> class="selected"</#if>><a href="<@ofbizUrl>ListWebSiteContent?webSiteId=${webSite.webSiteId}</@ofbizUrl>">${uiLabelMap.ContentContent}</a></li>
      <li<#if selected == "EditWebSiteParties"> class="selected"</#if>><a href="<@ofbizUrl>EditWebSiteParties?webSiteId=${webSite.webSiteId}</@ofbizUrl>">${uiLabelMap.PartyParties}</a></li>
      <li<#if selected == "WebSiteCMS"> class="selected"</#if>><a href="<@ofbizUrl>WebSiteCms?webSiteId=${webSite.webSiteId}</@ofbizUrl>">${uiLabelMap.ContentCMS}</a></li>
      <br class="clear"/>
    </ul>
</div>
<div class="h1">${uiLabelMap.ContentWebSite}&nbsp;<#if (webSite.siteName)?has_content>${webSite.siteName}</#if> [${uiLabelMap.CommonId} ${webSite.webSiteId}]</div>
</#if>