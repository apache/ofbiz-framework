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

<#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
<div class="screenlet">
  <form id="refreshRequestForm" name="refreshRequestForm" method="post" action="<@ofbizUrl>manageAddress</@ofbizUrl>">
  </form>

  <#-- Add address -->
  <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.CommonAdd} ${uiLabelMap.CommonNew} ${uiLabelMap.CommonAddresses}</div>
  </div>
  <div class="screenlet-body">
    <a class="buttontext" id="addAddress" href="javascript:void(0)">${uiLabelMap.CommonAdd} ${uiLabelMap.OrderAddress}</a>
    <div id="displayCreateAddressForm" class="popup" style="display: none;">
      <div id="serverError" class="errorMessage"></div>
      <form id="createPostalAddressForm" name="createPostalAddressForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
        <input type="hidden" name="roleTypeId" value="CUSTOMER">
        <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
        <div class="form-row">
          ${uiLabelMap.PartyAddressLine1}*
          <div class="form-field">
            <input type="text" class="inputBox required" name="address1" id="address1" value="" size="30" maxlength="30"><span id="advice-required-address1" style="display: none" class="errorMessage">(required)</span>
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyAddressLine2}<div class="form-field"><input type="text" class="inputBox" name="address2" value="" size="30" maxlength="30"></div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyCity}*
          <div class="form-field">
            <input type="text" class="inputBox required" name="city" id="city" value="" size="30" maxlength="30"><span id="advice-required-city" style="display: none" class="errorMessage">(required)</span>
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyZipCode}*
          <div class="form-field">
            <input type="text" class="inputBox required" name="postalCode" id="postalCode" value="" size="12" maxlength="10"><span id="advice-required-postalCode" style="display: none" class="errorMessage">(required)</span>
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyCountry}*
          <div class="form-field">
            <select name="countryGeoId" id="countryGeoId" class="selectBox required" style="width: 70%">
              ${screens.render("component://common/widget/CommonScreens.xml#countries")}
            </select>
            <span id="advice-required-countryGeoId" style="display: none" class="errorMessage">(required)</span>
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyState}*
          <div class="form-field">
            <select name="stateProvinceGeoId" id="stateProvinceGeoId" class="selectBox required" style="width: 70%">
              <option value="">${uiLabelMap.PartyNoState}</option>
              ${screens.render("component://common/widget/CommonScreens.xml#states")}
            </select>
            <span id="advice-required-stateProvinceGeoId" style="display: none" class="errorMessage">(required)</span>
          </div>
        </div>
        <div class="form-row">
          <b>${uiLabelMap.EcommerceMyDefaultShippingAddress}</b>
          <input type="checkbox" name="setShippingPurpose" id="setShippingPurpose" value="Y" <#if setShippingPurpose?exists>checked</#if>/>
        </div>
        <div class="form-row">
          <b>${uiLabelMap.EcommerceMyDefaultBillingAddress}</b>
          <input type="checkbox" name="setBillingPurpose" id="setBillingPurpose" value="Y" <#if setBillingPurpose?exists>checked</#if>/>
        </div>
        <div class="form-row">
          <a href="javascript:void(0);" id="submitPostalAddressForm" class="buttontext" onclick="createPartyPostalAddress('submitPostalAddressForm')">${uiLabelMap.CommonSave}</a>
          <form action="">
            <input class="popup_closebox buttontext" type="button" value="${uiLabelMap.CommonClose}"/>
          </form>
        </div>
      </form>
    </div>
    <script type="text/javascript">
      new Popup('displayCreateAddressForm','addAddress', {modal: true, position: 'center', trigger: 'click'})
    </script>
  </div>
  <br/>
  <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.EcommerceAddressBook}</div></div>
  <div class="screenlet-body"></div>

  <#-- Manage Addresses -->
  <div class="left center">
    <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.EcommerceDefault} ${uiLabelMap.CommonAddresses}</div></div>
    <div class="screenlet-body">
      <#assign postalAddressFlag = "N">
      <#list partyContactMechValueMaps as partyContactMechValueMap>
        <#assign contactMech = partyContactMechValueMap.contactMech?if_exists>
        <#if contactMech.contactMechTypeId?if_exists = "POSTAL_ADDRESS">
          <#assign partyContactMech = partyContactMechValueMap.partyContactMech?if_exists>
          <#if partyContactMechValueMap.partyContactMechPurposes?has_content>
            <#assign postalAddressFlag = "Y">
            <div id="displayEditAddressForm_${contactMech.contactMechId}" class="popup" style="display: none;">
              <#include "EditPostalAddress.ftl"/>
            </div>
            <div class="form-row">
              <div class="form-field">
                <#if showSetShippingPurpose == "N">
                  <h3>${uiLabelMap.EcommercePrimary} ${uiLabelMap.OrderShippingAddress}</h3>
                  <span class="buttontextdisabled">${uiLabelMap.EcommerceIsDefault}</span>
                <#else>
                  <h3>${uiLabelMap.EcommercePrimary} ${uiLabelMap.PartyBillingAddress}</h3>
                  <span class="buttontextdisabled">${uiLabelMap.EcommerceIsDefault}</span>
                </#if>
              </div>
            </div>

            <#assign postalAddress = partyContactMechValueMap.postalAddress?if_exists>
            <#if postalAddress?exists>
              <div class="form-row">
                <div class="form-label"></div>
                <div class="form-field">
                  <div>
                    ${postalAddress.address1}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city}
                    <#if postalAddress.stateProvinceGeoId?has_content>,&nbsp;${postalAddress.stateProvinceGeoId}</#if>
                    &nbsp;${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                    <#if (!postalAddress.countryGeoId?has_content || postalAddress.countryGeoId?if_exists = "USA")>
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ")  gt 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target="_blank" href="${uiLabelMap.EcommerceLookupWhitepagesLink}" class="linktext">(${uiLabelMap.EcommerceLookupWhitepages})</a>
                      </#if>
                    </#if>
                  </div>
                </div>
              </div>
              <div class="form-row"></div>
              <span>
                <#if showSetShippingPurpose == "N">
                  <div class="form-row">
                     <#assign pcmps = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(party.getRelatedByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechPurposeTypeId", "PHONE_SHIPPING")))>
                     <#if pcmps?has_content>
                       <#assign pcmp = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(pcmps)/>
                       <#assign telecomNumber = pcmp.getRelatedOne("TelecomNumber")/>
                     </#if>
      
                    <#if telecomNumber?has_content>
                      <#assign pcm = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(telecomNumber.getRelated("PartyContactMech"))/>
                      <h3>${uiLabelMap.PartyPhoneNumber}</h3>
                      ${telecomNumber.countryCode?if_exists}-
                      ${telecomNumber.areaCode?if_exists}-
                      ${telecomNumber.contactNumber?if_exists}-
                      ${pcm.extension?if_exists}
                    </#if>
                  </div><br/>
                  <a id="update_${contactMech.contactMechId}" href="javascript:void(0)" class="buttontext popup_link">${uiLabelMap.CommonEdit} ${uiLabelMap.OrderShippingAddress}</a>&nbsp;
                <#else>
                  <div class="form-row">
                     <#assign pcmps = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(party.getRelatedByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechPurposeTypeId", "PHONE_BILLING")))>
                     <#if pcmps?has_content>
                       <#assign pcmp = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(pcmps)/>
                       <#assign telecomNumber = pcmp.getRelatedOne("TelecomNumber")/>
                     </#if>
                     
                    <#if telecomNumber?has_content>
                      <#assign pcm = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(telecomNumber.getRelated("PartyContactMech"))/>
                      <h3>${uiLabelMap.PartyPhoneNumber}</h3>
                      ${telecomNumber.countryCode?if_exists}-
                      ${telecomNumber.areaCode?if_exists}-
                      ${telecomNumber.contactNumber?if_exists}-
                      ${pcm.extension?if_exists}
                    </#if>
                  </div><br/>
                  <a id="update_${contactMech.contactMechId}" href="javascript:void(0)" class="buttontext popup_link">${uiLabelMap.CommonEdit} ${uiLabelMap.PartyBillingAddress}</a>&nbsp;
                </#if>

                <div class="form-row"></div>
                <a href="<@ofbizUrl>deletePostalAddress?contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a>&nbsp;&nbsp;
              </span>
              <script type="text/javascript">
                new Popup('displayEditAddressForm_${contactMech.contactMechId}','update_${contactMech.contactMechId}', {modal: true, position: 'center', trigger: 'click'})
              </script>
            <#else>
              <div class="form-row">
                <div class="form-label">
                  <h5>${uiLabelMap.PartyPostalInformationNotFound}.</h5>
                </div>
              </div>
            </#if>
            <div class="form-row"><hr class="sepbar"/></div>
          </#if>
        </#if>
      </#list>
      <#if postalAddressFlag == "N">
        <div class="form-row">
          <div class="form-label">
            <h5>${uiLabelMap.PartyPostalInformationNotFound}.</h5>
          </div>
        </div>
      </#if>
    </div>
  </div>

  <div class="center right">
    <div class="screenlet-header">
      <div class="boxhead">&nbsp;${uiLabelMap.EcommerceAdditional} ${uiLabelMap.CommonAddresses}</div>
    </div>

    <div class="screenlet-body">
      <#assign postalAddressFlag = "N">
      <#list partyContactMechValueMaps as partyContactMechValueMap>
        <#assign contactMech = partyContactMechValueMap.contactMech?if_exists>
        <#if contactMech.contactMechTypeId?if_exists = "POSTAL_ADDRESS">
          <#assign partyContactMech = partyContactMechValueMap.partyContactMech?if_exists>
          <#if !(partyContactMechValueMap.partyContactMechPurposes?has_content)>
            <#assign postalAddressFlag = "Y">
            <div id="displayEditAddressForm_${contactMech.contactMechId}" class="popup" style="display: none;">
              <#include "EditPostalAddress.ftl"/>
            </div>
            <#assign postalAddress = partyContactMechValueMap.postalAddress?if_exists>
            <#if postalAddress?exists>
              <div class="form-row">
                <div class="form-label"></div>
                <div class="form-field">
                  <div>
                    ${postalAddress.address1}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city}
                    <#if postalAddress.stateProvinceGeoId?has_content>,&nbsp;${postalAddress.stateProvinceGeoId}</#if>
                    &nbsp;${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                    <#if (!postalAddress.countryGeoId?has_content || postalAddress.countryGeoId?if_exists = "USA")>
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") gt 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target="_blank" href="#" class="linktext">(${uiLabelMap.EcommerceLookupWhitepages})</a>
                      </#if>
                    </#if>
                  </div>
                </div>
              </div>
              <div class="form-row">
                <span>
                  <a id="update_${contactMech.contactMechId}" href="javascript:void(0)" class="buttontext popup_link">${uiLabelMap.CommonEdit}</a>&nbsp;
                  <a href="<@ofbizUrl>deletePostalAddress?contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a>&nbsp;&nbsp;
                </span>
              </div>
              <script type="text/javascript">
                new Popup('displayEditAddressForm_${contactMech.contactMechId}','update_${contactMech.contactMechId}', {modal: true, position: 'center', trigger: 'click'})
              </script>
            <#else>
              <div class="form-row">
                <div class="form-label">
                  <h5>${uiLabelMap.PartyPostalInformationNotFound}.</h5>
                </div>
              </div>
            </#if>
            <div class="form-row"><hr class="sepbar"/></div>
          </#if>
        </#if>
      </#list>
      <#if postalAddressFlag == "N">
        <div class="form-row">
          <div class="form-label">
            <h5>${uiLabelMap.PartyPostalInformationNotFound}.</h5>
          </div>
        </div>
      </#if>
    </div>
  </div>
  <div class="form-row"></div>
</div>