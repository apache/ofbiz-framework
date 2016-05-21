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

<#macro renderBlog contentId="" targetPurpose="" stdWrapId="">
<#if contentId?has_content>
    <#assign contentIdx = contentId/>
</#if>
<#assign viewIdx = "" />
<#if requestParameters.viewIndex?has_content>
<#assign viewIdx = requestParameters.viewIndex! />
</#if>
<#assign viewSz = "" />
<#if requestParameters.viewSize?has_content>
<#assign viewSz = requestParameters.viewSize! />
</#if>

<#assign sz=0/>
<table border="0">
<@loopSubContent contentId=contentIdx viewIndex=viewIdx viewSize=viewSz contentAssocTypeId="PUBLISH_LINK"
    pickWhen="purposes.contains(\"ARTICLE\") && \"CTNT_PUBLISHED\".equals(content.get(\"statusId\"))"
    returnAfterPickWhen="purposes.contains(\"ARTICLE\")"
    followWhen="contentAssocTypeId != null && contentAssocTypeId.equals(\"never follow\")">
  <#assign thisNodeTrailCsv=nodeTrailCsv!/>
  <#assign thisSubContentId=subContentId!/>
  <#assign thisNode=globalNodeTrail?last/>
  <#if thisNode?has_content>
  <#assign thisOwnerContentId=thisNode.value.ownerContentId!/>
  </#if>

  <#assign userLoginId=""/>
  <#if content?has_content && content.createdByUserLogin?has_content>
      <#assign userLoginId=content.createdByUserLogin/>
  </#if>
  <#assign authorName=Static["org.ofbiz.content.ContentManagementWorker"].getUserName(request,userLoginId!)/>

  <tr>
    <td width="40px">&nbsp;</td>
    <td class="blogtext" >
      <div>
        by:<#if authorName?has_content>${authorName!}
        <#else>
        <#if content?has_content>${content.createdByUserLogin!}</#if>
        </#if>
  &nbsp;
        <#if thisNode?? && thisNode.fromDate??>
          <#assign nowTime = thisNode.fromDate?string />
          <#assign shortTime = ""/>
          <#if nowTime?has_content>
              <#assign lastColon=nowTime?last_index_of(":") - 1/>
              <#assign shortTime=nowTime[0..lastColon]/>
          </#if>
          ${shortTime!}
        </#if>
      </div>
    </td>
    <td >
        <#if content?has_content>${content.contentName!}</#if>
        --
        <#if content?has_content>${content.description!}</#if>
    </td>
    <td width="40px" valign="bottom">
<a class="tabButton" href="<@ofbizUrl>showforumarticle?contentId=${thisSubContentId}&amp;nodeTrailCsv=${thisNodeTrailCsv!}&amp;forumId=${contentIdx!}</@ofbizUrl>" >${uiLabelMap.CommonView}</a>
    </td>
<@checkPermission mode="equals" entityOperation="_UPDATE" subContentId=content.contentId targetOperation="CONTENT_UPDATE" contentPurposeList="ARTICLE">
    <td width="40px" valign="bottom">
<a class="tabButton" style="height:14pt;" href="<@ofbizUrl>editforumarticle?contentIdTo=${content.contentId}&amp;nodeTrailCsv=${contentIdx!},${content.contentId}</@ofbizUrl>" >${uiLabelMap.CommonEdit}</a>
    </td>
</@checkPermission>
  </tr>
<#assign sz=listSize/>

</@loopSubContent>


<#if sz == 0 >
  <tr><td align="center">${uiLabelMap.CommonNoRecordFound}</td></tr>
</#if>
<@wrapSubContentCache subContentId=contentIdx wrapTemplateId=stdWrapId contentPurposeList="ARTICLE">
</@wrapSubContentCache>
</table>
<table border="0" class="summary">
<#assign targOp="HAS_USER_ROLE"/>
<#assign pageTargOp=targetOperation!/>
<#if pageTargOp?has_content>
    <#assign targOp=pageTargOp/>
