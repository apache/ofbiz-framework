<#--
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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
 * @author     Andy Zeneski
 * @created    July 12, 2002
 *@author     Olivier Heintz (olivier.heintz@nereide.biz) 
 * @version    1.0
 */ 
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
