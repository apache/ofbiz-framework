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

<#if (requestAttributes.uiLabelMap)?exists>
    <#assign uiLabelMap = requestAttributes.uiLabelMap>
</#if>
<#assign selected = headerItem?default("void")>

<div id="app-navigation">
  <h2>${uiLabelMap.FrameworkWebTools}</h2>
  <ul>
    <li<#if selected == "main"> class="selected"</#if>><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonMain}</a></li>
    <#if userLogin?has_content>
      <li<#if selected == "stats"> class="selected"</#if>><a href="<@ofbizUrl>StatsSinceStart</@ofbizUrl>">Stats</a></li>
      <li<#if selected == "jobs"> class="selected"</#if>><a href="<@ofbizUrl>jobList</@ofbizUrl>">Jobs</a></li>
      <li<#if selected == "cache"> class="selected"</#if>><a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>">Cache</a></li>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear" />
</div>
