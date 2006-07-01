<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones
 *@author     Olivier Heintz (olivier.heintz@nereide.biz) 
 *@since      1.0
-->

  <#if !mechMap.contactMech?exists>
    <#-- When creating a new contact mech, first select the type, then actually create -->
    <#if !preContactMechTypeId?has_content>
    <p class="head1">${uiLabelMap.PartyCreateNewContact}</p>
    <form method="post" action="<@ofbizUrl>editcontactmech</@ofbizUrl>" name="createcontactmechform">
      <input type="hidden" name="DONE_PAGE" value="${donePage}">
      <input type="hidden" name="partyId" value="${partyId}">
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <td width="26%"><div class="tabletext">${uiLabelMap.PartySelectContactType}:</div></td>
          <td width="74%">
            <select name="preContactMechTypeId" class="selectBox">
              <#list mechMap.contactMechTypes as contactMechType>
                <option value="${contactMechType.contactMechTypeId}">${contactMechType.get("description",locale)}</option>
              </#list>
            </select>&nbsp;<a href="javascript:document.createcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonCreate}]</a>
          </td>
        </tr>
      </table>
    </form>
    </#if>
  </#if>

  <#if mechMap.contactMechTypeId?has_content>
    <#if !mechMap.contactMech?has_content>
      <p class="head1">${uiLabelMap.PartyCreateNewContact}</p>
    &nbsp;<a href="<@ofbizUrl>authview/${donePage?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
      <#if contactMechPurposeType?exists>
        <div>(${uiLabelMap.PartyMsgContactHavePurpose} <b>"${contactMechPurposeType.get("description",locale)?if_exists}"</b>)</div>
      </#if>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <form method="post" action="<@ofbizUrl>${mechMap.requestName}</@ofbizUrl>" name="editcontactmechform">
        <input type="hidden" name="DONE_PAGE" value="${donePage}">
        <input type="hidden" name="contactMechTypeId" value="${mechMap.contactMechTypeId}">
        <input type="hidden" name="partyId" value="${partyId}">        
        <#if cmNewPurposeTypeId?has_content><input type="hidden" name="contactMechPurposeTypeId" value="${cmNewPurposeTypeId}"></#if>
        <#if preContactMechTypeId?exists><input type="hidden" name="preContactMechTypeId" value="${preContactMechTypeId}"></#if>
        <#if contactMechPurposeTypeId?exists><input type="hidden" name="contactMechPurposeTypeId" value="${contactMechPurposeTypeId?if_exists}"></#if>
        <#if paymentMethodId?has_content><input type='hidden' name='paymentMethodId' value='${paymentMethodId}'></#if>
    <#else>
      <p class="head1">${uiLabelMap.PartyEditContactInformation}</p>
    &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <#if mechMap.purposeTypes?has_content>
        <tr>
          <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyContactPurposes}</div></td>
          <td width="5">&nbsp;</td>
          <td width="74%">
            <table border="0" cellspacing="1" bgcolor="black">  
            <#if mechMap.partyContactMechPurposes?has_content>          
              <#list mechMap.partyContactMechPurposes as partyContactMechPurpose>
                <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                <tr>
                  <td bgcolor="white">
                    <div class="tabletext">&nbsp;
                      <#if contactMechPurposeType?has_content>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                      <#else>
                        <b>${uiLabelMap.PartyPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      (${uiLabelMap.CommonSince}:${partyContactMechPurpose.fromDate.toString()})
                      <#if partyContactMechPurpose.thruDate?has_content>(${uiLabelMap.CommonExpire}: ${partyContactMechPurpose.thruDate.toString()}</#if>
                    &nbsp;</div></td>
                  <td bgcolor="white"><div><a href="<@ofbizUrl>deletePartyContactMechPurpose?partyId=${partyId}&contactMechId=${contactMechId}&contactMechPurposeTypeId=${partyContactMechPurpose.contactMechPurposeTypeId}&fromDate=${partyContactMechPurpose.fromDate.toString()}&DONE_PAGE=${donePage?replace("=","%3d")}&useValues=true</@ofbizUrl>" class="buttontext">&nbsp;${uiLabelMap.CommonDelete}&nbsp;</a></div></td>
                </tr>
              </#list>
            </#if>              
            
              <tr>
                <form method="post" action="<@ofbizUrl>createPartyContactMechPurpose</@ofbizUrl>" name="newpurposeform">
                <input type="hidden" name="partyId" value="${partyId}">
                <input type="hidden" name="DONE_PAGE" value="${donePage}">
                <input type="hidden" name="useValues" value="true">
                <input type="hidden" name="contactMechId" value="${contactMechId?if_exists}">
                  <td bgcolor="white">
                    <select name="contactMechPurposeTypeId" class="selectBox">
                      <option></option>
                      <#list mechMap.purposeTypes as contactMechPurposeType>
                        <option value="${contactMechPurposeType.contactMechPurposeTypeId}">${contactMechPurposeType.get("description",locale)}</option>
                      </#list>
                    </select>
                  </td>
                </form>
                <td bgcolor="white"><div><a href="javascript:document.newpurposeform.submit()" class="buttontext">&nbsp;${uiLabelMap.PartyAddPurpose}&nbsp;</a></div></td>
              </tr>
            </table>
          </td>
        </tr>
        </#if>
        <form method="post" action="<@ofbizUrl>${mechMap.requestName}</@ofbizUrl>" name="editcontactmechform">
        <input type="hidden" name="contactMechId" value="${contactMechId}">
        <input type="hidden" name="contactMechTypeId" value="${mechMap.contactMechTypeId}">
        <input type="hidden" name="partyId" value="${partyId}">
        <input type="hidden" name="DONE_PAGE" value="${donePage?if_exists}">
    </#if>
  
  <#if "POSTAL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyToName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="toName" value="${(mechMap.postalAddress.toName)?default(request.getParameter('toName')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="attnName" value="${(mechMap.postalAddress.attnName)?default(request.getParameter('attnName')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="30" name="address1" value="${(mechMap.postalAddress.address1)?default(request.getParameter('address1')?if_exists)}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="address2" value="${(mechMap.postalAddress.address2)?default(request.getParameter('address2')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="city" value="${(mechMap.postalAddress.city)?default(request.getParameter('city')?if_exists)}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyState}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="stateProvinceGeoId" class="selectBox">
          <option>${(mechMap.postalAddress.stateProvinceGeoId)?if_exists}</option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode" value="${(mechMap.postalAddress.postalCode)?default(request.getParameter('postalCode')?if_exists)}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonCountry}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="countryGeoId" class="selectBox">
          <option>${(mechMap.postalAddress.countryGeoId)?if_exists}</option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
      *</td>
    </tr>
  <#elseif "TELECOM_NUMBER" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyPhoneNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="4" maxlength="10" name="countryCode" value="${(mechMap.telecomNumber.countryCode)?default(request.getParameter('countryCode')?if_exists)}">
        -&nbsp;<input type="text" class="inputBox" size="4" maxlength="10" name="areaCode" value="${(mechMap.telecomNumber.areaCode)?default(request.getParameter('areaCode')?if_exists)}">
        -&nbsp;<input type="text" class="inputBox" size="15" maxlength="15" name="contactNumber" value="${(mechMap.telecomNumber.contactNumber)?default(request.getParameter('contactNumber')?if_exists)}">
        &nbsp;${uiLabelMap.PartyContactExt}&nbsp;<input type="text" class="inputBox" size="6" maxlength="10" name="extension" value="${(mechMap.partyContactMech.extension)?default(request.getParameter('extension')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
      <td width="5">&nbsp;</td>
      <td><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyContactExt}]</div></td>
    </tr>
  <#elseif "EMAIL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${mechMap.contactMechType.get("description",locale)}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="60" maxlength="255" name="emailAddress" value="${(mechMap.contactMech.infoString)?default(request.getParameter('emailAddress')?if_exists)}">
      *</td>
    </tr>
  <#else>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${mechMap.contactMechType.get("description",locale)}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="60" maxlength="255" name="infoString" value="${(mechMap.contactMech.infoString)?if_exists}">
      *</td>
    </tr>
  </#if>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyContactAllowSolicitation}?</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="allowSolicitation" class="selectBox">
          <option>${(mechMap.partyContactMech.allowSolicitation)?if_exists}</option>
          <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
        </select>
      </td>
    </tr>
  </form>
  </table>

    &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
  <#else>
    &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
  </#if>