</#if>
<@checkPermission mode="equals" entityOperation="_CREATE" subContentId=contentDept statusId="CTNT_PUBLISHED" targetOperation=targOp contentPurposeList="ARTICLE" quickCheckContentId=contentIdx>
<tr><td align="right">
<a class="tabButton" style="height:14pt;" href="<@ofbizUrl>createforumarticle?forumId=${contentIdx!}&amp;nodeTrailCsv=${contentIdx!}</@ofbizUrl>" >${uiLabelMap.ProductNewArticle}</a>
</td></tr>
</@checkPermission>
<@checkPermission mode="not-equals" entityOperation="_CREATE" subContentId=contentDept statusId="CTNT_PUBLISHED" targetOperation=targOp contentPurposeList="ARTICLE" quickCheckContentId=contentIdx>
<tr><td align="right">
${uiLabelMap.EcommerceLoggedToPost}
</td></tr>
</@checkPermission>
</table>
<#--
<@checkPermission mode="not-equals" entityOperation="_CREATE" subContentId=contentIdx statusId="CTNT_PUBLISHED" targetOperation="HAS_USER_ROLE" contentPurposeList="ARTICLE">
            ${permissionErrorMsg!}
</@checkPermission>
-->

</#macro>

<#macro renderAncestryPath trail startIndex=0 endIndexOffset=0 buttonTitle="${uiLabelMap.CommonBackTo}">
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
         <td >
            ${indent}
            <#if content.contentTypeId == "WEB_SITE_PUB_PT" >
              <a class="tabButton" href="<@ofbizUrl>showforum?forumId=${content.contentId!}&amp;nodeTrailCsv=${csv}</@ofbizUrl>" >${uiLabelMap.CommonBackTo}</a> &nbsp;${content.contentName!}
            <#else>
              <a class="tabButton" href="<@ofbizUrl>showforumarticle?contentId=${content.contentId!}&amp;nodeTrailCsv=${csv}</@ofbizUrl>" >${uiLabelMap.CommonBackTo}to</a> &nbsp;${content.contentName!}
            </#if>
            <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
            [${content.contentId!}]</td>
        </#if>
       </tr>
      </#if>
      <#local counter = counter + 1>
    <#if 20 < counter > <#break/></#if>
    </#list>
    </table>
</#macro>

<#macro nextPrev listSize requestURL queryString lowIndex=0 highIndex=10 viewSize=10 viewIndex=0 >

<#assign lowIdx=lowIndex/>
<#assign highIdx=highIndex/>
<#assign viewSz=viewSize/>
<#assign viewIdx=viewIndex/>
<#assign listSz=listSize/>

<#if !lowIdx?has_content><#assign lowIdx=0/></#if>
<#if !highIdx?has_content><#assign highIdx=0/></#if>
<#if !viewSz?has_content><#assign viewSz=10/></#if>
<#if !viewIdx?has_content><#assign viewIdx=10/></#if>
<#if !listSz?has_content><#assign listSz=0/></#if>

<#if queryString?has_content>
    <#assign queryString = Static["org.ofbiz.base.util.UtilHttp"].stripViewParamsFromQueryString(queryString)/>
</#if>

<#assign lowIdxShow = lowIdx + 1 />
<#if highIdx < lowIdxShow >
  <#assign lowIdxShow = highIdx/>
</#if>
<table border="0" >
<tr><td>
             <#if 0 < listSz?number>
                <#if 0 < viewIdx?number>
                  <a href="${requestURL}?${queryString}&amp;viewSz=${viewSz}&amp;viewIdx=${viewIdx?number-1}" class="submenutext">${uiLabelMap.CommonPrevious}</a>
                <#else>
                  <span class="submenutextdisabled">${uiLabelMap.CommonPrevious}</span>
                </#if>
                <#if 0 < listSz>
                  <span class="submenutextinfo">${lowIdxShow} - ${highIdx!} ${uiLabelMap.CommonOf} ${listSz!}</span>
                </#if>
                <#if highIdx!?number < listSz!?number>
                  <a href="${requestURL}?${queryString!}&amp;viewSz=${viewSz!}&amp;viewIdx=${viewIdx!?number+1}" class="submenutextright">${uiLabelMap.CommonNext}</a>
                <#else>
                  <span class="submenutextrightdisabled">${uiLabelMap.CommonNext}</span>
                </#if>
              </#if>
</td></tr>
    </table>
</#macro>
