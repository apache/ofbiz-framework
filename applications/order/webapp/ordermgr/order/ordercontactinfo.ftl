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

<#macro partyPostalAddress postalContactMechList contactMechPurposeTypeId contactPostalAddress>
   <select name="contactMechId" class="selectBox">
      <option value="${contactPostalAddress.contactMechId}">${(contactPostalAddress.address1)?default("")} - ${contactPostalAddress.city?default("")}</option>
      <option value="${contactPostalAddress.contactMechId}"></option>
      <#list postalContactMechList as postalContactMech>
         <#assign postalAddress = postalContactMech.getRelatedOne("PostalAddress")?if_exists>
         <#assign partyContactPurposes = postalAddress.getRelated("PartyContactMechPurpose")?if_exists>
         <#list partyContactPurposes as partyContactPurpose>
         <#if postalContactMech.contactMechId?has_content && partyContactPurpose.contactMechPurposeTypeId == contactMechPurposeTypeId>
            <option value="${postalContactMech.contactMechId?if_exists}">${(postalAddress.address1)?default("")} - ${postalAddress.city?default("")}</option>
         </#if>
         </#list>
      </#list>
   </select>
</#macro>    

<#if displayParty?has_content || orderContactMechValueMaps?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderContactInformation}</div>
    </div>
    <div class="screenlet-body">
      <table width="100%" border="0" cellpadding="1" cellspacing="0">
        <tr>
          <td align="right" valign="top" width="15%">
            <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonName}</b></div>
          </td>
          <td width="5">&nbsp;</td>
          <td align="left" valign="top" width="80%">
            <div class="tabletext">
              <#if displayParty?has_content>
                <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                ${displayPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
              </#if>
              <#if partyId?exists>
                <span>&nbsp;(<a href="${customerDetailLink}${partyId}" target="partymgr" class="buttontext">${partyId}</a>)</span>
                <span>
                   <a href="<@ofbizUrl>/orderentry?partyId=${partyId}&amp;orderTypeId=${orderHeader.orderTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNewOrder}</a>
                   <a href="<@ofbizUrl>/findorders?lookupFlag=Y&amp;hideFields=Y&amp;partyId=${partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderOtherOrders}</a>
                </span>
              </#if>
            </div>
          </td>
        </tr>
        <#list orderContactMechValueMaps as orderContactMechValueMap>
          <#assign contactMech = orderContactMechValueMap.contactMech>
          <#assign contactMechPurpose = orderContactMechValueMap.contactMechPurposeType>
          <#--<#assign partyContactMech = orderContactMechValueMap.partyContactMech>-->
          <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <div class="tabletext">&nbsp;<b>${contactMechPurpose.get("description",locale)}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
                <#assign postalAddress = orderContactMechValueMap.postalAddress>
                <#if postalAddress?has_content>
                  <div class="tabletext">
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}<br/></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${postalAddress.attnName}<br/></#if>
                    ${postalAddress.address1}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if>
                    ${postalAddress.postalCode?if_exists}<br/>
                    ${postalAddress.countryGeoId?if_exists}<br/>
                    <#if !postalAddress.countryGeoId?exists || postalAddress.countryGeoId == "USA">
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") > 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target="_blank" href="http://www.whitepages.com/find_person_results.pl?fid=a&amp;s_n=${addressNum}&amp;s_a=${addressOther}&amp;c=${postalAddress.city?if_exists}&amp;s=${postalAddress.stateProvinceGeoId?if_exists}&amp;x=29&amp;y=18" class="buttontext">(lookup:whitepages.com)</a>
                      </#if>
                    </#if>
                  </div>
                  <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                  <form name="updateOrderContactMech" method="post" action="<@ofbizUrl>updateOrderContactMech</@ofbizUrl>">
                     <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                     <input type="hidden" name="contactMechPurposeTypeId" value="${contactMechPurpose.contactMechPurposeTypeId?if_exists}"/>
                     <input type="hidden" name="oldContactMechId" value="${contactMech.contactMechId?if_exists}"/>
                     <hr class="sepbar"/>      
                     <div><@partyPostalAddress postalContactMechList = postalContactMechList?if_exists contactMechPurposeTypeId = contactMechPurpose.contactMechPurposeTypeId?if_exists contactPostalAddress=postalAddress?if_exists/><input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/></div>
                  </form> 
                  </#if>
                </#if>
              <#elseif contactMech.contactMechTypeId == "TELECOM_NUMBER">
                <#assign telecomNumber = orderContactMechValueMap.telecomNumber>
                <div class="tabletext">
                  ${telecomNumber.countryCode?if_exists}
                  <#if telecomNumber.areaCode?exists>${telecomNumber.areaCode}-</#if>${telecomNumber.contactNumber}
                  <#--<#if partyContactMech.extension?exists>ext&nbsp;${partyContactMech.extension}</#if>-->
                  <#if !telecomNumber.countryCode?exists || telecomNumber.countryCode == "011" || telecomNumber.countryCode == "1">
                    <a target="_blank" href="http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&amp;telephone=${telecomNumber.contactNumber?if_exists}&amp;btnsubmit.x=20&amp;btnsubmit.y=8" class="buttontext">(lookup:anywho.com)</a>
                   <a target="_blank" href="http://whitepages.com/find_person_results.pl?fid=p&amp;ac=${telecomNumber.areaCode?if_exists}&amp;s=&amp;p=${telecomNumber.contactNumber?if_exists}&amp;pt=b&amp;x=40&amp;y=9" class="buttontext">(lookup:whitepages.com)</a>
                  </#if>
                </div>
              <#elseif contactMech.contactMechTypeId == "EMAIL_ADDRESS">
                <div class="tabletext">
                  ${contactMech.infoString}
                  <#if security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>
                     <br/>(<a href="<@ofbizUrl>confirmationmailedit?orderId=${orderId}&amp;partyId=${partyId}&amp;sendTo=${contactMech.infoString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderSendConfirmationEmail}</a>)
                  <#else>
                     <a href="mailto:${contactMech.infoString}" class="buttontext">(${uiLabelMap.OrderSendEmail})</a>
                  </#if>
                </div>
              <#elseif contactMech.contactMechTypeId == "WEB_ADDRESS">
                <div class="tabletext">
                  ${contactMech.infoString}
                  <#assign openString = contactMech.infoString>
                  <#if !openString?starts_with("http") && !openString?starts_with("HTTP")>
                    <#assign openString = "http://" + openString>
                  </#if>
                  <a target="_blank" href="${openString}" class="buttontext">(open&nbsp;page&nbsp;in&nbsp;new&nbsp;window)</a>
                </div>
              <#else>
                <div class="tabletext">
                  ${contactMech.infoString?if_exists}
                </div>
              </#if>
            </td>
          </tr>
        </#list>
      </table>
    </div>
</div>
</#if>
