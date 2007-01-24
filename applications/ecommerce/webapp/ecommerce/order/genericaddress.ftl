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

<#-- generic address information -->
<#assign toName = (parameters.toName)?if_exists>
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
    <input type="text" class="inputBox" size="30" maxlength="60" name="attnName" value="${(parameters.attnName)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="30" name="address1" value="${(parameters.address1)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="30" name="address2" value="${(parameters.address2)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="30" name="city" value="${(parameters.city)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyState}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="stateProvinceGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled</#if>>
      <#if (parameters.stateProvinceGeoId)?exists>
        <option>${parameters.stateProvinceGeoId}</option>
        <option value="${parameters.stateProvinceGeoId}">---</option>
      <#else>
        <option value="">${uiLabelMap.PartyNoState}</option>
      </#if>
      ${screens.render("component://common/widget/CommonScreens.xml#states")}
    </select>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode" value="${(parameters.postalCode)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
  *</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="countryGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled</#if>>
      <#if (parameters.countryGeoId)?exists>
        <option>${parameters.countryGeoId}</option>
        <option value="${parameters.countryGeoId}">---</option>
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
      <#if (parameters.allowSolicitation)?exists>
        <option>${parameters.allowSolicitation}</option>
        <option value="${parameters.allowSolicitation}">---</option>
      </#if>
      <option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
    </select>
  </td>
</tr>
