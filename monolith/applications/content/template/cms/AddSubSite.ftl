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

<form class="basic-form" name="addSubSite" method="post" action="<@ofbizUrl>postNewSubSite?rootForumId=${requestParameters.rootForumId}</@ofbizUrl>">
<table>
  <tr>
    <td class="label"><label>Site Name:</label>/td>
    <td><input type="text" size="20" name="contentName"/></td>
  </tr>
  <tr>
    <td class="label"><label>Site Description:</label></td>
    <td><input type="text" size="40" name="description"/></td>
  </tr>
  <tr>
    <td class="label"><label>Posted Msg Default Status:</label></td>
    <td>
      <select name="statusId">
        <option value="CTNT_IN_PROGRESS">Draft - not attached to any site</option>
        <option value="CTNT_FINAL_DRAFT">Final Draft - but must be approve (moderated)</option>
        <option value="CTNT_PUBLISHED">Publish immediately</option>
      </select>
    </td>
  </tr>
  <tr>
    <td class="label"/>
    <td><input type="submit" name="submitBtn" value="Create"/></td>
  </tr>
</table>
<input type="hidden" name="contentIdTo" value="${requestParameters.parentForumId}" />
<input type="hidden" name="ownerContentId" value="${requestParameters.parentForumId}" />
<input type="hidden" name="contentTypeId" value="WEB_SITE_PUB_PT" />
<input type="hidden" name="contentAssocTypeId" value="SUBSITE" />

</form>
