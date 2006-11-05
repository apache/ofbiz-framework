<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#macro renderPublishLinks pubPtList contentId formAction="/updatePublishLinks" displayMode="view">
<#if displayMode=="edit">
<form mode="POST" name="form_publish" action="<@ofbizUrl>${formAction}</@ofbizUrl>" >
<input type="hidden" name="contentId" value="${contentId}"/>
</#if>
<table border="1">
<tr>
<#assign rowCounter = 0/>
<#list pubPtList as pubPt>
<th>${pubPt[0]}</th>
</#list>
</tr>

<#if displayMode=="view">
  <tr>
<#--
    <td>${map.contentName}<input type="hidden" name="${rowCounter}_contentId" value="${map.contentId}"/></td>
-->
    <#list pubPtList as pubPt>
        <td valign="top">
        <#assign subList = pubPt[1]/>
        <#list subList as arr>${arr[0]}<#assign subId=arr[0]/>
            <#assign subDate=arr[2]?if_exists/>
            <#if subDate?has_content><nobr/><b>*</b></#if>
            <br/>
        </#list>
        </td>
    </#list>
  </tr>
</#if>

<#if displayMode=="edit">
  <tr>
<#--
    <td>${map.contentName}<input type="hidden" name="${rowCounter}_contentId" value="${map.contentId}"/></td>
-->
    <#list pubPtList as pubPt>
        <#assign siteId=pubPt[0]/>
        <td>
        <#assign subList = pubPt[1]/>
        <select name="select_${siteId}">
        <#assign noneSelected = true/>
        <#list subList as arr>
            <#assign subId=arr[0]/>
            <#assign subDescription=arr[1]/>
            <#assign subDate=arr[2]?if_exists/>
            <option value="${subId}" <#if subDate?has_content> SELECTED <#assign noneSelected = false/></#if> >${subDescription?if_exists}</option>
        </#list>
        <option <#if noneSelected> SELECTED </#if> value="" >--</option>
        </select>
        </td>
    </#list>

  </tr>
</#if>
</table>
<#if displayMode=="edit">
<input type="submit" name="update" value="Update" />
<input type="hidden" name="permittedOperations" value="CONTENT_PUBLISH" />
</form>
</#if>
</#macro>


<#macro publishContent forumId contentId formAction="/updatePublishLinksMulti"  indentIndex=0 catTrail=[]>

<#local thisContentId=catTrail[indentIndex]?if_exists/>

<#assign viewIdx = "" />
<#if requestParameters.viewIndex?has_content>
<#assign viewIdx = requestParameters.viewIndex?if_exists?number />
</#if>
<#assign viewSz = "" />
<#if requestParameters.viewSize?has_content>
<#assign viewSz = requestParameters.viewSize?if_exists?number />
</#if>

<#local indent = "">
<#local thisCatTrailCsv = "" />
<#local listUpper = (indentIndex - 1) />
<#if catTrail?size < listUpper >
    <#local listUpper = (catTrail?size - 1)>
</#if>
<#if 0 < listUpper >
  <#list 0..listUpper as idx>
      <#if thisCatTrailCsv?has_content>
          <#local thisCatTrailCsv = thisCatTrailCsv + ","/>
      </#if>
      <#local thisCatTrailCsv = thisCatTrailCsv + catTrail[idx]>
  </#list>
</#if>
<#if 0 < indentIndex >
  <#list 0..(indentIndex - 1) as idx>
      <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
  </#list>
</#if>


<@loopSubContentCache subContentId=forumId
    viewIndex=viewIdx
    viewSize=viewSz
    contentAssocTypeId="SUBSITE"
    returnAfterPickWhen="1==1";
