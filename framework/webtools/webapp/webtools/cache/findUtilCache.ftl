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
        <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsReloadCacheList}</a>
        <a href="<@ofbizUrl>FindUtilCacheClearAll</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsClearAllCaches}</a>
        <a href="<@ofbizUrl>FindUtilCacheClearAllExpired</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsClearExpiredFromAll}</a>
        <a href="<@ofbizUrl>ForceGarbageCollection</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsRunGC}</a>
    </div>
</#macro>

<h1>${uiLabelMap.PageTitleFindUtilCache}</h1>
<br />

<p>
    <u><b>${uiLabelMap.WebtoolsMemory}:</b></u>
    [<b>${uiLabelMap.WebtoolsTotalMemory}:</b> ${memory}]
    [<b>${uiLabelMap.WebtoolsFreeMemory}:</b> ${freeMemory}]
    [<b>${uiLabelMap.WebtoolsUsedMemory}:</b> ${usedMemory}]
    [<b>${uiLabelMap.WebtoolsMaxMemory}:</b> ${maxMemory}]
</p>
<br />

<@displayButtonBar/>

<#if cacheList?has_content>
    <table class="basic-table light-grid hover-bar" cellspacing="0">
        <tr class="header-row">
            <td>${uiLabelMap.WebtoolsCacheName}</td>
            <td>${uiLabelMap.WebtoolsSize}</td>
            <td>${uiLabelMap.WebtoolsHits}</td>
            <td>${uiLabelMap.WebtoolsMisses}</td>
            <td>${uiLabelMap.WebtoolsRemoves}</td>
            <td>${uiLabelMap.WebtoolsMaxSize}</td>
            <td>${uiLabelMap.WebtoolsExpireTime}</td>
            <td align="center">${uiLabelMap.WebtoolsUseSoftRef}</td>
            <td align="center">${uiLabelMap.WebtoolsUseFileStore}</td>
            <td align="center">${uiLabelMap.WebtoolsAdministration}</td>
        </tr>
        <#assign alt_row = false>
        <#list cacheList as cache>
            <tr<#if alt_row> class="alternate-row"</#if>>
                <td>${cache.cacheName?if_exists}</td>
                <td>${cache.cacheSize?if_exists}</td>
                <td>${cache.hitCount?if_exists}</td>
                <td>${cache.missCountTot?if_exists}/${cache.missCountNotFound?if_exists}/${cache.missCountExpired?if_exists}/${cache.missCountSoftRef?if_exists}</td>
                <td>${cache.removeHitCount?if_exists}/${cache.removeMissCount?if_exists}</td>
                <td>${cache.maxSize?if_exists}</td>
                <td>${cache.expireTime?if_exists}</td>
                <td align="center">${cache.useSoftReference?if_exists}</td>
                <td align="center">${cache.useFileSystemStore?if_exists}</td>
                <td class="button-col">
                    <a href="<@ofbizUrl>FindUtilCacheElements?UTIL_CACHE_NAME=${cache.cacheName?if_exists}</@ofbizUrl>">${uiLabelMap.WebtoolsElements}</a>
                    <#if hasUtilCacheEdit>
                        <a href="<@ofbizUrl>EditUtilCache?UTIL_CACHE_NAME=${cache.cacheName?if_exists}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
                    </#if>
                    <#if hasUtilCacheEdit>
                        <a href="<@ofbizUrl>FindUtilCacheClear?UTIL_CACHE_NAME=${cache.cacheName?if_exists}</@ofbizUrl>">${uiLabelMap.CommonClear}</a>
                    </#if>
                </td>
            </tr>
            <#assign alt_row = !alt_row>
        </#list>
    </table>
<#else>
    ${uiLabelMap.WebtoolsNoUtilCacheFound}
</#if>

<@displayButtonBar/>

