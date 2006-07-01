<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      3.1
-->

<#assign delegator = requestAttributes.delegator>
<#if communicationEvent.partyIdFrom?exists>
    <#assign fromName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdFrom, true)>
</#if>
<#if communicationEvent.partyIdTo?exists>
    <#assign toName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdTo, true)>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <#if (communicationEvent.partyIdFrom?if_exists != (userLogin.partyId)?if_exists)>
              <a href="<@ofbizUrl>newmessage?communicationEventId=${communicationEvent.communicationEventId}</@ofbizUrl>" class="submenutext">${uiLabelMap.PartyReply}</a>
            </#if>
            <a href="<@ofbizUrl>messagelist</@ofbizUrl>" class="submenutextright">${uiLabelMap.ViewList}</a>
        </div>
        <div class="boxhead">Read Message</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <tr><td>&nbsp;</td></tr>
          <tr>
              <td align="right"><div class="tableheadtext">${uiLabelMap.CommonFrom}:</div></td>
              <td><div class="tabletext">${fromName?if_exists}</div></td>
          </tr>
          <tr>
              <td align="right"><div class="tableheadtext">${uiLabelMap.CommonTo}:</div></td>
              <td><div class="tabletext">${toName?if_exists}</div></td>
          </tr>
          <tr>
              <td align="right"><div class="tableheadtext">${uiLabelMap.CommonDate}:</div></td>
              <td><div class="tabletext">${communicationEvent.entryDate}</div></td>
          </tr>
          <tr>
              <td align="right"><div class="tableheadtext">${uiLabelMap.EcommerceSubject}:</div></td>
              <td><div class="tabletext">&nbsp;${(communicationEvent.subject)?default("[${uiLabelMap.EcommerceNoSubject}]")}</div></td>
          </tr>
          <tr><td>&nbsp;</td></tr>
          <tr>
            <td>&nbsp;</td>
            <td>
              <div class="tabletext">${communicationEvent.content?default("[${uiLabelMap.EcommerceEmptyBody}]")}</div>
            </td>
          </tr>
        </table>
    </div>
</div>
