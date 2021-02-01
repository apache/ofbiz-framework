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

<#macro renderBegin></#macro>

<#macro renderEnd></#macro>

<#macro renderScreenBegin></#macro>

<#macro renderScreenEnd></#macro>

<#macro renderSectionBegin boundaryComment>
</#macro>

<#macro renderSectionEnd boundaryComment>
</#macro>

<#macro renderContainerBegin id style autoUpdateLink type autoUpdateInterval></#macro>
<#macro renderContainerEnd type></#macro>
<#macro renderContentBegin editRequest enableEditValue editContainerStyle></#macro>
<#macro renderContentBody></#macro>
<#macro renderContentEnd urlString editMode editContainerStyle editRequest enableEditValue></#macro>
<#macro renderSubContentBegin editContainerStyle editRequest enableEditValue></#macro>
<#macro renderSubContentBody></#macro>
<#macro renderSubContentEnd urlString editMode editContainerStyle editRequest enableEditValue></#macro>

<#macro renderHorizontalSeparator id style></#macro>
<#macro renderLabel text="" id="" style="">
    <#if text??>
        ${text}<#lt/>
    </#if>
</#macro>
<#macro renderLink parameterList targetWindow target uniqueItemName linkType actionUrl id style name height width linkUrl text imgStr></#macro>
<#macro renderImage src id style wid hgt border alt urlString></#macro>

<#macro renderContentFrame fullUrl width height border></#macro>
<#macro renderScreenletBegin id title collapsible saveCollapsed collapsibleAreaId expandToolTip collapseToolTip fullUrlString padded menuString showMore collapsed javaScriptEnabled></#macro>
<#macro renderScreenletSubWidget></#macro>
<#macro renderScreenletEnd></#macro>

<#macro renderScreenletPaginateMenu lowIndex actualPageSize ofLabel listSize paginateLastStyle lastLinkUrl paginateLastLabel paginateNextStyle nextLinkUrl paginateNextLabel paginatePreviousStyle paginatePreviousLabel previousLinkUrl paginateFirstStyle paginateFirstLabel firstLinkUrl></#macro>

<#macro renderColumnContainerBegin id style></#macro>
<#macro renderColumnContainerEnd></#macro>
<#macro renderColumnBegin id style></#macro>
<#macro renderColumnEnd></#macro>
