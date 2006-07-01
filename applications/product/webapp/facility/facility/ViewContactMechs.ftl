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
 * @author     thierry.grauss@etu.univ-tours.fr (migration to uiLabelMap)
 * @version    $Rev$
 * @since      2.2
 */
-->

<div class="head1">${uiLabelMap.ProductFacility} <span class='head2'>${facility.facilityName?if_exists} [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>
<#if facilityId?has_content>
    <a href="/workeffort/control/month?facilityId=${facilityId}&externalLoginKey=${externalLoginKey?if_exists}" class="buttontext">[${uiLabelMap.CommonViewCalendar}]</a>
    <a href="<@ofbizUrl>EditContactMech?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewContactMech}]</a>
</#if>

<br/><br/>
<table width='100%' border='0' cellspacing='0' cellpadding='0'>
  <tr>
    <td>
  <#if contactMeches?has_content>
    <table width="100%" border="0" cellpadding="0">
      <tr align="left" valign='bottom'>
        <td colspan="7">
          <span class="head2">${uiLabelMap.ProductContactTypeInformation}</span>
        </td>
      </tr>
      <#list contactMeches as contactMechMap>
          <#assign contactMech = contactMechMap.contactMech>
          <#assign facilityContactMech = contactMechMap.facilityContactMech>
          <tr><td colspan="7"><hr class='sepbar'></td></tr>
          <tr>
            <td align="right" valign="top" width="10%">
              <div class="tabletext">&nbsp;<b>${contactMechMap.contactMechType.get("description",locale)}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <#list contactMechMap.facilityContactMechPurposes as facilityContactMechPurpose>
                  <#assign contactMechPurposeType = facilityContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                    <div class="tabletext">
                      <#if contactMechPurposeType?has_content>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                      <#else>
                        <b>${uiLabelMap.ProductPurposeTypeNotFoundWithId}: "${facilityContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      <#if facilityContactMechPurpose.thruDate?has_content>
                      (${uiLabelMap.CommonExpire}: ${facilityContactMechPurpose.thruDate.toString()})
                      </#if>
                    </div>
              </#list>
              <#if "POSTAL_ADDRESS" = contactMech.contactMechTypeId>
                  <#assign postalAddress = contactMechMap.postalAddress>
                  <div class="tabletext">                    
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}<br/></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${postalAddress.attnName}<br/></#if>
                    ${postalAddress.address1?if_exists}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2?if_exists}<br/></#if>
                    ${postalAddress.city?if_exists},
                    ${postalAddress.stateProvinceGeoId?if_exists}
                    ${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                  </div>
                  <#if (postalAddress?has_content && !postalAddress.countryGeoId?has_content) || postalAddress.countryGeoId = "USA">
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") > 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target='_blank' href='http://www.whitepages.com/find_person_results.pl?fid=a&s_n=${addressNum}&s_a=${addressOther}&c=${postalAddress.city?if_exists}&s=${postalAddress.stateProvinceGeoId?if_exists}&x=29&y=18' class='buttontext'>(lookup:whitepages.com)</a>
                      </#if>
                  </#if>
              <#elseif "TELECOM_NUMBER" = contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <div class="tabletext">
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode}-</#if>${telecomNumber.contactNumber?if_exists}
                    <#if facilityContactMech.extension?has_content>${uiLabelMap.CommonExt} ${facilityContactMech.extension}</#if>
                    <#if (telecomNumber?has_content && !telecomNumber.countryCode?has_content) || telecomNumber.countryCode = "011">
                      <a target='_blank' href='http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&telephone=${telecomNumber.contactNumber?if_exists}&btnsubmit.x=20&btnsubmit.y=8' class='buttontext'>(lookup:anywho.com)</a>
                      <a target='_blank' href='http://whitepages.com/find_person_results.pl?fid=p&ac=${telecomNumber.areaCode?if_exists}&s=&p=${telecomNumber.contactNumber?if_exists}&pt=b&x=40&y=9' class='buttontext'>(lookup:whitepages.com)</a>
                    </#if>
                  </div>
              <#elseif "EMAIL_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                    <a href='mailto:${contactMech.infoString?if_exists}' class='buttontext'>(${uiLabelMap.CommonSendEmail})</a>
                  </div>
              <#elseif "WEB_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target='_blank' href='${openAddress}' class='buttontext'>((${uiLabelMap.CommonOpenPageNewWindow})</a>
                  </div>
              <#else>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                  </div>
              </#if>
              <div class="tabletext">(${uiLabelMap.CommonUpdated}:&nbsp;${facilityContactMech.fromDate.toString()})</div>
              <#if facilityContactMech.thruDate?has_content><div class='tabletext'><b>${uiLabelMap.CommonUpdatedEffectiveThru}:&nbsp;${facilityContactMech.thruDate.toString()}</b></div></#if>
            </td>
            <td width="5">&nbsp;</td>
            <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <td align="right" valign="top" nowrap width="1%">
              <div><a href='<@ofbizUrl>EditContactMech?facilityId=${facilityId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>' class="buttontext">
              [${uiLabelMap.CommonUpdate}]</a>&nbsp;</div>
            </td>
            </#if>
            <#if security.hasEntityPermission("PARTYMGR", "_DELETE", session)>
            <td align="right" valign="top" width="1%">
              <div><a href='<@ofbizUrl>deleteContactMech/ViewContactMechs?facilityId=${facilityId}&contactMechId=${contactMech.contactMechId}&facilityId=${facilityId}</@ofbizUrl>' class="buttontext">
              [${uiLabelMap.CommonExpire}]</a>&nbsp;&nbsp;</div>
            </td>
            </#if>
          </tr>
      </#list>
    </table>
  <#else>
    <div class="tabletext">${uiLabelMap.CommonNoContactInformationOnFile}.</div>
  </#if>
    </td>
  </tr>
</table>
