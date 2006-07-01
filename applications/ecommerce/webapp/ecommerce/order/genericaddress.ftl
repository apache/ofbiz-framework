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
 *@since      3.0
-->

<#-- generic address information -->
<#assign toName = (postalFields.toName)?if_exists>
<#if !toName?has_content && person?exists && person?has_content>
  <#assign toName = "">
  <#if person.personalTitle?has_content><#assign toName = person.personalTitle + " "></#if>
  <#assign toName = toName + person.firstName + " ">
  <#if person.middleName?has_content><#assign toName = toName + person.middleName + " "></#if>
  <#assign toName = toName + person.lastName>
  <#if person.suffix?has_content><#assign toName = toName + " " + person.suffix></#if>
</#if>

<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyToName}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="60" name="toName" value="${toName}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="60" name="attnName" value="${(postalFields.attnName)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="30" name="address1" value="${(postalFields.address1)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="30" name="address2" value="${(postalFields.address2)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="30" name="city" value="${(postalFields.city)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyState}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="stateProvinceGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled</#if>>
      <#if (postalFields.stateProvinceGeoId)?exists>
        <option>${postalFields.stateProvinceGeoId}</option>
        <option value="${postalFields.stateProvinceGeoId}">---</option>
      <#else>
        <option value="">${uiLabelMap.PartyNoState}</option>
      </#if>
      ${screens.render("component://common/widget/CommonScreens.xml#states")}
    </select>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode" value="${(postalFields.postalCode)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="countryGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled</#if>>
      <#if (postalFields.countryGeoId)?exists>
        <option>${postalFields.countryGeoId}</option>
        <option value="${postalFields.countryGeoId}">---</option>
      </#if>
      ${screens.render("component://common/widget/CommonScreens.xml#countries")}
    </select>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAllowSolicitation}?</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="allowSolicitation" class='selectBox' <#if requestParameters.useShipAddr?exists>disabled</#if>>
      <#if (partyContactMech.allowSolicitation)?exists>
        <option>${partyContactMech.allowSolicitation}</option>
        <option value="${partyContactMech.allowSolicitation}">---</option>
      </#if>
      <option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
    </select>
  </td>
</tr>
