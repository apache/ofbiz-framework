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

<div class="head1">${uiLabelMap.PageTitleFindUtilCache}</div>
<div width="100%">
    <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsReloadCacheList}</a>
    <a href="<@ofbizUrl>FindUtilCacheClearAll</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsClearAllCaches}</a>
    <a href="<@ofbizUrl>FindUtilCacheClearAllExpired</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsClearExpiredFromAll}</a>
    <a href="<@ofbizUrl>ForceGarbageCollection</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsRunGC}</a>
</div>
<div class="tabletext"><u><b>${uiLabelMap.WebtoolsMemory}:</b></u> [<b>${uiLabelMap.WebtoolsTotalMemory}:</b> ${memory}] [<b>${uiLabelMap.WebtoolsFreeMemory}:</b> ${freeMemory}] [<b>${uiLabelMap.WebtoolsUsedMemory}:</b> ${usedMemory}] [<b>${uiLabelMap.WebtoolsMaxMemory}:</b> ${maxMemory}]</span></div>
<div>&nbsp;</div>
<#if cacheList?has_content>
    <#assign rowClass='viewManyTR1'>
    <table cellpadding="2" cellspacing="0" border="1" width="100%" class="boxoutside">
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsCacheName}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsSize}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsHits}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsMisses}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsRemoves}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsMaxSize}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsExpireTime}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsUseSoftRef}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.WebtoolsUseFileStore}</div></td>
        <td colspan="3"><div class="tableheadtext">${uiLabelMap.WebtoolsAdministration}</div></td>
      </tr>
      <#list cacheList as cache>
      <tr class='${rowClass}'>
        <td><div class="tabletext">${cache.cacheName?if_exists}</div></td>
        <td><div class="tabletext">${cache.cacheSize?if_exists}</div></td>
        <td><div class="tabletext">${cache.hitCount?if_exists}</div></td>
        <td><div class="tabletext">${cache.missCountTot?if_exists}/${cache.missCountNotFound?if_exists}/${cache.missCountExpired?if_exists}/${cache.missCountSoftRef?if_exists}</div></td>
        <td><div class="tabletext">${cache.removeHitCount?if_exists}/${cache.removeMissCount?if_exists}</div></td>
        <td><div class="tabletext">${cache.maxSize?if_exists}</div></td>
        <td><div class="tabletext">${cache.expireTime?if_exists}</div></td>
        <td><div class="tabletext">${cache.useSoftReference?if_exists}</div></td>
        <td><div class="tabletext">${cache.useFileSystemStore?if_exists}</div></td>
        <td align="center" valign=middle>
            <a href="<@ofbizUrl>FindUtilCacheElements?UTIL_CACHE_NAME=${cache.cacheName?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsElements}</a>
        </td>
        <td align="center" valign=middle>
              <#if hasUtilCacheEdit>
                <a href="<@ofbizUrl>EditUtilCache?UTIL_CACHE_NAME=${cache.cacheName?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
              </#if>
        </td>
        <td align="center" valign=middle>
              <#if hasUtilCacheEdit>
                <a href="<@ofbizUrl>FindUtilCacheClear?UTIL_CACHE_NAME=${cache.cacheName?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClear}</a>
              </#if>
        </td>
      </tr>
      <#if rowClass=='viewManyTR1'>
          <#assign rowClass='viewManyTR2'>
      <#else>
        <#assign rowClass='viewManyTR1'>
      </#if>
        </#list>
    </table>
<#else>
    <div width="100%">
    ${uiLabelMap.WebtoolsNoUtilCacheFound}
    </div>
</#if>
<div width="100%">
    <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsReloadCacheList}</a>
    <a href="<@ofbizUrl>FindUtilCacheClearAll</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsClearAllCaches}</a>
    <a href="<@ofbizUrl>FindUtilCacheClearAllExpired</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsClearExpiredFromAll}</a>
    <a href="<@ofbizUrl>ForceGarbageCollection</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsRunGC}</a>
</div>
