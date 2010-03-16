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
        <a href="<@ofbizUrl>EditUtilCacheClear?UTIL_CACHE_NAME=${cacheName?if_exists}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsClearThisCache}</a>
        <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsBackToCacheMaintenance}</a>
    </div>
</#macro>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.PageTitleEditUtilCache}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <br />
    <@displayButtonBar/>

    <#if cache?has_content>
        <form method="post" action="<@ofbizUrl>EditUtilCacheUpdate?UTIL_CACHE_NAME=${cacheName?if_exists}</@ofbizUrl>">
            <table class="basic-table" cellspacing="0">
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsCacheName}</td>
                    <td colspan="2">${cache.cacheName?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsSize}</td>
                    <td colspan="2">${cache.cacheSize?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsHits}</td>
                    <td colspan="2">${cache.hitCount?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsMissesTotal}</td>
                    <td colspan="2">${cache.missCountTot?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsMissesNotFound}</td>
                    <td colspan="2">${cache.missCountNotFound?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsMissesExpire}</td>
                    <td colspan="2">${cache.missCountExpired?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsMissesSoftReference}</td>
                    <td colspan="2">${cache.missCountSoftRef?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsRemovesHit}</td>
                    <td colspan="2">${cache.removeHitCount?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsRemovesMisses}</td>
                    <td colspan="2">${cache.removeMissCount?if_exists}</td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsMaxSize}</td>
                    <td>${cache.maxSize?if_exists}</td>
                    <td><input type="text" size="15" maxlength="15" name="UTIL_CACHE_MAX_SIZE" value="${cache.maxSize?if_exists}"/></td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsExpireTime}</td>
                    <td>${cache.expireTime?if_exists} (${cache.hrs?if_exists}:${cache.mins?if_exists}:${cache.secs?if_exists})</td>
                    <td><input type="text" size="15" maxlength="15" name="UTIL_CACHE_EXPIRE_TIME" value="${cache.expireTime?if_exists}"/></td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsUseSoftRef}</td>
                    <td>${cache.useSoftReference?if_exists}</td>
                    <td>
                        <select name="UTIL_CACHE_USE_SOFT_REFERENCE">
                            <option value="true" <#if cache.useSoftReference?default('false') == 'true'>selected="selected"</#if>>${uiLabelMap.CommonTrue}</option>
                            <option value="false" <#if cache.useSoftReference?default('true') == 'false'>selected="selected"</#if>>${uiLabelMap.CommonFalse}</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td class="label">${uiLabelMap.WebtoolsUseFileStore}</td>
                    <td colspan="2">${cache.useFileSystemStore?if_exists}</td>
                </tr>
                <tr>
                    <td colspan="3" align="center">
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                    </td>
                </tr>
            </table>
        </form>
    <#else>
        ${uiLabelMap.WebtoolsNoUtilCacheSpecified}
    </#if>

    <@displayButtonBar/>
  </div>
</div>
