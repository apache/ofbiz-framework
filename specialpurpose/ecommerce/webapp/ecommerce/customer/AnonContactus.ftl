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

<h3>${uiLabelMap.CommonContactUs}</h3>
<form id="contactForm" method="post" action="<@ofbizUrl>submitAnonContact</@ofbizUrl>">
  <fieldset>
    <input type="hidden" name="partyIdFrom" value="${(userLogin.partyId)?if_exists}" />
    <input type="hidden" name="partyIdTo" value="${productStore.payToPartyId?if_exists}"/>
    <input type="hidden" name="contactMechTypeId" value="WEB_ADDRESS" />
    <input type="hidden" name="communicationEventTypeId" value="WEB_SITE_COMMUNICATI" />
    <input type="hidden" name="productStoreId" value="${productStore.productStoreId}" />
    <input type="hidden" name="emailType" value="CONT_NOTI_EMAIL" />
    <input type="hidden" name="note" value="${Static["org.ofbiz.base.util.UtilHttp"].getFullRequestUrl(request).toString()}" />
    <div>
      <label for="comment">${uiLabelMap.CommonComment}:</label>
      <textarea name="content" id="comment" class="required" cols="50" rows="5"></textarea>
    </div>
    <div>
      <label for="emailAddress">${uiLabelMap.FormFieldTitle_emailAddress} *</label>
      <input type="text" name="emailAddress" id="emailAddress" class="required" />
    </div>
    <div>
      <label for="firstName">${uiLabelMap.FormFieldTitle_firstName}</label>
      <input type="text" name="firstName" id="firstName" class="required" />
    </div>
    <div>
      <label for="lastName">${uiLabelMap.FormFieldTitle_lastName}</label>
      <input type="text" name="lastName" id="lastName" class="required" />
    </div>
    <div>
      <label for="postalCode">${uiLabelMap.CommonZipPostalCode}</label>
      <input name="postalCode" id="postalCode" type="text" />
    </div>
    <div>
      <label>${uiLabelMap.CommonCountry}</label>
      <select name="countryCode" id="countryCodeGeoId" class="required">
        ${screens.render("component://common/widget/CommonScreens.xml#countries")}
      </select>
    </div>
  </fieldset>
  <div>
    <input type="submit" value="${uiLabelMap.CommonSubmit}" />
  </div>
</form>