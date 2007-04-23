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

<#macro displayButtonBar>
    <div class="button-bar">
        <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsBackToCacheMaintenance}</a>
    </div>
</#macro>

<h1>${uiLabelMap.PageTitleFindUtilCacheElements}</h1>
<br />

<p>
    <b>${uiLabelMap.WebtoolsCacheName}:</b> ${cacheName?if_exists} (${now})
    <b>${uiLabelMap.WebtoolsSizeTotal}:</b> ${totalSize} ${uiLabelMap.WebtoolsBytes}
</p>
<br />

<@displayButtonBar/>

<#if cacheName?has_content>
    <#if cacheElementsList?has_content>
        <table class="basic-table light-grid hover-bar" cellspacing="0">
            <tr class="header-row">
                <td>${uiLabelMap.WebtoolsCacheElementKey}</td>
                <td>${uiLabelMap.WebtoolsExpireTime}</td>
                <td>${uiLabelMap.WebtoolsBytes}</td>
                <td>&nbsp;</td>
            </tr>
            <#assign alt_row = false>
            <#list cacheElementsList as cacheElement>
                <tr<#if alt_row> class="alternate-row"</#if>>
                    <td>${cacheElement.elementKey?if_exists}</td>
                    <td nowrap="nowrap">${cacheElement.expireTime?if_exists}</td>
                    <td>${cacheElement.lineSize?if_exists}</td>
                    <td class="button-col">
                        <#if hasUtilCacheEdit>
                            <a href="<@ofbizUrl>FindUtilCacheElementsRemoveElement?UTIL_CACHE_NAME=${cacheName?if_exists}&UTIL_CACHE_ELEMENT_NUMBER=${cacheElement.keyNum?if_exists}</@ofbizUrl>">${uiLabelMap.CommonRemove}</a>
                        </#if>
                    </td>
                </tr>
                <#assign alt_row = !alt_row>
            </#list>
        </table>
    <#else>
        ${uiLabelMap.WebtoolsNoUtilCacheElementsFound}
    </#if>
<#else>
    ${uiLabelMap.WebtoolsNoUtilCacheSpecified}
</#if>

<@displayButtonBar/>

