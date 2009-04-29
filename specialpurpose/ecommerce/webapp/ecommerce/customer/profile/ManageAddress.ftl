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

  <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.EcommerceAddressBook}</div></div>
  <div class="screenlet-body">
    <#-- Add address -->
    <div class="form-row" align="right">
      <a class="buttontext" id="addAddress" href="javascript:void(0)">${uiLabelMap.EcommerceAddNewAddress}</a>
    </div>
    <div id="displayCreateAddressForm" class="popup" style="display: none;">
      <div id="serverError" class="errorMessage"></div>
      <form id="createPostalAddressForm" name="createPostalAddressForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
        <input type="hidden" name="roleTypeId" value="CUSTOMER">
        <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
        <div class="form-row">
          ${uiLabelMap.PartyAddressLine1}*
          <span id="advice-required-address1" style="display: none" class="errorMessage">(required)</span>
          <div class="form-field">
            <input type="text" class="required" name="address1" id="address1" value="" size="30" maxlength="30">
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyAddressLine2}<div class="form-field"><input type="text" name="address2" value="" size="30" maxlength="30"></div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyCity}*
          <span id="advice-required-city" style="display: none" class="errorMessage">(required)</span>
          <div class="form-field">
            <input type="text" class="required" name="city" id="city" value="" size="30" maxlength="30">
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyZipCode}*
          <span id="advice-required-postalCode" style="display: none" class="errorMessage">(required)</span>
          <div class="form-field">
            <input type="text" class="required" name="postalCode" id="postalCode" value="" size="30" maxlength="10">
          </div>
        </div>
        <div class="form-row">
          ${uiLabelMap.PartyCountry}*
          <span id="advice-required-countryGeoId" style="display: none" class="errorMessage">(required)</span>
          <div class="form-field">
            <select name="countryGeoId" id="countryGeoId" class="required" style="width: 70%">
              <#if countryGeoId??>
                <option value="${countryGeoId}">${countryGeoId}</option>
              </#if>
              ${screens.render("component://common/widget/CommonScreens.xml#countries")}
            </select>
          </div>
        </div>
        <div id="states" class="form-row">
          ${uiLabelMap.PartyState}*
          <span id="advice-required-stateProvinceGeoId" style="display: none" class="errorMessage">(required)</span>
          <div class="form-field">
            <select name="stateProvinceGeoId" id="stateProvinceGeoId" style="width: 70%">
              <#if stateProvinceGeoId?has_content>
                <option value="${stateProvinceGeoId}">${stateProvinceGeoId}</option>
              <#else>
                <option value="_NA_">${uiLabelMap.PartyNoState}</option>
              </#if>
            </select>
          </div>
        </div>
        <div class="form-row">
          <b>${uiLabelMap.EcommerceMyDefaultBillingAddress}</b>
          <input type="checkbox" name="setBillingPurpose" id="setBillingPurpose" value="Y" <#if setBillingPurpose?exists>checked</#if>/>
        </div>
        <div class="form-row">
          <b>${uiLabelMap.EcommerceMyDefaultShippingAddress}</b>
          <input type="checkbox" name="setShippingPurpose" id="setShippingPurpose" value="Y" <#if setShippingPurpose?exists>checked</#if>/>
        </div>
        <div class="form-row">
          <a href="javascript:void(0);" id="submitPostalAddressForm" class="buttontext" onclick="createPartyPostalAddress('submitPostalAddressForm')">${uiLabelMap.CommonSubmit}</a>
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

  <#-- Default Addresses -->
  <div class="left center">
    <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.EcommerceDefaultAddresses}</div></div>
    <div class="screenlet-body">
      <#--===================================== Billing Address and Telecom number ===========================================-->
      <h3>${uiLabelMap.EcommercePrimaryBillingAddress}</h3>
      <#if billToContactMechId?exists>
        ${billToAddress1?if_exists}<br/>
        <#if billToAddress2?has_content>${billToAddress2?if_exists}<br/></#if>
        <#if billToStateProvinceGeoId?has_content && billToStateProvinceGeoId != "_NA_">
          ${billToStateProvinceGeoId}
        </#if>
          ${billToCity?if_exists},
          ${billToPostalCode?if_exists}<br/>
          ${billToCountryGeoId?if_exists}<br/>
        <#if billToTelecomNumber?has_content>
          ${billToTelecomNumber.countryCode?if_exists}-
          ${billToTelecomNumber.areaCode?if_exists}-
          ${billToTelecomNumber.contactNumber?if_exists}
          <#if billToExtension?exists>-${billToExtension?if_exists}</#if><br/>
        </#if>
        <a id="updateBillToPostalAddress" href="javascript:void(0)" class="buttontext popup_link">${uiLabelMap.CommonEdit}</a>&nbsp;
      <#else>
        ${uiLabelMap.PartyPostalInformationNotFound}
      </#if>
      <div id="displayEditBillToPostalAddress" class="popup" style="display: none;">
        <#include "EditBillToAddress.ftl"/>
      </div>
      <div class="form-row"><hr class="sepbar"/></div>
      <script type="text/javascript">
        new Popup('displayEditBillToPostalAddress', 'updateBillToPostalAddress', {modal: true, position: 'center', trigger: 'click'})
      </script>

    <#--===================================== Shipping Address and Telecom number ===========================================-->
      <h3>${uiLabelMap.EcommercePrimaryShippingAddress}</h3>
      <#if shipToContactMechId?exists>
        ${shipToAddress1?if_exists}<br/>
        <#if shipToAddress2?has_content>${shipToAddress2?if_exists}<br/></#if>
        <#if shipToStateProvinceGeoId?has_content && shipToStateProvinceGeoId != "_NA_">
          ${shipToStateProvinceGeoId}
        </#if>
          ${shipToCity?if_exists},
          ${shipToPostalCode?if_exists}<br/>
          ${shipToCountryGeoId?if_exists}<br/>
        <#if shipToTelecomNumber?has_content>
          ${shipToTelecomNumber.countryCode?if_exists}-
          ${shipToTelecomNumber.areaCode?if_exists}-
          ${shipToTelecomNumber.contactNumber?if_exists}
          <#if shipToExtension?exists>-${shipToExtension?if_exists}</#if><br/>
        </#if>
        <a id="updateShipToPostalAddress" href="javascript:void(0)" class="buttontext popup_link">${uiLabelMap.CommonEdit}</a>&nbsp;
      <#else>
        ${uiLabelMap.PartyPostalInformationNotFound}
      </#if>
      <div id="displayEditShipToPostalAddress" class="popup" style="display: none;">
        <#include "EditShipToAddress.ftl"/>
      </div>
      <div class="form-row"><hr class="sepbar"/></div>
      <script type="text/javascript">
          new Popup('displayEditShipToPostalAddress','updateShipToPostalAddress', {modal: true, position: 'center', trigger: 'click'})
      </script>
    </div>
  </div>

  <#-- Additional Addresses -->
  <div class="center right">
    <div class="screenlet-header">
      <div class="boxhead">&nbsp;${uiLabelMap.EcommerceAdditionalAddresses}</div>
    </div>

    <div class="screenlet-body">
      <#assign postalAddressFlag = "N">
      <#list partyContactMechValueMaps as partyContactMechValueMap>
        <#assign contactMech = partyContactMechValueMap.contactMech?if_exists>
        <#if contactMech.contactMechTypeId?if_exists = "POSTAL_ADDRESS">
          <#assign partyContactMech = partyContactMechValueMap.partyContactMech?if_exists>
          <#if !(partyContactMechValueMap.partyContactMechPurposes?has_content)>
            <#assign postalAddressFlag = "Y">
            <#assign postalAddress = partyContactMechValueMap.postalAddress?if_exists>
            <div id="displayEditAddressForm_${contactMech.contactMechId}" class="popup" style="display: none;">
              <#include "EditPostalAddress.ftl"/>
            </div>
            <#if postalAddress?exists>
              <div class="form-row">
                <div class="form-label"></div>
                <div class="form-field">
                  <div>
                    ${postalAddress.address1}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city}
                    <#if postalAddress.stateProvinceGeoId?has_content && postalAddress.stateProvinceGeoId != "_NA_">
                      ${postalAddress.stateProvinceGeoId}
                    </#if>
                    &nbsp;${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                    <#if (!postalAddress.countryGeoId?has_content || postalAddress.countryGeoId?if_exists = "USA")>
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") gt 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target="_blank" href="#" class="linktext">(${uiLabelMap.CommonLookupWhitepages})</a>
                      </#if>
                    </#if>
                  </div>
                </div>
              </div>
              <div class="form-row">
                <span>
                  <a id="update_${contactMech.contactMechId}" href="javascript:void(0)" class="buttontext popup_link" onclick="showState('${contactMech.contactMechId}')">${uiLabelMap.CommonEdit}</a>&nbsp;
                  <form name= "deletePostalAddress_${contactMech.contactMechId}" method= "post" action= "<@ofbizUrl>deletePostalAddress</@ofbizUrl>">
                    <input type= "hidden" name= "contactMechId" value= "${contactMech.contactMechId}"/>
                    <a href='javascript:document.deletePostalAddress_${contactMech.contactMechId}.submit()' class='buttontext'>&nbsp;${uiLabelMap.CommonDelete}&nbsp;</a>
                  </form> 
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