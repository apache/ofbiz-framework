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

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <#if showMessageLinks?default("false")?upper_case == "TRUE">
                <a href="<@ofbizUrl>messagelist</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceViewList}</a>
            </#if>
        </div>
        <div class="boxhead">&nbsp;${pageHeader}</div>
    </div>
    <div class="screenlet-body">
      <form name="contactus" method="post" action="<@ofbizUrl>${submitRequest}</@ofbizUrl>" style="margin: 0;">
        <input type="hidden" name="partyIdFrom" value="${userLogin.partyId}"/>
        <input type="hidden" name="contactMechTypeId" value="WEB_ADDRESS"/>
        <input type="hidden" name="communicationEventTypeId" value="WEB_SITE_COMMUNICATI"/>
        <input type="hidden" name="note" value="${Static["org.ofbiz.base.util.UtilHttp"].getFullRequestUrl(request).toString()}"/>
        <#if message?has_content>
          <input type="hidden" name="parentCommEventId" value="${communicationEvent.communicationEventId}"/>
          <#if (communicationEvent.origCommEventId?exists && communicationEvent.origCommEventId?length > 0)>
            <#assign orgComm = communicationEvent.origCommEventId>
          <#else>
            <#assign orgComm = communicationEvent.communicationEventId>
          </#if>
          <input type="hidden" name="origCommEventId" value="${orgComm}"/>
        </#if>
        <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
          <tr>
            <td colspan="3">&nbsp;</td>
          </tr>
          <tr>
            <td width="5">&nbsp;</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.CommonFrom}:</div></td>
            <td><div class="tabletext">&nbsp;${sessionAttributes.autoName} [${userLogin.partyId}] (${uiLabelMap.CommonNotYou}?&nbsp;<a href="<@ofbizUrl>autoLogout</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClickHere}</a>)</div></td>
          </tr>
          <#if partyIdTo?has_content>
            <#assign partyToName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, partyIdTo, true)>
            <input type="hidden" name="partyIdTo" value="${partyIdTo}"/>
            <tr>
              <td colspan="3">&nbsp;</td>
            </tr>
            <tr>
              <td width="5">&nbsp;</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.CommonTo}:</div></td>
              <td><div class="tabletext">&nbsp;${partyToName}</div></td>
            </tr>
          </#if>
          <tr>
            <td colspan="3">&nbsp;</td>
          </tr>
          <#assign defaultSubject = (communicationEvent.subject)?default("")>
          <#if (defaultSubject?length == 0)>
            <#assign replyPrefix = "RE: ">
            <#if parentEvent?has_content>
              <#if !parentEvent.subject?default("")?upper_case?starts_with(replyPrefix)>
                <#assign defaultSubject = replyPrefix>
              </#if>
              <#assign defaultSubject = defaultSubject + parentEvent.subject?default("")>
            </#if>
          </#if>
          <tr>
            <td width="5">&nbsp;</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.EcommerceSubject}:</div></td>
            <td><input type="input" class="inputBox" name="subject" size="20" value="${defaultSubject}"/>
          </tr>
          <tr>
            <td colspan="3">&nbsp;</td>
          </tr>
          <tr>
            <td width="5">&nbsp;</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.CommonMessage}:</div></td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td colspan="2">
              <textarea name="content" class="textAreaBox" cols="40" rows="5"></textarea>
            </td>
          </tr>
          <tr>
            <td colspan="3">&nbsp;</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonSend}"/></td>
          </tr>
        </table>
      </form>
    </div>
</div>
