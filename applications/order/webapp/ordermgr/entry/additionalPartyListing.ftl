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

<#-- ==================== Party Listing dialog box ========================= -->
<#if additionalPartyRoleMap?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyAdditionalPartyListing}</div>
    </div>
    <div class="screenlet-body">
      <table border="0" width="100%" cellpadding="0">
        <#list roleList as role>
          <tr>
            <td align="left" valign="bottom"><div class="tableheadtext">${roleData[role].get("description",locale)}</div></td>
          </tr>
          <tr>
            <td colspan="4"><hr class="sepbar"/></td>
          </tr>
          <#list additionalPartyRoleMap[role] as party>
            <tr>
              <td><div class="tabletext">${party}</div></td>
              <td>
                <div class="tabletext">
                  <#if partyData[party].type == "person">
                    ${partyData[party].firstName?if_exists}
                  <#else>
                    ${partyData[party].groupName?if_exists}
                  </#if>
                </div>
              </td>
              <td>
                <div class="tabletext">
                  <#if partyData[party].type == "person">
                    ${partyData[party].lastName?if_exists}
                  </#if>
                </div>
              </td>
              <td align="right">
                <a href="<@ofbizUrl>removeAdditionalParty?additionalRoleTypeId=${role}&additionalPartyId=${party}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
              </td>
            </tr>
          </#list>
          <tr><td>&nbsp;</td></tr>
        </#list>
      </table>
    </div>
</div>
</#if>
