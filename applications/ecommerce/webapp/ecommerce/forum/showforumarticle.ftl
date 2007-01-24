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

<#import "bloglib.ftl" as blog/>
<div class="screenlet" >
<div style="margin:10px;" >
<@blog.renderAncestryPath trail=ancestorList?default([]) endIndexOffset=1 />

<#-- Do this so that we don't have to find the content twice (again in renderSubContent) -->
<#assign lastNode = globalNodeTrail?if_exists?last/>
<#if lastNode?has_content>
  <#assign subContent=lastNode.value/>
<#else>
<#assign subContent = delegator.findByPrimaryKeyCache("Content", Static["org.ofbiz.base.util.UtilMisc"].toMap("contentId", subContentId))/>
<#assign dummy = globalNodeTrail.add(lastNode)/>
</#if>
<br/>
<div class="head1">${uiLabelMap.EcommerceContentFor} [${subContentId}] ${subContent.contentName?if_exists} - ${subContent.description?if_exists}:</div><br/>
<table border="0" width="100%" class="blogtext">
    <tr>
    <td width="40">&nbsp;</td>
    <td>
    <@renderSubContentCache subContentId=requestParameters.contentId />
    </td>
    <td width="40" valign="bottom">
<#--
<@wrapSubContentCache subContentId=subContentId wrapTemplateId="WRAP_VIEW" >
</@wrapSubContentCache >
-->
<@checkPermission mode="equals" entityOperation="_CREATE" targetOperation="HAS_USER_ROLE" >
    <a class="tabButton" href="<@ofbizUrl>createforumresponse?contentIdTo=${requestParameters.contentId}&amp;nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" >${uiLabelMap.EcommerceRespond}</a>
</@checkPermission>
<br/>

    </td>
    </tr>
</table>
<hr/>
<#--
<@checkPermission mode="not-equals" subContentId=subContentId targetOperation="CONTENT_CREATE|CONTENT_RESPOND" contentPurposeList="RESPONSE" >
            ${permissionErrorMsg?if_exists}
</@checkPermission>
-->

        <div class="head1">${uiLabelMap.EcommerceResponses}</div><br/>
<table border="0" width="100%" class="tableheadtext">
<@loopSubContentCache  contentAssocTypeId="RESPONSE" subContentId=subContentId mapKey=""
                pickWhen="contentAssocTypeId != null && \"RESPONSE\".equals(contentAssocTypeId) && mapKey == null"
                followWhen="contentAssocTypeId != null && \"_never_\".equals(contentAssocTypeId)"
>
  <tr>
    <#assign indentStr=indent?default("0")/>
    <#assign indent=indentStr?number/>
    <#if 1 < indent >
  <td class="tabletext">
        <#assign thisContentId = ""/>
        <#if nodeTrailCsv?exists>
            <#assign idList = nodeTrailCsv?split(",")/>
            <#if 0 < idList?size >
                <#assign thisContentId = idList?last>
            </#if>
        </#if>
        <#if content?exists>
        <a class="tabButton" href="<@ofbizUrl>showforumresponse?contentId=${thisContentId}&amp;nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" >${uiLabelMap.CommonView}</a>
[${thisContentId}] ${content.contentName?if_exists}-${content.description?if_exists}
        </#if>

    </#if>
</@loopSubContentCache >
<#--
<@wrapSubContentCache subContentId=subContentId wrapTemplateId="WRAP_NEXT_PREV" >
</@wrapSubContentCache >
-->
</table>
</div>
</div>
