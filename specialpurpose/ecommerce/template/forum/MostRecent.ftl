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
    function submitRows(rowCount) {
        var rowCountElement = document.createElement("input");
        rowCountElement.setAttribute("name", "_rowCount");
        rowCountElement.setAttribute("type", "hidden");
        rowCountElement.setAttribute("value", rowCount);
        document.forms.mostrecent.appendChild(rowCountElement);
        document.forms.mostrecent.submit();
    }
</script>

<table width="100%" border="0" >

 <form name="mostrecent" mode="post" action="<@ofbizUrl>publishResponse</@ofbizUrl>"/>
  <#assign row=0/>
  <#list entityList as content>
    <@checkPermission entityOperation="_ADMIN" targetOperation="CONTENT_PUBLISH" subContentId=forumId >
        <tr>
          <td> ${uiLabelMap.CommonId}:${content.contentId} </td>
          <td> ${uiLabelMap.CommonName}:${content.contentName} </td>
      <@injectNodeTrailCsv subContentId=content.contentId redo="true" contentAssocTypeId="PUBLISH_LINK">
          <td>
  <a class="tabButton" href="<@ofbizUrl>showforumresponse?contentId=${content.contentId}&nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" >${uiLabelMap.CommonView}</a>
          </td>
          <td>
          ${uiLabelMap.CommonSubmitted}:
          <input type="radio" name="statusId_o_${row}" value="CTNT_IN_PROGRESS" checked="checked" />
          </td>
          <td>
          ${uiLabelMap.CommonPublish}:
          <input type="radio" name="statusId_o_${row}" value="CTNT_PUBLISHED"/>
          </td>
        </tr>
          <input type="hidden" name="contentId_o_${row}" value="${content.contentId}"/>
        <tr>
          <td colspan="5">
          ${uiLabelMap.CommonContent}:<br />
            <@renderSubContentCache subContentId=content.contentId/>
          </td>
        </tr>
        
        <#assign row = row + 1/>
      </@injectNodeTrailCsv >
    </@checkPermission >
  </#list>
    <#if 0 < entityList?size >
        <tr>
          <td colspan="5">
<div class="smallSubmit" ><a href="javascript:submitRows('${row?default(0)}')">${uiLabelMap.CommonUpdate}</a></div>
          </td>
        </tr>
    </#if>
          <input type="hidden" name="forumId" value="${forumId}"/>
 </form>
</table>
