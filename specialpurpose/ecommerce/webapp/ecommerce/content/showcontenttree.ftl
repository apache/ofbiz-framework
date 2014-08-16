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

<#include "bloglib.ftl" />
<#assign siteId = requestParameters.contentId! />

<@renderAncestryPath trail=ancestorList?default([]) endIndexOffset=1 siteId=siteId searchOn="true"/>

<#if ancestorList?has_content && (0 < ancestorList?size) >
    <#assign lastContent=ancestorList?last />
    <h1>[${lastContent.contentId}] ${lastContent.description}
              <a class="tabButton" href="<@ofbizUrl>searchContent?siteId=${lastContent.contentId!}</@ofbizUrl>" >${uiLabelMap.CommonSearch}</a>
    </h1>
</#if>

<#assign viewIdx = "" />
<#if requestParameters.viewIndex?has_content>
<#assign viewIdx = requestParameters.viewIndex! />
</#if>
<#assign viewSz = "" />
<#if requestParameters.viewSize?has_content>
<#assign viewSz = requestParameters.viewSize! />
</#if>
<#assign nodeTrailCsv=requestParameters.nodeTrailCsv!/>
<#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in showcontenttree, nodeTrailCsv(0):" + nodeTrailCsv, "")/>
<#assign nodeTrail=[]/>
<#assign firstContentId=""/>
<#if nodeTrailCsv?has_content>
  <#assign nodeTrail=Static["org.ofbiz.base.util.StringUtil"].split(nodeTrailCsv, ",") />
  <#if 0 < nodeTrail?size>
    <#assign firstContentId=nodeTrail[0]?string/>
  </#if>
</#if>

<#--
<#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in showcontenttree, siteId:" + siteId, "")/>
<#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in showcontenttree, nodeTrail:" + nodeTrail, "")/>
-->

<div>
    <@renderCategoryBrowse contentId=siteId indentIndex=1 nodeTrail=nodeTrail />
</div>

<#macro renderCategoryBrowse contentId="" indentIndex=0 nodeTrail=[] viewSz=9999 viewIdx=0>
    <!-- start of renderCategoryBrowse for contentId=${contentId} -->

        <#local contentIdx = contentId! />
        <#if (!contentIdx?? || contentIdx?length == 0)>
            <#local contentIdx = page.contentIdx! />
            <#if (!contentIdx?? || contentIdx?length == 0)>
            </#if>
        </#if>

        <#local thisContentId=nodeTrail[indentIndex]!/>

        <#local thisNodeTrailCsv = "" />
        <#local listUpper = (indentIndex - 1) />
        <#if nodeTrail?size < listUpper >
            <#local listUpper = (nodeTrail?size - 1)>
        </#if>
        <#list 0..listUpper as idx>
            <#if thisNodeTrailCsv?has_content>
                <#local thisNodeTrailCsv = thisNodeTrailCsv + ","/>
            </#if>
            <#if nodeTrail[idx]??>
              <#local thisNodeTrailCsv = thisNodeTrailCsv + nodeTrail[idx]>
            </#if>
        </#list>

        <!-- in showcontenttree, contentIdx: ${contentIdx} -->

        <!-- Look for content first -->
        <@loopSubContent contentId=contentIdx viewIndex=viewIdx viewSize=viewSz contentAssocTypeId="PUBLISH_LINK" returnAfterPickWhen="1==1">
            <#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in showcontenttree, nodeTrailCsv(1):" + nodeTrailCsv, "")/>
            <#local thisCsv=thisNodeTrailCsv + "," + subContentId />
            <a class="tabButton" href="<@ofbizUrl>viewcontent?contentId=${subContentId!}&nodeTrailCsv=${thisCsv}</@ofbizUrl>">${uiLabelMap.CommonView}</a>  ${content.description!}<br />
        </@loopSubContent>


        <!-- Look for sub-topics -->
        <@loopSubContent contentId=contentIdx viewIndex=viewIdx viewSize=viewSz returnAfterPickWhen="1==1" orderBy="contentName">
            <#local plusMinus="+"/>
            <#if thisContentId == subContentId>
                <#local plusMinus="-"/>
            </#if>
            <#local thisCsv=thisNodeTrailCsv />
            <#local thisCsv=thisNodeTrailCsv + "," + subContentId />
            <a class="tabButton" href="<@ofbizUrl>showcontenttree?contentId=${siteId!}&nodeTrailCsv=${thisCsv}</@ofbizUrl>" >${plusMinus}</a> &nbsp;${content.description!}
            <a class="tabButton" href="<@ofbizUrl>searchContent?siteId=${subContentId!}&nodeTrailCsv=${thisCsv}</@ofbizUrl>" >${uiLabelMap.CommonSearch}</a> <br />
            <#if thisContentId == subContentId>
                <#assign catTrail = nodeTrail + [subContentId]/>
                <div><@renderCategoryBrowse contentId=subContentId indentIndex=(indentIndex + 1) nodeTrail=catTrail viewSz=viewSz viewIdx=viewIdx /></div>
            </#if>
        </@loopSubContent>
</#macro>
