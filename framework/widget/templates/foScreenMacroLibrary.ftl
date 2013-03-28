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
<#macro getFoStyle style>
    <#assign foStyles = {
        "tabletext":"border-left=\"solid black\" border-right=\"solid black\" padding-left=\"2pt\" padding-top=\"2pt\"",
        "tabletextright":"border-left=\"solid black\" border-right=\"solid black\" padding-left=\"2pt\" padding-top=\"2pt\" text-align=\"right\"",
        "tableheadverysmall":"column-width=\"0.3in\"",
        "tableheadsmall":"column-width=\"0.5in\"",
        "tableheadmedium":"column-width=\"1.5in\"",
        "tableheadwide":"column-width=\"3in\"",
        "head1":"font-size=\"12\" font-weight=\"bold\"",
        "head2":"font-weight=\"bold\"",
        "head3":"font-weight=\"bold\" font-style=\"italic\"",
        "h1":"font-size=\"12\" font-weight=\"bold\"",
        "h2":"font-weight=\"bold\"",
        "h3":"font-weight=\"bold\" font-style=\"italic\"",
        "error":"color=\"red\""}/>
    <#assign foStyle = foStyles[style]?default("")/>
    ${foStyle?default("")}
</#macro>

<#escape x as x?xml>

<#macro renderScreenBegin>
<?xml version="1.0" encoding="UTF-8"?>
</#macro>

<#macro renderScreenEnd>
</#macro>

<#macro renderSectionBegin boundaryComment>
</#macro>

<#macro renderSectionEnd boundaryComment>
</#macro>
<#macro renderContainerBegin id style autoUpdateLink autoUpdateInterval><fo:block <#if style?has_content><@getFoStyle style/></#if>></#macro>
<#macro renderContainerEnd></fo:block></#macro>
<#macro renderContentBegin editRequest enableEditValue editContainerStyle></#macro>
<#macro renderContentBody></#macro>
<#macro renderContentEnd></#macro>
<#macro renderSubContentBegin></#macro>
<#macro renderSubContentBody></#macro>
<#macro renderSubContentEnd urlString editMode editContainerStyle editRequest enableEditValue></#macro>

<#macro renderHorizontalSeparator id style><fo:block><fo:leader leader-length="100%" leader-pattern="rule" rule-style="solid" rule-thickness="0.1mm" color="black"/></fo:block></#macro>
<#macro renderLabel text id style><#if text?has_content><fo:block <#if style?has_content><@getFoStyle style/></#if>>${text}</fo:block></#if></#macro>
<#macro renderLink parameterList targetWindow target uniqueItemName linkType actionUrl id style name linkUrl text imgStr></#macro>
<#macro renderImage src id style wid hgt border alt urlString></#macro>

<#macro renderContentFrame></#macro>
<#macro renderScreenletBegin id title collapsible saveCollapsed collapsibleAreaId expandToolTip collapseToolTip fullUrlString padded menuString showMore collapsed javaScriptEnabled></#macro>
<#macro renderScreenletSubWidget></#macro>
<#macro renderScreenletEnd></#macro>

<#macro renderScreenletPaginateMenu lowIndex actualPageSize ofLabel listSize paginateLastStyle lastLinkUrl paginateLastLabel paginateNextStyle nextLinkUrl paginateNextLabel paginatePreviousStyle paginatePreviousLabel previousLinkUrl paginateFirstStyle paginateFirstLabel firstLinkUrl></#macro>
</#escape>

