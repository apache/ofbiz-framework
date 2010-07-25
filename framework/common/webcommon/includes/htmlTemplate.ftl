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

<#include "component://widget/templates/htmlFormMacroLibrary.ftl"/>

<#macro lookupField className="" alert="" name="" value="" size="20" maxlength="20" id="" event="" action="" disabled="" autocomplete="" descriptionFieldName="" formName="" fieldFormName="" targetParameterIter="" imgSrc="" ajaxUrl="" ajaxEnabled="" presentation="" width="20" height="20" position="topleft" fadeBackground="true" clearText="" showDescription="" initiallyCollapsed="">
    <#if (!ajaxEnabled?has_content)>
        <#assign javascriptEnabled = Static["org.ofbiz.base.util.UtilHttp"].isJavaScriptEnabled(request) />
        <#if (javascriptEnabled)>
            <#local ajaxEnabled = true>
        </#if>
    </#if>
    <#if (!id?has_content)>
        <#local id = Static["org.ofbiz.base.util.UtilHttp"].getNextUniqueId(request) />
    </#if>
    <#if (!ajaxUrl?has_content)>
        <#local ajaxUrl = requestAttributes._REQUEST_HANDLER_.makeLink(request, response, fieldFormName)/>
        <#local ajaxUrl = id + "," + ajaxUrl + ",ajaxLookup=Y&amp;searchValueField=" + name />
    </#if>
    <#if (!showDescription?has_content)>
        <#local showDescription = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("widget.properties", "widget.lookup.showDescription", "N")>
        <#if "Y" == showDescription>
            <#local showDescription = "true" />
        <#else>
            <#local showDescription = "false" />
        </#if>
    </#if>
        <#if "true" == disabled>
            <#local disabled = true/>
        <#else>
            <#local disabled = false />
        </#if>    
    <@renderLookupField className alert name value size maxlength id event action disabled autocomplete descriptionFieldName formName fieldFormName targetParameterIter imgSrc ajaxUrl ajaxEnabled presentation width height position fadeBackground clearText showDescription initiallyCollapsed/>
</#macro>

<#macro nextPrev commonUrl="" ajaxEnabled=false javaScriptEnabled=false paginateStyle="nav-pager" paginateFirstStyle="nav-first" viewIndex=0 highIndex=0 listSize=0 viewSize=1 ajaxFirstUrl="" firstUrl="" paginateFirstLabel="" paginatePreviousStyle="nav-previous" ajaxPreviousUrl="" previousUrl="" paginatePreviousLabel="" pageLabel="" ajaxSelectUrl="" selectUrl="" ajaxSelectSizeUrl="" selectSizeUrl="" commonDisplaying="" paginateNextStyle="nav-next" ajaxNextUrl="" nextUrl="" paginateNextLabel="" paginateLastStyle="nav-last" ajaxLastUrl="" lastUrl="" paginateLastLabel="" paginateViewSizeLabel="" >
    <#local javaScriptEnabled = javaScriptEnabled />
    <#if (!javaScriptEnabled)>
        <#local javaScriptEnabled = Static["org.ofbiz.base.util.UtilHttp"].isJavaScriptEnabled(request) />
    </#if>
    <#if (commonUrl?has_content)>
        <#if (!firstUr?has_content)>
            <#local firstUrl=commonUrl+"VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexFirst}"/>
        </#if>
        <#if (!previousUrl?has_content)>
             <#local previousUrl=commonUrl+"VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexPrevious}"/>
        </#if>
        <#if (!firstUr?has_content)>
            <#local nextUrl=commonUrl+"VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexNext}"/>
        </#if>
        <#if (!firstUr?has_content)>
            <#local lastUrl=commonUrl+"VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexLast}"/>
        </#if>
        <#if (!firstUr?has_content)>
            <#local selectUrl=commonUrl+"VIEW_SIZE=${viewSize}&amp;VIEW_INDEX="/>
        </#if>
        <#if (!firstUr?has_content)>
            <#local selectSizeUrl=commonUrl+"VIEW_SIZE='+this.value+'&amp;VIEW_INDEX=0"/>
        </#if>
    </#if>
    <#if (!commonDisplaying?has_content)>
        <#local commonDisplaying="${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}"/>    
    </#if>
    <@renderNextPrev paginateStyle paginateFirstStyle viewIndex highIndex listSize viewSize ajaxEnabled javaScriptEnabled ajaxFirstUrl firstUrl paginateFirstLabel paginatePreviousStyle ajaxPreviousUrl previousUrl paginatePreviousLabel pageLabel ajaxSelectUrl selectUrl ajaxSelectSizeUrl selectSizeUrl commonDisplaying paginateNextStyle ajaxNextUrl nextUrl paginateNextLabel paginateLastStyle ajaxLastUrl lastUrl paginateLastLabel paginateViewSizeLabel />
</#macro>