>
    <#local isPublished = "" />
    <#assign contentAssocViewFrom=Static["org.ofbiz.content.content.ContentWorker"].getContentAssocViewFrom(delegator, subContentId, contentId, "PUBLISH_LINK", null, null)?if_exists />
    <#if contentAssocViewFrom?has_content>
        <#local isPublished = "checked" />
    </#if>
       <tr>
         <td >
            ${indent}
            <#local plusMinus="-"/>
            ${plusMinus} ${content.contentName?if_exists}
         </td >
         <td  class="tabletext" >
            <input type="checkbox" name="publish_o_${rowCount}" value="Y" ${isPublished}/>
         </td >
            <input type="hidden" name="contentIdTo_o_${rowCount}" value="${subContentId}" />
            <input type="hidden" name="contentId_o_${rowCount}" value="${contentId}" />
            <input type="hidden" name="contentAssocTypeId_o_${rowCount}" value="PUBLISH_LINK" />
            <input type="hidden" name="statusId_o_${rowCount}" value="BLOG_SUBMITTED" />
       </tr>
       <#assign rowCount = rowCount + 1 />
       <@publishContent forumId=subContentId contentId=contentId indentIndex=(indentIndex + 1)/>
</@loopSubContentCache >

</#macro>


<#macro showSites forumId formAction="/enableSites"  indentIndex=0 catTrail=[]>

<#local thisContentId=catTrail[indentIndex]?if_exists/>

<#local indent = "">
<#if 0 < indentIndex >
  <#list 0..(indentIndex - 1) as idx>
      <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
  </#list>
</#if>


<@loopSubContentCache subContentId=forumId
    viewIndex=0
    viewSize=9999
    contentAssocTypeId="SUBSITE"
    returnAfterPickWhen="1==1";
>
       <tr>
         <td class="tabletext" >
            ${indent}
            <#local plusMinus="-"/>
            ${plusMinus} ${content.contentName?if_exists}
         </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>CMSSites?rootForumId=${rootForumId}&moderatedSiteId=${content.contentId}</@ofbizUrl>">Moderate</a>
         </td >
         <td >&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp; </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>CMSSites?rootForumId=${rootForumId}&permRoleSiteId=${content.contentId}</@ofbizUrl>">User Roles</a>
         </td >
         <td >&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp; </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>addSubSite?rootForumId=${rootForumId}&parentForumId=${content.contentId}</@ofbizUrl>" >Add Child Forum</a>
         </td >
         <td >&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp; </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>removeSite?rootForumId=${rootForumId}&contentId=${content.contentId}&contentIdTo=${forumId}&contentAssocTypeId=SUBSITE</@ofbizUrl>">RemoveSite</a>
         </td >
       </tr>
       <#assign rowCount = rowCount + 1 />
       <@showSites forumId=subContentId indentIndex=(indentIndex + 1)/>
</@loopSubContentCache >

</#macro>

<#macro moderateSite forumId rootForumId >
<table width="100%" border="0" >
 <form name="mostrecent" mode="POST" action="<@ofbizUrl>publishResponse</@ofbizUrl>"/>
  <#assign row=0/>
  <#list mostRecentList as content>
    <@checkPermission entityOperation="_ADMIN" targetOperation="CONTENT_PUBLISH" subContentId=forumId >
        <tr>
          <td class="tabletext"> <b>id:</b>${content.contentId} </td>
          <td class="tabletext"> <b>name:</b>${content.contentName} </td>
      <@injectNodeTrailCsv subContentId=content.contentId redo="true" contentAssocTypeId="PUBLISH_LINK">
          <td>
  <a class="tabButton" href="<@ofbizUrl>CMSContentEdit?contentId=${content.contentId}&nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" >View</a>
          </td>
          <td class="tabletext">
          <b>submitted:</b>
          <input type="radio" name="statusId_o_${row}" value="BLOG_SUBMITTED" checked/>
          </td>
          <td class="tabletext">
          <b>publish:</b>
          <input type="radio" name="statusId_o_${row}" value="BLOG_PUBLISHED"/>
          </td>
          <td class="tabletext">
          <b>reject:</b>
          <input type="radio" name="statusId_o_${row}" value="BLOG_REJECTED"/>
          </td>
        </tr>
          <input type="hidden" name="contentId_o_${row}" value="${content.contentId}"/>
        <tr>
          <td colspan="6" class="tabletext">
          <b>content:</b><br/>
            <@renderSubContentCache subContentId=content.contentId/>
          </td>
        </tr>
        <tr> <td colspan="5"> <hr/> </td> </tr>
        <#assign row = row + 1/>
      </@injectNodeTrailCsv >
    </@checkPermission >
  </#list>
    <#if 0 < mostRecentList?size >
        <tr>
          <td colspan="5">
            <input type="submit" name="submitBtn" value="Update"/>
          </td>
        </tr>
    </#if>
          <input type="hidden" name="moderatedSiteId" value="${forumId}"/>
          <input type="hidden" name="rootForumId" value="${rootForumId}"/>
          <input type="hidden" name="_rowCount" value="${mostRecentList?size}"/>
 </form>
