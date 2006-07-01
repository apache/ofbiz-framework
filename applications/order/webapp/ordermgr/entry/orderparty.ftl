<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.2
-->

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session)>
<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="center">
            <div class="boxhead">&nbsp;${uiLabelMap.PartyParty}</div>
          </td>               
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>          
          <td align="center">
            <#if person?has_content>
              <div class="tabletext"><a href="${customerDetailLink}${partyId}" class="buttontext">${person.firstName?if_exists}&nbsp;${person.lastName?if_exists}</a></div>
            <#elseif partyGroup?has_content>
                              <div class='tabletext'><a href="${customerDetailLink}${partyId}" class="buttontext">${partyGroup.groupName?if_exists}</a></div>
            </#if>
            <form method="post" action="<@ofbizUrl>orderentry</@ofbizUrl>" name="setpartyform">
              <div><input type="text" class="inputBox" name="partyId" size='10' value="${partyId?if_exists}"></div>
              <div class="tabletext">
                <a href="javascript:document.setpartyform.submit();" class="buttontext">${uiLabelMap.CommonSet}</a>&nbsp;|&nbsp;<a href="/partymgr/control/findparty" class="buttontext">${uiLabelMap.CommonFind}</a><#if partyId?default("_NA_") != "_NA_" && partyId?default("_NA_") != "">&nbsp;|&nbsp;<a href="${customerDetailLink}${partyId}" class="buttontext">${uiLabelMap.CommonView}</a></#if>
              </div>
            </form>
          </td>                        
        </tr>
      </table>
    </TD>
  </TR>
</TABLE>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
