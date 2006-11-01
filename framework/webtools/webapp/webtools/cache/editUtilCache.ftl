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

<div class="head1">${uiLabelMap.PageTitleEditUtilCache}</div>
<div>&nbsp;</div>
<div class="tabletext"><b>${uiLabelMap.WebtoolsCacheName}:</b> ${cacheName?if_exists}</div>
<div>&nbsp;</div>
<div width="100%">
    <a href="<@ofbizUrl>EditUtilCacheClear?UTIL_CACHE_NAME=${cacheName?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsClearThisCache}</a>
    <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsBackToCacheMaintenance}</a>
</div>
<div>&nbsp;</div>
<#if cache?has_content>
    <form method="POST" action='<@ofbizUrl>EditUtilCacheUpdate?UTIL_CACHE_NAME=${cacheName?if_exists}</@ofbizUrl>'>
        <table cellpadding="2" cellspacing="0" border="1" width="50%" class="boxoutside">
            <tr class='viewManyTR1'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsCacheName}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.cacheName?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR2'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsSize}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.cacheSize?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR1'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsHits}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.hitCount?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR2'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsMissesTotal}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.missCountTot?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR1'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsMissesNotFound}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.missCountNotFound?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR2'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsMissesExpire}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.missCountExpired?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR1'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsMissesSoftReference}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.missCountSoftRef?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR2'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsRemovesHit}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.removeHitCount?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR1'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsRemovesMisses}</div></td>
                <td colspan="2"><div class="tableheadtext">${cache.removeMissCount?if_exists}</div></td>
            </tr>
            <tr class='viewManyTR2'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsMaxSize}</div></td>
                <td><div class="tableheadtext">${cache.maxSize?if_exists}</div></td>
                <td><input type="text" class='inputBox' size="15" maxlength="15" name="UTIL_CACHE_MAX_SIZE" value="${cache.maxSize?if_exists}"></td>
            </tr>
            <tr class='viewManyTR1'>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsExpireTime}</div></td>
                <td><div class="tableheadtext">${cache.expireTime?if_exists} (${cache.hrs?if_exists}:${cache.mins?if_exists}:${cache.secs?if_exists})</div></td>
                <td><input type="text" class='inputBox' size="15" maxlength="15" name="UTIL_CACHE_EXPIRE_TIME" value="${cache.expireTime?if_exists}"></td>
            </tr>
            <tr class='viewManyTR2'>
                <#if cache.useSoftReference?exists && cache.useSoftReference == "true">
                    <#assign selectedTrue = "selected">
                    <#assign selectedFalse = "">
                    <#assign valueSoftRef = "${uiLabelMap.CommonTrue}">
                <#else>
                    <#assign selectedTrue = "">
                    <#assign selectedFalse = "selected">
                    <#assign valueSoftRef = "${uiLabelMap.CommonFalse}">
                </#if>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsUseSoftRef}</div></td>
                <td><div class="tableheadtext">${valueSoftRef}</div></td>
                <td>
                    <select name="UTIL_CACHE_USE_SOFT_REFERENCE" class="selectBox">
                        <option value="true"  ${selectedTrue}>${uiLabelMap.CommonTrue}</option>
                        <option value="false" ${selectedFalse}>${uiLabelMap.CommonFalse}</option>
                    </select>
                  </td>
            </tr>
            <tr class='viewManyTR1'>
                <#if cache.useFileSystemStore?exists && cache.useFileSystemStore == "true">
                    <#assign valueFileSys = "${uiLabelMap.CommonTrue}">
                <#else>
                    <#assign valueFileSys = "${uiLabelMap.CommonFalse}">
                </#if>
                <td><div class="tableheadtext">${uiLabelMap.WebtoolsUseFileStore}</div></td>
                <td colspan="2"><div class="tableheadtext">${valueFileSys}</div></td>
            </tr>
            <tr>
                <td colspan="3" align="center">
                    <input type="submit" value="${uiLabelMap.CommonUpdate}">
                </td>
            </tr>
        </table>
    </form>
<#else>
    <div width="100%">
        ${uiLabelMap.WebtoolsNoUtilCacheSpecified}
    </div>
</#if>
<div>&nbsp;</div>
<div width="100%">
    <a href="<@ofbizUrl>EditUtilCacheClear?UTIL_CACHE_NAME=${cacheName?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsClearThisCache}</a>
    <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsBackToCacheMaintenance}</a>
</div>