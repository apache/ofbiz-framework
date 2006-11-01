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

<div class="head1">${uiLabelMap.PageTitleFindUtilCacheElements}</div>
<div>&nbsp;</div>
<div class="tabletext"><b>${uiLabelMap.WebtoolsCacheName}:</b> ${cacheName?if_exists} (${now})</div>
<div>&nbsp;</div>
<div width="100%">
    <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsBackToCacheMaintenance}</a>
</div>
<div>&nbsp;</div>
<#if cacheName?has_content>
	<#if cacheElementsList?has_content>
		<#assign rowClass='viewManyTR1'>
		<table cellpadding="2" cellspacing="0" border="1" width="50%" class="boxoutside">
		  <tr>
		    <td><div class="tableheadtext">${uiLabelMap.WebtoolsCacheElementKey}</div></td>
		    <td><div class="tableheadtext">${uiLabelMap.WebtoolsExpireTime}</div></td>
		    <td><div class="tableheadtext">${uiLabelMap.WebtoolsBytes}</div></td>
		    <td><div class="tableheadtext">&nbsp;</div></td>
		  </tr>
		  <#list cacheElementsList as cacheElement>
		  <tr class='${rowClass}'>
		    <td><div class="tabletext">${cacheElement.elementKey?if_exists}</div></td>
		    <td><div class="tabletext">${cacheElement.expireTime?if_exists}</div></td>
		    <td><div class="tabletext">${cacheElement.lineSize?if_exists}</div></td>
		    <td align="center" valign=middle>
		      	<#if hasUtilCacheEdit>
		        	<a href="<@ofbizUrl>FindUtilCacheElementsRemoveElement?UTIL_CACHE_NAME=${cacheName?if_exists}&UTIL_CACHE_ELEMENT_NUMBER=${cacheElement.keyNum?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
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
			${uiLabelMap.WebtoolsNoUtilCacheElementsFound}
		</div>
	</#if>
<#else>
	<div width="100%">
		${uiLabelMap.WebtoolsNoUtilCacheSpecified}
	</div>
</#if>
<div>&nbsp;</div>
<div class="tabletext">${uiLabelMap.WebtoolsSizeTotal}: ${totalSize} ${uiLabelMap.WebtoolsBytes}</div>
<div>&nbsp;</div>
<div width="100%">
    <a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsBackToCacheMaintenance}</a>
</div>
