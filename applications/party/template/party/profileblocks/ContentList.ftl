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

  <div id="partyContentList">
      <#if partyContent?has_content>
        <table class="basic-table" cellspacing="0">
          <#list partyContent as pContent>
            <#assign content = pContent.getRelatedOne("Content", false)>
            <#assign contentType = content.getRelatedOne("ContentType", true)>
            <#assign mimeType = content.getRelatedOne("MimeType", true)!>
            <#assign status = content.getRelatedOne("StatusItem", true)!>
            <#assign pcType = pContent.getRelatedOne("PartyContentType", false)>
            <tr>
              <td class="button-col"><a href="<@ofbizUrl>EditPartyContents?contentId=${pContent.contentId}&amp;partyId=${pContent.partyId}&amp;partyContentTypeId=${pContent.partyContentTypeId}&amp;fromDate=${pContent.fromDate}</@ofbizUrl>">${content.contentId}</a></td>
              <td>${(pcType.get("description", locale))!}</td>
              <td>${content.contentName!}</td>
              <td>${(contentType.get("description",locale))!}</td>
              <td>${(mimeType.description)!}</td>
              <td>${(status.get("description",locale))!}</td>
              <td>${pContent.fromDate!}</td>
              <td class="button-col">
                <#if (content.contentName?has_content)>
                  <a href="<@ofbizUrl>stream?contentId=${(content.contentId)!}</@ofbizUrl>" target="_blank">${uiLabelMap.CommonView}</a>
                </#if>
                <form name="removePartyContent_${pContent_index}" method="post" action="<@ofbizUrl>removePartyContent/viewprofile</@ofbizUrl>">
                  <input type="hidden" name="contentId" value="${pContent.contentId}" />
                  <input type="hidden" name="partyId" value="${pContent.partyId}" />
                  <input type="hidden" name="partyContentTypeId" value="${pContent.partyContentTypeId}" />
                  <input type="hidden" name="fromDate" value="${pContent.fromDate}" />
                  <input type="submit" value="${uiLabelMap.CommonRemove}"/>
                </form>
              </td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoContent}
      </#if>
  </div>