</table>


</#macro>

<#macro grantSiteRoles forumId rootForumId >
<table width="100%" border="0" >
  <TR>
    <TD width='100%'>
      <form name="siteRoleForm" mode="POST" action="<@ofbizUrl>updateSiteRoles</@ofbizUrl>">
      <input type="hidden" name="permRoleSiteId" value="${forumId}"/>
      <input type="hidden" name="forumId" value="${forumId}"/>
      <input type="hidden" name="rootForumId" value="${rootForumId}"/>
      <table width='100%' border='0' cellspacing='0' cellpadding='4' class='boxoutside'>
        <tr>
            <td class="">User</td>
            <#list blogRoleIdList as roleTypeId>
              <td class="">${roleTypeId}</td>
            </#list>
        </tr>

      <#assign rowCount=0/>
        <#list siteList as siteRoleMap>
          <tr>
            <td class="">${siteRoleMap.partyId}</td>
            <#list blogRoleIdList as roleTypeId>
              <#assign cappedSiteRole= Static["org.ofbiz.entity.model.ModelUtil"].dbNameToVarName(roleTypeId) />
              <td align="center">
              <input type="checkbox" name="${cappedSiteRole}_o_${rowCount}" value="Y" <#if siteRoleMap[cappedSiteRole]?if_exists == "Y">checked</#if>/>
              </td>
          <input type="hidden" name="${cappedSiteRole}FromDate_o_${rowCount}" value="${siteRoleMap[cappedSiteRole + "FromDate"]?if_exists}"/>
            </#list>
          </tr>
          <input type="hidden" name="contentId_o_${rowCount}" value="${forumId}"/>
          <input type="hidden" name="partyId_o_${rowCount}" value="${siteRoleMap.partyId}"/>
          <#assign rowCount=rowCount + 1/>
        </#list>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead"><input type="text" name="partyId_o_${rowCount}" value=""/>
<a href="javascript:call_fieldlookup3('<@ofbizUrl>LookupPerson</@ofbizUrl>')"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"></a></div>
          </td>
            <#list blogRoleIdList as roleTypeId>
              <#assign cappedSiteRole= Static["org.ofbiz.entity.model.ModelUtil"].dbNameToVarName(roleTypeId) />
              <td align="center">
              <input type="checkbox" name="${cappedSiteRole}_o_${rowCount}" value="Y" />
              </td>
            </#list>
            <input type="hidden" name="contentId_o_${rowCount}" value="${forumId}"/>
            <#assign rowCount=rowCount + 1/>
        </tr>
          <tr>
            <td>
            <input type="submit" name="submitBtn" value="Update"/>
            </td>
          </tr>
      </table>
          <input type="hidden" name="_rowCount" value="${blogRoleIdList}"/>
      </form>
    </TD>
  </TR>
</table>

<SCRIPT language="javascript">
function call_fieldlookup3(view_name) {
        window.target = document.siteRoleForm.partyId_o_${rowCount - 1};
	var obj_lookupwindow = window.open(view_name,'FieldLookup', 'width=700,height=550,scrollbars=yes,status=no,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
	obj_lookupwindow.opener = window;
	obj_lookupwindow.focus();
}
</script>

</#macro>
