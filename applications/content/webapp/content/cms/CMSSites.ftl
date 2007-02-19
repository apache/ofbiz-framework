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
<SCRIPT language="javascript">
function call_fieldlookup4(rootForumId, parentForumId ) {
	var obj_lookupwindow = window.open("addSubSite?rootForumId=" + rootForumId + "&parentForumId=" + parentForumId, 'FieldLookup', 'width=500,height=250,scrollbars=yes,status=no,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
	obj_lookupwindow.opener = window;
	obj_lookupwindow.focus();
}
</script>


<#--
<#include "publishlib.ftl" />
-->
<#if !rootForumId?has_content>
    <#assign rootForumId=requestParameters.rootForumId?if_exists/>
</#if>
<#if !rootForumId?has_content>
    <#assign rootForumId=defaultSiteId?if_exists/>
</#if>
<@checkPermission entityOperation="_ADMIN" targetOperation="CONTENT_ADMIN" >
<br/>
<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <TR>
    <TD width='100%'>
      <form name="userform" mode="POST" action="<@ofbizUrl>CMSSites</@ofbizUrl>" >
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='appTitle'>
        <tr>
          <td colspan="1" valign="middle" align="right">
            <div class="boxhead">&nbsp; Root Site ID&nbsp;&nbsp; </div>
          </td>
          <td valign="middle" align="left">
            <div class="boxhead">
             <input type="text" name="rootForumId" size="20" value="${rootForumId?if_exists}">
            </div>
          </td>
          <td valign="middle" align="right">
            <a href="javascript:document.userform.submit()" class="submenutextright">Refresh</a>
          </td>
        </tr>
      <table>
      </form>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <form mode="POST" name="publishsite" action="<@ofbizUrl>linkContentToPubPt</@ofbizUrl>">
              <table width="100%" border="0" cellpadding="1">
                    <#assign rowCount = 0 />
                    <@showSites forumId=rootForumId />
              </table>
            </form>
          </td>
        </tr>
        <tr>
         <td >
            <a class="buttontext" href="<@ofbizUrl>addSubSite?rootForumId=${rootForumId}&parentForumId=${rootForumId}</@ofbizUrl>" >Add Top Level Forum</a>
         </td >
        </tr>

      </table>
    </TD>
  </TR>
<#if requestParameters.moderatedSiteId?has_content>
  <TR>
    <TD width='100%'>
      <TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
         <tr><td><hr/></td></tr>
         <tr><td align="center"><div class="head1">Unapproved entries for forum Id:${requestParameters.moderatedSiteId}</div></td></tr>
         <tr><td><hr/></td></tr>
         <@moderateSite rootForumId=rootForumId forumId=requestParameters.moderatedSiteId />
      </TABLE>
    </TD>
  </TR>
</#if>
<#if requestParameters.permRoleSiteId?has_content>
  <TR>
    <TD width='100%'>
      <TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
         <tr><td><hr/></td></tr>
         <tr><td align="center"><div class="head1">Associated roles for forum Id:${requestParameters.permRoleSiteId}</div></td></tr>
         <tr><td><hr/></td></tr>
         <@grantSiteRoles rootForumId=rootForumId forumId=requestParameters.permRoleSiteId/>
      </TABLE>
    </TD>
  </TR>
</#if>
</TABLE>
</@checkPermission>

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
          <input type="radio" name="statusId_o_${row}" value="CTNT_FINAL_DRAFT" checked/>
          </td>
          <td class="tabletext">
          <b>publish:</b>
          <input type="radio" name="statusId_o_${row}" value="CTNT_PUBLISHED"/>
          </td>
          <td class="tabletext">
          <b>reject:</b>
          <input type="radio" name="statusId_o_${row}" value="CTNT_DEACTIVATED"/>
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
