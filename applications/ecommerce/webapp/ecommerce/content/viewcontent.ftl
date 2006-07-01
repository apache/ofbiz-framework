<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Al Byers (byersa@automationgroups.com)
 *@version    $Rev$
 *@since      3.1
-->

<div class="screenlet">
<div style="margin:10px;">
<#-- Do this so that we don't have to find the content twice (again in renderSubContent) -->
<#assign subContentId=requestParameters.contentId?if_exists/>
<#assign nodeTrailCsv=requestParameters.nodeTrailCsv?if_exists/>
<#-- <#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in viewcontent, nodeTrailCsv:" + nodeTrailCsv, "")/> -->
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
    <div class="head1">[${lastContent.contentId}] ${lastContent.description}</div>
</#if>
<#--
<#assign globalNodeTrail=[]/>
<#assign firstContentId=""/>
<#if nodeTrailCsv?has_content>
  <#assign globalNodeTrail=Static["org.ofbiz.base.util.StringUtil"].split(nodeTrailCsv, ",") />
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
    <#assign subContent = delegator.findByPrimaryKeyCache("Content", Static["org.ofbiz.base.util.UtilMisc"].toMap("contentId", subContentId))/>
</#if>
<#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in viewcontent, subContent:" + subContent, "")/>
<br/>
<div class="head1">${uiLabelMap.EcommerceContentFor} [${subContentId}] ${subContent.contentName?if_exists} - ${subContent.description?if_exists}:</div><br/>
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
    <a class="tabButton" href="<@ofbizUrl>createforumresponse?contentIdTo=${requestParameters.contentId}&amp;nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" >Respond</a>
</@checkPermission>
-->
<br/>

    </td>
    </tr>
    <#assign thisContentId = subContentId/>
    <@loopSubContentCache subContentId=thisContentId viewIndex=0 viewSize=9999 contentAssocTypeId="RELATED_CONTENT">
      <#assign thisNodeTrailCsv = nodeTrailCsv />
      <tr>
        <td colspan="3" align="right">
          <a class="tabButton" href="<@ofbizUrl>viewcontent?contentId=${subContentId}&amp;nodeTrailCsv=${thisNodeTrailCsv?if_exists}</@ofbizUrl>" >${content.contentName?if_exists}</a>
        </td>
      </tr>
    </@loopSubContentCache>
</table>
<hr/>
<#--
<@checkPermission mode="not-equals" subContentId=subContentId targetOperation="CONTENT_CREATE|CONTENT_RESPOND" contentPurposeList="RESPONSE" >
            ${permissionErrorMsg?if_exists}
</@checkPermission>
-->

</div>
</div>


<#macro renderAncestryPath trail siteId startIndex=0 endIndexOffset=0 buttonTitle="Back to" searchOn="" >
    <#local indent = "">
    <#local csv = "">
    <#local counter = 0>
    <#local len = trail?size>
    <table border="0" class="tabletext" cellspacing="4">
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
              <a class="tabButton" href="<@ofbizUrl>showcontenttree?contentId=${content.contentId?if_exists}&nodeTrailCsv=${csv}</@ofbizUrl>" >${uiLabelMap.CommonBackto}</a> &nbsp;${content.contentName?if_exists}
            <#else>
              <a class="tabButton" href="<@ofbizUrl>viewcontent?contentId=${content.contentId?if_exists}&nodeTrailCsv=${csv}</@ofbizUrl>" >${uiLabelMap.CommonBackto}</a> &nbsp;${content.contentName?if_exists}
            </#if>
            <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
            [${content.contentId?if_exists}]
            <#if searchOn?has_content && searchOn?lower_case == "true">
                &nbsp;
              <a class="tabButton" href="<@ofbizUrl>searchContent?siteId=${siteId?if_exists}&nodeTrailCsv=${csv}</@ofbizUrl>" >${uiLabelMap.CommonSearch}</a> 
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
