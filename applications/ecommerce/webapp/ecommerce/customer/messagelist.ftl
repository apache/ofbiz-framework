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

<#macro showMessage communicationEvent isSentMessage>
  <#if communicationEvent.partyIdFrom?has_content>
    <#assign partyNameFrom = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdFrom, true)>
  <#else/>
    <#assign partyNameFrom = "${uiLabelMap.CommonNA}">
  </#if>
  <#if communicationEvent.partyIdTo?has_content>
    <#assign partyNameTo = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, communicationEvent.partyIdTo, true)>
  <#else/>
    <#assign partyNameTo = "${uiLabelMap.CommonNA}">
  </#if>
              <tr>
                <td><div class="tabletext">${partyNameFrom}</div></td>
                <td><div class="tabletext">${partyNameTo}</div></td>
                <td><div class="tabletext">${communicationEvent.subject?default("")}</div></td>
                <td><div class="tabletext">${communicationEvent.entryDate}</div></td>
                <td align="right">
                  <a href="<@ofbizUrl>readmessage?communicationEventId=${communicationEvent.communicationEventId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.EcommerceRead}]</a>
                  <#if isSentMessage>
                    <a href="<@ofbizUrl>newmessage?communicationEventId=${communicationEvent.communicationEventId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.PartyReply}]</a>
                  </#if>
                </td>
              </tr>
</#macro>

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <#if parameters.showSent?if_exists == "true">
              <a href="<@ofbizUrl>messagelist</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceViewReceivedOnly}</a>
            <#else>
              <a href="<@ofbizUrl>messagelist?showSent=true</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceViewSent}</a>
            </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.CommonMessages}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <#if (!receivedCommunicationEvents?has_content && !sentCommunicationEvents?has_content)>
            <tr><td><div class="tabletext">${uiLabelMap.EcommerceNoMessages}.</div></td></tr>
          <#else/>
            <tr>
              <td><div class="tableheadtext">${uiLabelMap.CommonFrom}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.CommonTo}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.EcommerceSubject}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.EcommerceSentDate}</div></td>
              <td>&nbsp;</td>
            </tr>
            <tr><td colspan="5"><hr class="sepbar"/></td></tr>
            <#list receivedCommunicationEvents?if_exists as receivedCommunicationEvent>
              <@showMessage communicationEvent=receivedCommunicationEvent isSentMessage=false/>
            </#list>
            <#list sentCommunicationEvents?if_exists as sentCommunicationEvent>
              <@showMessage communicationEvent=sentCommunicationEvent isSentMessage=true/>
            </#list>
          </#if>
        </table>
    </div>
</div>
