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
<script type="text/javascript" language="javascript">
function call_fieldlookup4(rootForumId, parentForumId ) {
    var obj_lookupwindow = window.open("addSubSite?rootForumId=" + rootForumId + "&amp;parentForumId=" + parentForumId, 'FieldLookup', 'width=500,height=250,scrollbars=yes,status=no,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
</script>


<#if !rootForumId?has_content>
    <#assign rootForumId=requestParameters.rootForumId!/>
</#if>
<#if !rootForumId?has_content>
    <#assign rootForumId=defaultSiteId!/>
</#if>
<@checkPermission entityOperation="_ADMIN" targetOperation="CONTENT_ADMIN" >
<br />
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <form name="userform" method="post" action="<@ofbizUrl>CMSSites</@ofbizUrl>" >
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='appTitle'>
        <tr>
          <td colspan="1" valign="middle" align="right">
            <div class="boxhead">&nbsp; Root Site ID&nbsp;&nbsp; </div>
          </td>
          <td valign="middle">
            <div class="boxhead">
             <input type="text" name="rootForumId" size="20" value="${rootForumId!}"/>
            </div>
          </td>
          <td valign="middle" align="right">
            <a href="javascript:document.userform.submit()" class="submenutextright">Refresh</a>
          </td>
        </tr>
      </table>
      </form>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <form method="post" name="publishsite" action="<@ofbizUrl>linkContentToPubPt</@ofbizUrl>">
              <table width="100%" border="0" cellpadding="1">
                    <#assign rowCount = 0 />
                    <@showSites forumId=rootForumId />
              </table>
            </form>
          </td>
        </tr>
        <tr>
         <td >
            <a class="buttontext" href="<@ofbizUrl>addSubSite?rootForumId=${rootForumId}&amp;parentForumId=${rootForumId}</@ofbizUrl>" >Add Top Level Forum</a>
         </td >
        </tr>

      </table>
    </td>
  </tr>
<#if requestParameters.moderatedSiteId?has_content>
  <tr>
    <td width='100%'>
      <table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
         <tr><td><hr /></td></tr>
         <tr><td align="center"><h1>Unapproved entries for forum Id:${requestParameters.moderatedSiteId}</h1></td></tr>
         <tr><td><hr /></td></tr>
         <@moderateSite rootForumId=rootForumId forumId=requestParameters.moderatedSiteId />
      </table>
    </td>
  </tr>
</#if>
<#if requestParameters.permRoleSiteId?has_content>
  <tr>
    <td width='100%'>
      <table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
         <tr><td><hr /></td></tr>
         <tr><td align="center"><h1>Associated roles for forum Id:${requestParameters.permRoleSiteId}</h1></td></tr>
         <tr><td><hr /></td></tr>
         <@grantSiteRoles rootForumId=rootForumId forumId=requestParameters.permRoleSiteId/>
      </table>
    </td>
  </tr>
</#if>
</table>
</@checkPermission>

<#macro showSites forumId formAction="/enableSites"  indentIndex=0 catTrail=[]>

<#local thisContentId=catTrail[indentIndex]!/>

<#local indent = "">
<#if 0 < indentIndex >
  <#list 0..(indentIndex - 1) as idx>
      <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
  </#list>
</#if>


<@loopSubContent contentId=forumId viewIndex=0 viewSize=9999 contentAssocTypeId="SUBSITE" returnAfterPickWhen="1==1";>
       <tr>
         <td >
            ${indent}
            <#local plusMinus="-"/>
            ${plusMinus} ${content.contentName!}
         </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>CMSSites?rootForumId=${rootForumId}&amp;moderatedSiteId=${content.contentId}</@ofbizUrl>">Moderate</a>
         </td >
         <td >&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp; </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>CMSSites?rootForumId=${rootForumId}&amp;permRoleSiteId=${content.contentId}</@ofbizUrl>">User Roles</a>
         </td >
         <td >&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp; </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>addSubSite?rootForumId=${rootForumId}&amp;parentForumId=${content.contentId}</@ofbizUrl>" >Add Child Forum</a>
         </td >
         <td >&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp; </td >
         <td >
            <a class="buttontext" href="<@ofbizUrl>removeSite?rootForumId=${rootForumId}&amp;contentId=${content.contentId}&amp;contentIdTo=${forumId}&amp;contentAssocTypeId=SUBSITE</@ofbizUrl>">RemoveSite</a>
         </td >
       </tr>
       <#assign rowCount = rowCount + 1 />
       <@showSites forumId=subContentId indentIndex=(indentIndex + 1)/>
</@loopSubContent>

</#macro>


<#macro moderateSite forumId rootForumId >
<table width="100%" border="0" >
 <form name="mostrecent" method="post" action="<@ofbizUrl>publishResponse</@ofbizUrl>"/>
  <#assign row=0/>
  <#list mostRecentList as content>
    <@checkPermission entityOperation="_ADMIN" targetOperation="CONTENT_PUBLISH" subContentId=forumId >
        <tr>
          <td> <b>id:</b>${content.contentId} </td>
          <td> <b>name:</b>${content.contentName} </td>
      <@injectNodeTrailCsv subContentId=content.contentId redo="true" contentAssocTypeId="PUBLISH_LINK">
          <td>
  <a class="tabButton" href="<@ofbizUrl>CMSContentEdit?contentId=${content.contentId}&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" >View</a>
          </td>
          <td>
          <label><b>submitted:</b>
          <input type="radio" name="statusId_o_${row}" value="CTNT_FINAL_DRAFT" checked="checked"/>
          </label>
          </td>
          <td>
          <label><b>publish:</b>
          <input type="radio" name="statusId_o_${row}" value="CTNT_PUBLISHED"/>
          </label>
          </td>
          <td>
          <label><b>reject:</b>
          <input type="radio" name="statusId_o_${row}" value="CTNT_DEACTIVATED"/>
          </label>
          </td>
        </tr>
          <input type="hidden" name="contentId_o_${row}" value="${content.contentId}"/>
        <tr>
          <td colspan="6">
          <b>content:</b><br />
            <@renderSubContentCache subContentId=content.contentId/>
          </td>
        </tr>
        <tr> <td colspan="5"> <hr /> </td> </tr>
        <#assign row = row + 1/>
      </@injectNodeTrailCsv >
    </@checkPermission >
  </#list>
    <#if 0 < mostRecentList?size >
        <tr>
          <td colspan="5">
            <input type="submit" name="submitBtn" value="${uiLabelMap.CommonUpdate}"/>
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
  <tr>
    <td width='100%'>
      <form name="siteRoleForm" method="post" action="<@ofbizUrl>updateSiteRoles</@ofbizUrl>">
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
              <#assign cappedSiteRole= Static["org.apache.ofbiz.entity.model.ModelUtil"].dbNameToVarName(roleTypeId) />
              <td align="center">
              <input type="checkbox" name="${cappedSiteRole}_o_${rowCount}" value="Y" <#if siteRoleMap[cappedSiteRole]! == "Y">checked="checked"</#if>/>
              </td>
          <input type="hidden" name="${cappedSiteRole}FromDate_o_${rowCount}" value="${siteRoleMap[cappedSiteRole + "FromDate"]!}"/>
            </#list>
          </tr>
          <input type="hidden" name="contentId_o_${rowCount}" value="${forumId}"/>
          <input type="hidden" name="partyId_o_${rowCount}" value="${siteRoleMap.partyId}"/>
          <#assign rowCount=rowCount + 1/>
        </#list>
        <tr>
          <td valign="middle">
            <@htmlTemplate.lookupField formName="siteRoleForm" name="partyId_o_${rowCount}" id="partyId_o_${rowCount}" fieldFormName="LookupPerson"/><#-- FIXME check if should be changed -->
          </td>
            <#list blogRoleIdList as roleTypeId>
              <#assign cappedSiteRole= Static["org.apache.ofbiz.entity.model.ModelUtil"].dbNameToVarName(roleTypeId) />
              <td align="center">
              <input type="checkbox" name="${cappedSiteRole}_o_${rowCount}" value="Y" />
              </td>
            </#list>
            <input type="hidden" name="contentId_o_${rowCount}" value="${forumId}"/>
            <#assign rowCount=rowCount + 1/>
        </tr>
          <tr>
            <td>
            <input type="submit" name="submitBtn" value="${uiLabelMap.CommonUpdate}"/>
            </td>
          </tr>
      </table>
          <input type="hidden" name="_rowCount" value="${blogRoleIdList}"/>
      </form>
    </td>
  </tr>
</table>

<script type="text/javascript" language="javascript">
function call_fieldlookup3(view_name) {
        window.target = document.siteRoleForm.partyId_o_${rowCount - 1};
    var obj_lookupwindow = window.open(view_name,'FieldLookup', 'width=700,height=550,scrollbars=yes,status=no,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
</script>

</#macro>
