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

<#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request) />
<div class="screenlet">
  <form id="refreshRequestForm" method="post" action="<@ofbizUrl>manageAddress</@ofbizUrl>">
  </form>

  <h3>${uiLabelMap.EcommerceAddressBook}</h3>
  <div class="screenlet-body">
    <#-- Add address -->
    <a class="button" id="addAddress" href="javascript:void(0)">${uiLabelMap.EcommerceAddNewAddress}</a>
    <div id="displayCreateAddressForm" class="popup" style="display: none;">
      <div id="serverError" class="errorMessage"></div>
      <form id="createPostalAddressForm" method="post" action="">
        <fieldset>
          <input type="hidden" name="roleTypeId" value="CUSTOMER" />
          <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}" />
          <div>
            <label for="address1">${uiLabelMap.PartyAddressLine1}*
               <span id="advice-required-address1" style="display: none" class="errorMessage">(required)</span>
            </label>
            <input type="text" class="required" name="address1" id="address1" value="" maxlength="30" />
          </div>
          <div>
            <label for="address2">${uiLabelMap.PartyAddressLine2}</label>
            <input type="text" name="address2" id="address2" value="" maxlength="30" />
          </div>
          <div>
            <label for="city">${uiLabelMap.PartyCity}*
              <span id="advice-required-city" style="display: none" class="errorMessage">(required)</span>
            </label>
            <input type="text" class="required" name="city" id="city" value="" maxlength="30" />
          </div>
          <div>
            <label for="postalCode">${uiLabelMap.PartyZipCode}*
              <span id="advice-required-postalCode" style="display: none" class="errorMessage">(required)</span>
            </label>
            <input type="text" class="required" name="postalCode" id="postalCode" value="" maxlength="10" />
          </div>
          <div>
            <label for="countryGeoId">${uiLabelMap.PartyCountry}*
              <span id="advice-required-countryGeoId" style="display: none" class="errorMessage">(required)</span>
            </label>
             <select name="countryGeoId" id="countryGeoId" class="required" style="width: 70%">
               <#if countryGeoId??>
                 <option value="${countryGeoId}">${countryGeoId}</option>
               </#if>
               ${screens.render("component://common/widget/CommonScreens.xml#countries")}
             </select>
          </div>
          <div id="states">
            <label for="stateProvinceGeoId">${uiLabelMap.PartyState}*
              <span id="advice-required-stateProvinceGeoId" style="display: none" class="errorMessage">(required)</span>
            </label>  
              <select name="stateProvinceGeoId" id="stateProvinceGeoId" style="width: 70%">
              <#if stateProvinceGeoId?has_content>
                <option value="${stateProvinceGeoId}">${stateProvinceGeoId}</option>
              <#else>
                <option value="_NA_">${uiLabelMap.PartyNoState}</option>
              </#if>
              </select>
          </div>
          <div>
            <label for="setBillingPurpose">${uiLabelMap.EcommerceMyDefaultBillingAddress}</label>
            <input type="checkbox" name="setBillingPurpose" id="setBillingPurpose" value="Y" <#if setBillingPurpose?exists>checked="checked"</#if> />
          </div>
          <div>
            <label for="setShippingPurpose">${uiLabelMap.EcommerceMyDefaultShippingAddress}</label>
            <input type="checkbox" name="setShippingPurpose" id="setShippingPurpose" value="Y" <#if setShippingPurpose?exists>checked="checked"</#if> />
          </div>
          <div>
            <a href="javascript:void(0);" id="submitPostalAddressForm" class="button" onclick="createPartyPostalAddress('submitPostalAddressForm')">${uiLabelMap.CommonSubmit}</a>
            <a href="javascript:void(0);" class="popup_closebox button" >${uiLabelMap.CommonClose}</a>
          </div>
        </fieldset>
      </form>
    </div>
    <script type="text/javascript">
      //<![CDATA[
        new Popup('displayCreateAddressForm','addAddress', {modal: true, position: 'center', trigger: 'click'})
      //]]>
    </script>
  </div>

  <#-- Default Addresses -->
  <div class="left center">
    <h3>${uiLabelMap.EcommerceDefaultAddresses}</h3>
    <div class="screenlet-body">
      <#--===================================== Billing Address and Telecom number ===========================================-->
      <h3>${uiLabelMap.EcommercePrimaryBillingAddress}</h3>
      <ul>
      <#if billToContactMechId?exists>
        <li>${billToAddress1?if_exists}</li>
        <#if billToAddress2?has_content><li>${billToAddress2?if_exists}</li></#if>
        <li>
          <#if billToStateProvinceGeoId?has_content && billToStateProvinceGeoId != "_NA_">
            ${billToStateProvinceGeoId}
          </#if>
          ${billToCity?if_exists},
          ${billToPostalCode?if_exists}
        </li>
        <li>${billToCountryGeoId?if_exists}</li>
        <#if billToTelecomNumber?has_content>
        <li>
          ${billToTelecomNumber.countryCode?if_exists}-
          ${billToTelecomNumber.areaCode?if_exists}-
          ${billToTelecomNumber.contactNumber?if_exists}
          <#if billToExtension?exists>-${billToExtension?if_exists}</#if>
        </li>
        </#if>
        <li><a id="updateBillToPostalAddress" href="javascript:void(0)" class="button popup_link">${uiLabelMap.CommonEdit}</a></li>
      <#else>
        <li>${uiLabelMap.PartyPostalInformationNotFound}</li>
      </#if>
      </ul>
      <div id="displayEditBillToPostalAddress" class="popup" style="display: none;">
        <#include "EditBillToAddress.ftl" />
      </div>
      <script type="text/javascript">
        //<![CDATA[
        new Popup('displayEditBillToPostalAddress', 'updateBillToPostalAddress', {modal: true, position: 'center', trigger: 'click'})
        //]]>
      </script>

    <#--===================================== Shipping Address and Telecom number ===========================================-->
      <h3>${uiLabelMap.EcommercePrimaryShippingAddress}</h3>
      <ul>
      <#if shipToContactMechId?exists>
        <li>${shipToAddress1?if_exists}</li>
        <#if shipToAddress2?has_content><li>${shipToAddress2?if_exists}</li></#if>
        <li>
        <#if shipToStateProvinceGeoId?has_content && shipToStateProvinceGeoId != "_NA_">
          ${shipToStateProvinceGeoId}
        </#if>
          ${shipToCity?if_exists},
          ${shipToPostalCode?if_exists}
        </li>
        <li>${shipToCountryGeoId?if_exists}</li>
        <#if shipToTelecomNumber?has_content>
        <li>
          ${shipToTelecomNumber.countryCode?if_exists}-
          ${shipToTelecomNumber.areaCode?if_exists}-
          ${shipToTelecomNumber.contactNumber?if_exists}
          <#if shipToExtension?exists>-${shipToExtension?if_exists}</#if>
        </li>
        </#if>
        <li><a id="updateShipToPostalAddress" href="javascript:void(0)" class="button popup_link">${uiLabelMap.CommonEdit}</a></li>
      <#else>
        <li>${uiLabelMap.PartyPostalInformationNotFound}</li>
      </#if>
      </ul>
      <div id="displayEditShipToPostalAddress" class="popup" style="display: none;">
        <#include "EditShipToAddress.ftl" />
      </div>
      <script type="text/javascript">
         //<![CDATA[
          new Popup('displayEditShipToPostalAddress','updateShipToPostalAddress', {modal: true, position: 'center', trigger: 'click'})
          //]]>
      </script>
    </div>
  </div>

  <#-- Additional Addresses -->
  <div class="center right">
    <h3>${uiLabelMap.EcommerceAdditionalAddresses}</h3>

    <div class="screenlet-body">
      <#assign postalAddressFlag = "N" />
      <#list partyContactMechValueMaps as partyContactMechValueMap>
        <#assign contactMech = partyContactMechValueMap.contactMech?if_exists />
        <#if contactMech.contactMechTypeId?if_exists = "POSTAL_ADDRESS">
          <#assign partyContactMech = partyContactMechValueMap.partyContactMech?if_exists />
          <#if !(partyContactMechValueMap.partyContactMechPurposes?has_content)>
            <#assign postalAddressFlag = "Y" />
            <#assign postalAddress = partyContactMechValueMap.postalAddress?if_exists />
            <div id="displayEditAddressForm_${contactMech.contactMechId}" class="popup" style="display: none;">
              <#include "EditPostalAddress.ftl" />
            </div>
            <#if postalAddress?exists>
                <div class="form-field">
                    <ul>
                      <li>${postalAddress.address1}</li>
                      <#if postalAddress.address2?has_content><li>${postalAddress.address2}</li></#if>
                      <li>${postalAddress.city}</li>
                      <li>
                      <#if postalAddress.stateProvinceGeoId?has_content && postalAddress.stateProvinceGeoId != "_NA_">
                        ${postalAddress.stateProvinceGeoId}
                      </#if>
                        ${postalAddress.postalCode?if_exists}
                      </li>
                    <#if postalAddress.countryGeoId?has_content><li>${postalAddress.countryGeoId}</li></#if>
                    </ul>
                    <#if (!postalAddress.countryGeoId?has_content || postalAddress.countryGeoId?if_exists = "USA")>
                      <#assign addr1 = postalAddress.address1?if_exists />
                      <#if (addr1.indexOf(" ") > 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" ")) />
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1) />
                        <a target="_blank" href="#" class="linktext">(${uiLabelMap.CommonLookupWhitepages})</a>
                      </#if>
                    </#if>
                </div>
              <div>
                <span>
                  <a id="update_${contactMech.contactMechId}" href="javascript:void(0)" class="button popup_link" onclick="showState('${contactMech.contactMechId}')">${uiLabelMap.CommonEdit}</a></span>
                  <form id="deletePostalAddress_${contactMech.contactMechId}" method= "post" action= "<@ofbizUrl>deletePostalAddress</@ofbizUrl>">
                    <fieldset>
                      <input type= "hidden" name= "contactMechId" value= "${contactMech.contactMechId}" />
                      <a href="javascript:$('deletePostalAddress_${contactMech.contactMechId}').submit()" class='button'>${uiLabelMap.CommonDelete}</a>
                    </fieldset>
                  </form> 
              </div>
              <script type="text/javascript">
                //<![CDATA[
                new Popup('displayEditAddressForm_${contactMech.contactMechId}','update_${contactMech.contactMechId}', {modal: true, position: 'center', trigger: 'click'})
                //]]>
              </script>
            <#else>
              <div>
                 <label>${uiLabelMap.PartyPostalInformationNotFound}.</label>
              </div>
            </#if>
          </#if>
        </#if>
      </#list>
      <#if postalAddressFlag == "N">
        <div>
            <label>${uiLabelMap.PartyPostalInformationNotFound}.</label>
        </div>
      </#if>
    </div>
  </div>
</div>