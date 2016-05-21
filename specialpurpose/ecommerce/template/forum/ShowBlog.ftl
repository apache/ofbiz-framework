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

<#import "/includes/BlogLib.ftl" as blog/>

<div class="screenlet" >
<h1>${uiLabelMap.EcommerceFromSite}:</h1><br />
<div>
<@renderSiteAncestryPath trail=siteAncestorList?default([])/>
<#if trailList?? && 1 < trailList?size >
<h1>${uiLabelMap.EcommerceFromParentArticle}:</h1><br />
</#if>
<#if trailList?has_content>
    <@blog.renderAncestryPath trail=trailList startIndex=1 endIndexOffset=1 />
    <#if 0 < trailList?size >
        <#assign pair=trailList[trailList?size - 1]/>
        <#assign pair0 = pair[0]!>
        <#assign pair1 = pair[1]!>
        <hr />
        <h1>${uiLabelMap.EcommerceContentFor} ${pair1!}[${pair0!}]:</h1><br />
    </#if>
<#else>
</#if>

<#assign thisContentId=subContentId!>
<#if !thisContentId?has_content>
    <#assign thisContentId=contentId!>
</#if>
<table border="0" width="100%" class="blogtext">
    <tr>
    <td width="40">&nbsp;</td>
    <td>
    <@renderSubContentCache subContentId=thisContentId />
    </td>
    <td width="40" valign="bottom">
<@checkPermission subContentId=subContentId targetOperation="CONTENT_CREATE|CONTENT_RESPOND" contentPurposeList="RESPONSE" >
<a class="tabButton" href="<@ofbizUrl>AddResponse?contentIdTo=${subContentId}&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" >${uiLabelMap.EcommerceRespond}</a>
</@checkPermission>
<br />

    </td>
    </tr>
</table>

<#--
<@checkPermission mode="not-equals" subContentId=subContentId targetOperation="CONTENT_CREATE|CONTENT_RESPOND" contentPurposeList="RESPONSE" >
            ${permissionErrorMsg!}
</@checkPermission>
-->

<table border="0" width="100%" class="tableheadtext">
<!-- Note that the "...When" arguments in the loopSubContent must be compatible with those in
     any embedded transformSubContent, because it will assume that the first node has already
     had its conditions checked.
     It is not convenient to have the traverseSubContent check or recheck the first node
     because the associated ContentAssoc entity is not known.
-->
        <h1>${uiLabelMap.EcommerceResponses}</h1><br />
<@loopSubContent contentAssocTypeId="RESPONSE" contentId=subContentId mapKey=""
                pickWhen="contentAssocTypeId != null && contentAssocTypeId.equals(\"RESPONSE\") && mapKey == null"
                followWhen="contentAssocTypeId != null && contentAssocTypeId.equals(\"RESPONSE\")">
    <@traverseSubContentCache  contentAssocTypeId="RESPONSE"
                            pickWhen="contentAssocTypeId != null && contentAssocTypeId.equals(\"RESPONSE\")"
                            followWhen="contentAssocTypeId != null && contentAssocTypeId.equals(\"RESPONSE\")"
                            wrapTemplateId=""
                        >
    <#assign indentStr=indent?default("0")/>
    <#assign indent=indentStr?number/>
    <#if 1 < indent >
        <#assign fillRange=1..indent/>
        <#assign indentFill=""/>
        <#list fillRange as i>
            <#assign indentFill = indentFill + "&nbsp;&nbsp;&nbsp;&nbsp;" />
        </#list>
        <#assign thisContentId = ""/>
        <#if nodeTrailCsv??>
            <#assign idList = nodeTrailCsv?split(",")/>
            <#if 0 < idList?size >
                <#assign thisContentId = idList?last>
            </#if>
        </#if>
        <#if content??>
  <tr>
  <td>
        ${indentFill}
        <a class="tabButton" href="<@ofbizUrl>ViewBlog?contentId=${thisContentId}&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" >${uiLabelMap.CommonView}</a>
                     ${content.contentId!}-${content.description!}<br />
  </td>
  </tr>
        </#if>
    </#if>
     </@traverseSubContentCache>
</@loopSubContent>
<@wrapSubContentCache subContentId=subContentId wrapTemplateId="WRAP_NEXT_PREV" >
</@wrapSubContentCache>
</table>
</div>
</div>


<#-- not used, will be deleted -->
<#macro getCurrentContent >
    <#assign globalNodeTrail=globalNodeTrail/>
    <#if globalNodeTrail??>
        <#assign currentNode=globalNodeTrail?last/>
        <#if currentNode??>
            <#assign currentValue=currentNode.value/>
            <#if currentValue??>
                <@wrapSubContentCache subContentId=currentValue.contentId wrapTemplateId="WRAP_ARTICLE" >
                    <@traverseSubContentCache  contentAssocTypeId="SUB_CONTENT"
                            pickWhen="mapKey != null && mapKey.equals(\"ARTICLE\")"
                            returnAfterPickWhen="mapKey != null && mapKey.equals(\"ARTICLE\")"
                            followWhen="contentAssocTypeId != null && contentAssocTypeId.equals(\"_never_\")"
                            wrapTemplateId=""
                        >
                <#assign description=currentValue.description?default("No description")/>
${uiLabelMap.CommonDescription}[${currentValue.contentId!}]:${description}
<a class="tabButton" href="<@ofbizUrl>ViewBlog?contentId=${thisContentId}&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" >${uiLabelMap.CommonView}</a>
                   </@traverseSubContentCache >
                </@wrapSubContentCache>
            </#if>
        </#if>
    </#if>
</#macro>

<#macro renderSiteAncestryPath trail startIndex=0 >
    <#assign indent = "">
    <#assign csv = "">
    <#assign counter = 0>
    <#assign len = trail?size>
    <table border="0" cellspacing="4">
    <#list trail as webSitePublishPoint>
        <#if counter < len && startIndex <= counter >
       <tr>
         <td >
            ${indent}
            <a class="tabButton" href="<@ofbizUrl>main?pubPt=${webSitePublishPoint.contentId!}</@ofbizUrl>" >${uiLabelMap.CommonBackTo}</a> &nbsp;${webSitePublishPoint.templateTitle!}
                <#assign indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
         [${webSitePublishPoint.contentId!}]</td>
        </#if>
       </tr>
        <#assign counter = counter + 1>
    <#if 20 < counter > <#break/></#if>
    </#list>
    </table>
</#macro>
