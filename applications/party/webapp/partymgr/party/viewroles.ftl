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

<#-- Party Roles -->
<br/>
<TABLE border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">${uiLabelMap.PartyMemberRoles}</div>
          </td>
        </tr>
      </table>
    </TD>
    </form>
  </TR>
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td>
            <#if partyRoles?has_content>
            <table width="100%" border="0" cellpadding="1">
              <#list partyRoles as userRole>
              <tr>
                <td align="right" valign="top" width="10%" nowrap><div class="tabletext"><b>${uiLabelMap.PartyRole}</b></div></td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="70%"><div class="tabletext">${userRole.get("description",locale)} [${userRole.roleTypeId}]</div></td>
                <#if hasDeletePermission>
                <td align="right" valign="top" width="20%">
                  <a href="<@ofbizUrl>deleterole?partyId=${partyId}&roleTypeId=${userRole.roleTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>&nbsp;
                </td>
                </#if>
              </tr>
              </#list>
            </table>
            <#else>
              <div class="tabletext">${uiLabelMap.PartyNoPartyRolesFound}</div>
            </#if>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <#if hasUpdatePermission>
  <TR>
    <TD width="100%"><hr class="sepbar"></TD>
  </TR>
  <TR>
    <TD width="100%" >
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <form name="addPartyRole" method="post" action="<@ofbizUrl>addrole/viewroles</@ofbizUrl>">
        <input type="hidden" name="partyId" value="${partyId}">
        <tr>
          <td align="right" width="75%"><span class="tabletext">&nbsp;${uiLabelMap.PartyAddToRole}:&nbsp;</span></td>
          <td>
            <select name="roleTypeId" class="selectBox">
              <#list roles as role>
                <option value="${role.roleTypeId}">${role.get("description",locale)}</option>
              </#list>
            </select>
          </td>
          <td>
            <a href="javascript:document.addPartyRole.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a>&nbsp;&nbsp;
          </td>
        </tr>
        </form>
      </table>
    </TD>
  </TR>
  </#if>
</TABLE>

<#-- Add role type -->
<#if hasCreatePermission>
<br/>
<TABLE border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">${uiLabelMap.PartyNewRoleType}</div>
          </td>
        </tr>
      </table>
    </TD>
    </form>
  </TR>
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="1" cellpadding="1" class="boxbottom">
        <form method="post" action="<@ofbizUrl>createroletype</@ofbizUrl>" name="createroleform">
        <input type='hidden' name='party_id' value='${partyId}'>
        <input type='hidden' name='partyId' value='${partyId}'>
        <tr>
          <td width="16%"><div class="tabletext">${uiLabelMap.PartyRoleTypeId}</div></td>
          <td width="84%">
            <input type="text" name="roleTypeId" size="20" class="inputBox">*
          </td>
        <tr>
          <td width="16%"><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
          <td width="84%">
            <input type="text" name="description" size="30" class="inputBox">*
            &nbsp;&nbsp;<a href="javascript:document.createroleform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
          </td>
        </tr>
        </form>
      </table>
    </TD>
  </TR>
</TABLE>
</#if>
