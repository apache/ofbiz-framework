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
  <div id="partyContent" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.PartyContent}</li>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
          ${screens.render("component://party/widget/partymgr/ProfileScreens.xml#ContentList")}
      <hr />
      <div class="label">${uiLabelMap.PartyAttachContent}</div>
      <form id="uploadPartyContent" method="post" enctype="multipart/form-data" action="<@ofbizUrl>uploadPartyContent</@ofbizUrl>">
        <input type="hidden" name="dataCategoryId" value="PERSONAL"/>
        <input type="hidden" name="contentTypeId" value="DOCUMENT"/>
        <input type="hidden" name="statusId" value="CTNT_PUBLISHED"/>
        <input type="hidden" name="partyId" value="${partyId}" id="contentPartyId"/>
        <input type="file" name="uploadedFile" class="required error" size="25"/>
        <div>
        <select name="partyContentTypeId" class="required error">
          <option value="">${uiLabelMap.PartySelectPurpose}</option>
          <#list partyContentTypes as partyContentType>
            <option value="${partyContentType.partyContentTypeId}">${partyContentType.get("description", locale)?default(partyContentType.partyContentTypeId)}</option>
          </#list>
        </select>
        </div>
        <div class="label">${uiLabelMap.PartyIsPublic}</div>
        <select name="isPublic">
            <option value="N">${uiLabelMap.CommonNo}</option>
            <option value="Y">${uiLabelMap.CommonYes}</option>
        </select>
        <select name="roleTypeId">
          <option value="">${uiLabelMap.PartySelectRole}</option>
          <#list roles as role>
            <option value="${role.roleTypeId}" <#if "_NA_" == role.roleTypeId>selected="selected"</#if>>${role.get("description", locale)?default(role.roleTypeId)}</option>
          </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonUpload}" />
      </form>
    </div>
  </div>