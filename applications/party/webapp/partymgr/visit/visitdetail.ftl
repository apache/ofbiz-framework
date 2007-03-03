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

<!-- begin visitdetail.ftl -->
  <h1>${uiLabelMap.PartyVisitDetail}</h1>
  <br/>

  <table class="basic-table" cellspacing="0">
    <tr>
      <td class="label">${uiLabelMap.PartyVisitIDSessionID}</td>
      <td>${visit.visitId?if_exists} / ${visit.sessionId?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyVisitorId}</td>
      <td>${visit.visitorId?default("${uiLabelMap.CommonNot} ${uiLabelMap.CommonFound}")}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyPartyIDUserLoginID}</td>
      <td><a href="<@ofbizUrl>viewprofile?partyId=${visit.partyId?if_exists}</@ofbizUrl>">${visit.partyId?if_exists}</a> / <a href="<@ofbizUrl>viewprofile?partyId=${visit.partyId?if_exists}</@ofbizUrl>">${visit.userLoginId?if_exists}</a></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyUserCreated}</td>
      <td>${visit.userCreated?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyWebApp}</td>
      <td>${visit.webappName?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyServer}</td>
      <td><a href="http://uptime.netcraft.com/up/graph/?site=${visit.serverIpAddress?if_exists}" target="_blank">${visit.serverIpAddress?if_exists}</a> / <a href="http://uptime.netcraft.com/up/graph/?site=${visit.serverIpAddress?if_exists}" target="_blank">${visit.serverHostName?if_exists}</a></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyClient}</td>
      <td><a href="http://ws.arin.net/cgi-bin/whois.pl?queryinput=${visit.clientIpAddress?if_exists}" target="_blank">${visit.clientIpAddress?if_exists}</a> / <a href="http://www.networksolutions.com/cgi-bin/whois/whois?STRING=${visit.clientHostName?if_exists}&SearchType=do" target="_blank">${visit.clientHostName?if_exists}</a></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyClientUser}</td>
      <td>${visit.clientUser?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyInitialLocale}</td>
      <td>${visit.initialLocale?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyInitialRequest}</td>
      <td><a href="${visit.initialRequest?if_exists}" >${visit.initialRequest?if_exists}</a></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyInitialReferer}</td>
      <td><a href="${visit.initialReferrer?if_exists}" >${visit.initialReferrer?if_exists}</a></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyInitialUserAgent}</td>
      <td>${visit.initialUserAgent?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyCookie}</td>
      <td>${visit.cookie?if_exists}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonFromDateThruDate}</td>
      <td>${(visit.fromDate?string)?if_exists} / ${(visit.thruDate?string)?default("["+uiLabelMap.PartyStillActive+"]")}</td>
    </tr>
  </table>

  <br/>
  <h1>${uiLabelMap.PartyHitTracker}</h1>

  <#if serverHits?has_content>
    <div class="align-float">
      <b>
        <#if 0 < viewIndex>
          <a href="<@ofbizUrl>visitdetail?visitId=${visitId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize>
          ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
        </#if>
        <#if highIndex < listSize>
          | <a href="<@ofbizUrl>visitdetail?visitId=${visitId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonNext}</a>
        </#if>
      </b>
    </div>
    <br class="clear" />
  </#if>

  <table class="basic-table" cellspacing="0">
    <tr class="header-row">
      <td>${uiLabelMap.PartyContentId}</td>
      <td>${uiLabelMap.PartyType}</td>
      <td>${uiLabelMap.PartySize}</td>
      <td>${uiLabelMap.PartyStartTime}</td>
      <td>${uiLabelMap.PartyTime}</td>
      <td>${uiLabelMap.PartyURI}</td>
    </tr>
    <#-- set initial row color -->
    <#assign rowClass = "2">
    <#list serverHits[lowIndex..highIndex-1] as hit>
      <#assign serverHitType = hit.getRelatedOne("ServerHitType")?if_exists>      
      <tr<#if rowClass == "1"> class="alternate-row"</#if>>
        <td>${hit.contentId?if_exists}</td>
        <td>${serverHitType.get("description",locale)?if_exists}</td>
        <td>&nbsp;&nbsp;${hit.numOfBytes?default("?")}</td>
        <td>${hit.hitStartDateTime?string?if_exists}</td>
        <td>${hit.runningTimeMillis?if_exists}</td>
        <td>
          <#assign url = (hit.requestUrl)?if_exists>
          <#if url?exists>
            <#assign len = url?length>
            <#if 45 < len>
              <#assign url = url[0..45] + "...">
            </#if>
          </#if>
          <a href="${hit.requestUrl?if_exists}" target="_blank">${url}</a>
        </td>
      </tr>
      <#-- toggle the row color -->
      <#if rowClass == "2">
        <#assign rowClass = "1">
      <#else>
        <#assign rowClass = "2">
      </#if>
    </#list>
  </table>

  <#if serverHits?has_content>
    <div class="align-float">
      <b>
        <#if 0 < viewIndex>
          <a href="<@ofbizUrl>visitdetail?visitId=${visitId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize>
          ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
        </#if>
        <#if highIndex < listSize>
          | <a href="<@ofbizUrl>visitdetail?visitId=${visitId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonNext}</a>
        </#if>
      </b>
    </div>
    <br class="clear" />
  </#if>

  <#if security.hasPermission("SEND_CONTROL_APPLET", session)>
    <br/>
    <h1>${uiLabelMap.PartyPagePushFollowing}</h1>
    <br/>

    <table border="0" cellpadding="5" cellspacing="5">
      <form name="pushPage" method="get" action="<@ofbizUrl>pushPage</@ofbizUrl>">
        <tr>
          <th>${uiLabelMap.PartyPushURL}</th>
          <td>
            <input type="hidden" name="followerSid" value="${visit.sessionId}">
            <input type="hidden" name="visitId" value="${visit.visitId}">
            <input type="input" name="pageUrl">
          </td>
          <td><input type="submit" value="${uiLabelMap.CommonSubmit}"></td>
        </tr>
        <tr>
          <td colspan="3"><hr/></td>
        </tr>
      </form>
      <form name="setFollower" method="get" action="<@ofbizUrl>setAppletFollower</@ofbizUrl>">
        <tr>
          <th>${uiLabelMap.PartyFollowSession}</th>
          <td>
            <input type="hidden" name="followerSid" value="${visit.sessionId}">
            <input type="hidden" name="visitId" value="${visit.visitId}">
            <input type="text" name="followSid">
          </td>
          <td><input type="submit" value="${uiLabelMap.CommonSubmit}"></td>
        </tr>
      </form>
    </table>
  </#if>
