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

<#macro renderScreenBegin>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
</#macro>

<#macro renderScreenEnd>
</#macro>

<#macro renderSectionBegin boundaryComment>
<#if boundaryComment?has_content>
<!-- ${boundaryComment} -->
</#if>
</#macro>

<#macro renderSectionEnd boundaryComment>
<#if boundaryComment?has_content>
<!-- ${boundaryComment} -->
</#if>
</#macro>

<#macro renderContainerBegin id style autoUpdateLink autoUpdateInterval>
<#if autoUpdateLink?has_content>
<script type="text/javascript">ajaxUpdateAreaPeriodic('${id}', '${autoUpdateLink}', '', '${autoUpdateInterval}');</script>
</#if>
<div<#if id?has_content> id="${id}"</#if><#if style?has_content> class="${style}"</#if>>
</#macro>
<#macro renderContainerEnd></div></#macro>
<#macro renderContentBegin editRequest enableEditValue editContainerStyle><#if editRequest?has_content && enableEditValue == "true"><div class=${editContainerStyle}></#if></#macro>
<#macro renderContentBody></#macro>
<#macro renderContentEnd urlString editMode editContainerStyle editRequest enableEditValue>
<#if editRequest?exists && enableEditValue == "true">
<#if urlString?exists><a href="${urlString}">${editMode}</a><#rt/></#if>
<#if editContainerStyle?exists></div><#rt/></#if>
</#if>
</#macro>
<#macro renderSubContentBegin editContainerStyle editRequest enableEditValue><#if editRequest?exists && enableEditValue == "true"><div class="${editContainerStyle}"></#if></#macro>
<#macro renderSubContentBody></#macro>
<#macro renderSubContentEnd urlString editMode editContainerStyle editRequest enableEditValue>
<#if editRequest?exists && enableEditValue == "true">
<#if urlString?exists><a href="${urlString}">${editMode}</a><#rt/></#if>
<#if editContainerStyle?exists></div><#rt/></#if>
</#if>
</#macro>

<#macro renderHorizontalSeparator id style><hr<#if id?has_content> id="${id}"</#if><#if style?has_content> class="${style}"</#if>/></#macro>

<#macro renderLabel text id style>
  <#if text?has_content>
    <#-- Label is considered block level element in screen widget. There is not reason to render text outside of any html element. Use of style element has set pattern and we'll use style 
       to determine appropriate html element to use -->
    <#if style?has_content>
      <#if style=="h1">
        <h1 
      <#elseif style=="h2">
        <h2 
      <#elseif style=="h3">
        <h3 
      <#elseif style=="h4">
        <h4 
      <#elseif style=="h5">
        <h5 
      <#elseif style=="h6">
        <h6 
      <#else>
        <p class="${style}" 
      </#if>
    <#else>
      <p 
    </#if>
    <#if id?has_content >
        <#if id?has_content> id="${id}"</#if>
    </#if>
        >${text}
    <#if style?has_content>
      <#if style=="h1">
        </h1> 
      <#elseif style=="h2">
        </h2> 
      <#elseif style=="h3">
        </h3> 
      <#elseif style=="h4">
        </h4> 
      <#elseif style=="h5">
        </h5> 
      <#elseif style=="h6">
        </h6> 
      <#else>
        </p> 
      </#if>
    <#else>
      </p>
    </#if>
  </#if>
</#macro>

<#macro renderLink parameterList targetWindow target uniqueItemName linkType actionUrl id style name linkUrl text imgStr>
<#if "hidden-form" == linkType>
<form method="post" action="${actionUrl}" <#if targetWindow?has_content>target="${targetWindow}"</#if> onsubmit="javascript:submitFormDisableSubmits(this)" name="${uniqueItemName}"><#rt/>
<#list parameterList as parameter>
<input name="${parameter.name}" value="${parameter.value}" type="hidden"/><#rt/>
</#list>
</form><#rt/>
</#if>
<a <#if id?has_content>id="${id}"</#if> <#if style?has_content>class="${style}"</#if> <#if name?has_content>name="${name}"</#if> <#if targetWindow?has_content>target="${targetWindow}"</#if> href="<#if "hidden-form"==linkType>javascript:document.${uniqueItemName}.submit()<#else>${linkUrl}</#if>"><#rt/>
<#if imgStr?has_content>${imgStr}<#else><#if text?has_content>${text}</#if></#if></a>
</#macro>
<#macro renderImage src id style wid hgt border alt urlString>
<#if src?has_content>
<img <#if id?has_content>id="${id}"</#if><#if style?has_content> class="${style}"</#if><#if wid?has_content> width="${wid}"</#if><#if hgt?has_content> height="${hgt}"</#if><#if border?has_content> border="${border}"</#if> alt="<#if alt?has_content>${alt}</#if>" src="${urlString}" />
</#if>
</#macro>

