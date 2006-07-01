<#--
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
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
 *@author     Peter Goron (peter.goron@nereide.biz)
 *@version    $Rev$
 *@since      3.1
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
