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

<#-- Party IDs -->
<#assign partyId = requestParameters.partyId?if_exists>
<#assign partyIdTo = requestParameters.partyIdTo?if_exists>

<br/>
<#if hasUpdatePermission>

<TABLE border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">${uiLabelMap.PartyLink}</div>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width="100%" >
      <center>
      <table border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <#if partyTo?has_content && partyFrom?has_content>
          <form name="linkparty" method="post" action="<@ofbizUrl>setPartyLink</@ofbizUrl>">
          <tr>
            <td colspan="2" align="center">
              <div class="head1">
                <font color="red">
                    ${uiLabelMap.PartyLinkMessage1}
                </font>
              </div>
            </td>
          </tr>
          <tr><td colspan="2">&nbsp;</td></tr>
          <tr>
            <td align="right"><span class="tabletext">&nbsp;${uiLabelMap.PartyLink}:&nbsp;</span></td>
            <td>
              <input type="hidden" name="partyId" value="${partyFrom.partyId}"/>
              <span class="tabletext">
                  <#if personFrom?has_content>
                    ${personFrom.lastName}, ${personFrom.firstName}
                  <#elseif groupFrom?has_content>
                    ${groupFrom.groupName}
                  <#else>
                    [${uiLabelMap.PartyUnknown}]
                  </#if>
                  &nbsp;<b>[${partyFrom.partyId}]</b>
              </span>
            </td>
          </tr>
          <tr>
            <td align="right"><span class="tabletext">&nbsp;${uiLabelMap.CommonTo}:&nbsp;</span></td>
            <td>
              <input type="hidden" name="partyIdTo" value="${partyTo.partyId}"/>
              <span class="tabletext">
                  <#if personTo?has_content>
                    ${personTo.lastName}, ${personTo.firstName}
                  <#elseif groupTo?has_content>
                    ${groupTo.groupName}
                  <#else>
                    [${uiLabelMap.PartyUnknown}]
                  </#if>
                  &nbsp;<b>[${partyTo.partyId}]</b>
              </span>
            </td>
          </tr>
          <tr><td colspan="2">&nbsp;</td></tr>
          <tr>
            <td colspan="2" align="center">
              <a href="javascript:document.linkparty.submit()" class="buttontext">${uiLabelMap.CommonConfirm}</a>&nbsp;&nbsp;
            </td>
        </tr>
        </form>
        <#else>
          <form name="linkpartycnf" method="post" action="<@ofbizUrl>linkparty</@ofbizUrl>">
          <tr>
            <td><span class="tabletext">&nbsp;${uiLabelMap.PartyLink}:&nbsp;</span></td>
            <td><input type="text" class="inputBox" name="partyId" value="${partyId?if_exists}"></td>
            <td><span class="tabletext">&nbsp;${uiLabelMap.CommonTo}:&nbsp;</span></td>
            <td><input type="text" class="inputBox" name="partyIdTo" value="${partyIdTo?if_exists}"></td>
            <td>
              <a href="javascript:document.linkpartycnf.submit()" class="buttontext">${uiLabelMap.CommonLink}</a>&nbsp;&nbsp;
            </td>
          </tr>
          </form>
        </#if>
      </table>
      </center>
    </TD>
  </TR>

</TABLE>
</#if>
