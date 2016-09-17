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

<div class="screenlet">
  <div style="margin:10px;">
  <#-- Do this so that we don't have to find the content twice (again in renderSubContent) -->
  <#assign subContentId=requestParameters.contentId!/>
  <#assign nodeTrailCsv=requestParameters.nodeTrailCsv!/>
  <#-- <#assign dummy=Static["org.apache.ofbiz.base.util.Debug"]
      .logInfo("in viewcontent, nodeTrailCsv:" + nodeTrailCsv, "")/> -->
  <#if ancestorList?has_content && (0 < ancestorList?size) >
    <#assign lastContent=ancestorList?last />
    <#assign firstContent=ancestorList[0] />
  </#if>
  <#if firstContent?has_content>
    <#assign siteId = firstContent.contentId/>
  </#if>
  <#if siteId?has_content>
    <@renderAncestryPath trail=ancestorList?default([]) endIndexOffset=1 siteId=siteId/>
  </#if>

  <#if lastContent?has_content>
    <h1>[${lastContent.contentId}] ${lastContent.description!}</h1>
  </#if>
  <#--
  <#assign globalNodeTrail=[]/>
  <#assign firstContentId=""/>
  <#if nodeTrailCsv?has_content>
    <#assign globalNodeTrail=Static["org.apache.ofbiz.base.util.StringUtil"].split(nodeTrailCsv, ",") />
    <#if 0 < globalNodeTrail?size>
      <#assign firstContentId=globalNodeTrail[0]?string/>
    </#if>
  </#if>
  <#assign globalNodeTrail=requestParameters.globalNodeTrail?default([])/>
  -->
  <#if globalNodeTrail?has_content && (0 < globalNodeTrail?size) >
    <#assign lastNode = globalNodeTrail?last/>
    <#if lastNode?has_content>
      <#assign subContent=lastNode.value/>
    </#if>
  <#else>
    <#assign subContent = delegator.findOne("Content",
        Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("contentId", subContentId), true)/>
  </#if>
  <#assign dummy=Static["org.apache.ofbiz.base.util.Debug"]
      .logInfo("in viewcontent, subContent:" + subContent, "")/>
    <br/>
    <h1>${uiLabelMap.EcommerceContentFor} [${subContentId}] ${subContent.contentName!}
      - ${subContent.description!}:</h1><br/>
    <table border="0" class="blogtext">
      <tr>
        <td width="40">&nbsp;</td>
        <td>
          <@renderSubContentCache subContentId=subContentId />
        </td>
        <td width="40" valign="bottom">
        <#--
        <@wrapSubContentCache subContentId=subContentId wrapTemplateId="WRAP_VIEW" >
        </@wrapSubContentCache >
        <@checkPermission mode="equals" entityOperation="_CREATE" targetOperation="HAS_USER_ROLE" >
          <a class="tabButton" href="<@ofbizUrl>createforumresponse?contentIdTo=${requestParameters.contentId}&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" >Respond</a>
        </@checkPermission>
        -->
          <br/>
        </td>
      </tr>
      <#assign thisContentId = subContentId/>
      <@loopSubContent contentId=thisContentId viewIndex=0 viewSize=9999 contentAssocTypeId="RELATED_CONTENT">
        <#assign thisNodeTrailCsv = nodeTrailCsv />
        <tr>
          <td colspan="3" align="right">
            <a class="tabButton"
               href="<@ofbizUrl>viewcontent?contentId=${subContentId}&amp;nodeTrailCsv=${thisNodeTrailCsv!}</@ofbizUrl>">${content.contentName!}</a>
          </td>
        </tr>
      </@loopSubContent>
    </table>
    <hr/>
  <#--
  <@checkPermission mode="not-equals" subContentId=subContentId
      targetOperation="CONTENT_CREATE|CONTENT_RESPOND" contentPurposeList="RESPONSE" >
    ${permissionErrorMsg!}
  </@checkPermission>
  -->

  </div>
</div>


<#macro renderAncestryPath trail siteId startIndex=0 endIndexOffset=0 buttonTitle="Back to" searchOn="" >
  <#local indent = "">
  <#local csv = "">
  <#local counter = 0>
  <#local len = trail?size>
  <table border="0" cellspacing="4">
    <#list trail as content>
      <#if counter < (len - endIndexOffset) && startIndex <= counter >
        <#if 0 < counter >
          <#local csv = csv + ","/>
        </#if>
        <#local csv = csv + content.contentId/>
        <#if counter < len && startIndex <= counter >
        <tr>
          <td>
            ${indent}
            <#if content.contentTypeId == "WEB_SITE_PUB_PT" >
              <a class="tabButton"
                  href="<@ofbizUrl>showcontenttree?contentId=${content.contentId!}&nodeTrailCsv=${csv}</@ofbizUrl>">
                ${uiLabelMap.CommonBackTo}
              </a>
              &nbsp;${content.contentName!}
            <#else>
              <a class="tabButton"
                  href="<@ofbizUrl>viewcontent?contentId=${content.contentId!}&nodeTrailCsv=${csv}</@ofbizUrl>">
                ${uiLabelMap.CommonBackTo}
              </a>
              &nbsp;${content.contentName!}
            </#if>
            <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
            [${content.contentId!}]
            <#if searchOn?has_content && searchOn?lower_case == "true">
              &nbsp;
              <a class="tabButton"
                 href="<@ofbizUrl>searchContent?siteId=${siteId!}&nodeTrailCsv=${csv}</@ofbizUrl>">
                ${uiLabelMap.CommonSearch}
              </a>
            </#if>
          </#if>
        </td>
      </tr>
      </#if>
      <#local counter = counter + 1>
      <#if 20 < counter > <#break/></#if>
    </#list>
  </table>
</#macro>