<#macro renderContentFrame fullUrl width height border><iframe src="${fullUrl}" width="${width}" height="${height}" <#if border?has_content>border="${border}"</#if> /></#macro>
<#macro renderScreenletBegin id title collapsible saveCollapsed collapsibleAreaId expandToolTip collapseToolTip fullUrlString padded menuString showMore collapsed javaScriptEnabled>
<div class="screenlet"<#if id?has_content> id="${id}"</#if>><#rt/>
<#if showMore>
<div class="screenlet-title-bar"><ul><#if title?has_content><li class="h3">${title}</li></#if>
<#if collapsible>
<li class="<#rt/>
<#if collapsed>
collapsed"><a <#if javaScriptEnabled>onclick="javascript:toggleScreenlet(this, '${collapsibleAreaId}', '${saveCollapsed?string}', '${expandToolTip}', '${collapseToolTip}');"<#else>href="${fullUrlString}"</#if><#if expandToolTip?has_content> title="${expandToolTip}"</#if>
<#else>
expanded"><a <#if javaScriptEnabled>onclick="javascript:toggleScreenlet(this, '${collapsibleAreaId}', '${saveCollapsed?string}', '${expandToolTip}', '${collapseToolTip}');"<#else>href="${fullUrlString}"</#if><#if expandToolTip?has_content> title="${expandToolTip}"</#if>
</#if>
>&nbsp;</a></li>
</#if>
<#if !collapsed>
${menuString}
</#if>
</ul><br class="clear" /></div>
</#if>
<div <#if collapsibleAreaId?has_content> id="${collapsibleAreaId}" <#if collapsed> style="display: none;"</#if></#if><#if padded> class="screenlet-body"<#else> class="screenlet-body no-padding"</#if>>
</#macro>
<#macro renderScreenletSubWidget></#macro>
<#macro renderScreenletEnd></div></div></#macro>
<#macro renderScreenletPaginateMenu lowIndex actualPageSize ofLabel listSize paginateLastStyle lastLinkUrl paginateLastLabel paginateNextStyle nextLinkUrl paginateNextLabel paginatePreviousStyle paginatePreviousLabel previousLinkUrl paginateFirstStyle paginateFirstLabel firstLinkUrl>
    <li class="${paginateLastStyle}<#if !lastLinkUrl?has_content> disabled</#if>"><#if lastLinkUrl?has_content><a href="${lastLinkUrl}">${paginateLastLabel}</a><#else>${paginateLastLabel}</#if></li>
    <li class="${paginateNextStyle}<#if !nextLinkUrl?has_content> disabled</#if>"><#if nextLinkUrl?has_content><a href="${nextLinkUrl}">${paginateNextLabel}</a><#else>${paginateNextLabel}</#if></li>
    <#if (listSize?number > 0) ><li>${lowIndex?number + 1} - ${lowIndex?number + actualPageSize?number} ${ofLabel} ${listSize}</li><#rt/></#if>
    <li class="${paginatePreviousStyle?default("nav-previous")}<#if !previousLinkUrl?has_content> disabled</#if>"><#if previousLinkUrl?has_content><a href="${previousLinkUrl}">${paginatePreviousLabel}</a><#else>${paginatePreviousLabel}</#if></li>
    <li class="${paginateFirstStyle?default("nav-first")}<#if !firstLinkUrl?has_content> disabled</#if>"><#if firstLinkUrl?has_content><a href="${firstLinkUrl}">${paginateFirstLabel}</a><#else>${paginateFirstLabel}</#if></li>
</#macro>
