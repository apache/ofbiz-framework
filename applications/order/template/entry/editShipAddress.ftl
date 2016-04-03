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

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>

   <div class="screenlet-body">
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
              <#if postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>updatePostalAddressOrderEntry</@ofbizUrl>" name="checkoutsetupform">
                <input type="hidden" name="contactMechId" value="${shipContactMechId!}"/>
              <#else>
              <form method="post" action="<@ofbizUrl>createPostalAddress</@ofbizUrl>" name="checkoutsetupform">
                <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS"/>
                <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
              </#if>
                <input type="hidden" name="partyId" value="${cart.getPartyId()?default("_NA_")}"/>
                <input type="hidden" name="finalizeMode" value="ship"/>
                <#if orderPerson?? && orderPerson?has_content>
                  <#assign toName = "">
                  <#if orderPerson.personalTitle?has_content><#assign toName = orderPerson.personalTitle + " "></#if>
                  <#assign toName = toName + orderPerson.firstName + " ">
                  <#if orderPerson.middleName?has_content><#assign toName = toName + orderPerson.middleName + " "></#if>
                  <#assign toName = toName + orderPerson.lastName>
                  <#if orderPerson.suffix?has_content><#assign toName = toName + " " + orderPerson.suffix></#if>
                <#elseif parameters.toName??>
                  <#assign toName = parameters.toName>
                <#else>
                  <#assign toName = "">
                </#if>
                <table width="100%" border="0" cellpadding="1" cellspacing="0">
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonToName}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" size="30" maxlength="60" name="toName" value="${toName}"/>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonAttentionName}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" size="30" maxlength="60" name="attnName" value="${parameters.attnName!}"/>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonAddressLine} 1</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" size="30" maxlength="30" name="address1" value="${parameters.address1!}"/>
                    *</td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonAddressLine} 2</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" size="30" maxlength="30" name="address2" value="${parameters.address2!}"/>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonCity}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" size="30" maxlength="30" name="city" value="${parameters.city!}"/>
                    *</td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonStateProvince}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <select name="stateProvinceGeoId">
                        <option value=""></option>
                        ${screens.render("component://common/widget/CommonScreens.xml#states")}
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonZipPostalCode}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" size="12" maxlength="10" name="postalCode" value="${parameters.postalCode!}"/>
                    *</td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonCountry}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <select name="countryGeoId">
                        ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                      </select>
                    *</td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top"><div>${uiLabelMap.OrderAllowSolicitation}</div></td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <select name="allowSolicitation">
                        <#assign selectedValue = parameters.allowSolicitation?default("")/>
                        <option></option><option ${(selectedValue=="Y")?string("selected=\"selected\"","")}>Y</option><option ${(selectedValue=="N")?string("selected=\"selected\"","")}>N</option>
                      </select>
                    </td>
                  </tr>
                </table>
              </form>
          </td>
        </tr>
      </table>
   </div>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
