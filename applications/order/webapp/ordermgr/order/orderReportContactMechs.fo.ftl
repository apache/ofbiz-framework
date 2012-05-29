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
<#escape x as x?xml>

<#if orderHeader.getString("orderTypeId") == "PURCHASE_ORDER">
    <#if supplierGeneralContactMechValueMap?exists>
        <#assign contactMech = supplierGeneralContactMechValueMap.contactMech>
        <fo:block font-weight="bold">${uiLabelMap.OrderPurchasedFrom}:</fo:block>
        <#assign postalAddress = supplierGeneralContactMechValueMap.postalAddress>
        <#if postalAddress?has_content>
            <fo:block text-indent="0.2in">
                <#if postalAddress.toName?has_content><fo:block>${postalAddress.toName}</fo:block></#if>
                <#if postalAddress.attnName?has_content><fo:block>${postalAddress.attnName?if_exists}</fo:block></#if>
                <fo:block>${postalAddress.address1?if_exists}</fo:block>
                <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2?if_exists}</fo:block></#if>
                <fo:block>
                    <#assign stateGeo = (delegator.findOne("Geo", {"geoId", postalAddress.stateProvinceGeoId?if_exists}, false))?if_exists />
                    ${postalAddress.city}<#if stateGeo?has_content>, ${stateGeo.geoName?if_exists}</#if> ${postalAddress.postalCode?if_exists}
                </fo:block>
                <fo:block>
                    <#assign countryGeo = (delegator.findOne("Geo", {"geoId", postalAddress.countryGeoId?if_exists}, false))?if_exists />
                    <#if countryGeo?has_content>${countryGeo.geoName?if_exists}</#if>
                </fo:block>
            </fo:block>                
        </#if>
    <#else>
        <#-- here we just display the name of the vendor, since there is no address -->
        <#assign vendorParty = orderReadHelper.getBillFromParty()>
        <fo:block>
            <fo:inline font-weight="bold">${uiLabelMap.OrderPurchasedFrom}:</fo:inline> ${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(vendorParty)}
        </fo:block>
    </#if>
</#if>

<#-- list all postal addresses of the order.  there should be just a billing and a shipping here. -->
<#list orderContactMechValueMaps as orderContactMechValueMap>
    <#assign contactMech = orderContactMechValueMap.contactMech>
    <#assign contactMechPurpose = orderContactMechValueMap.contactMechPurposeType>
    <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
        <#assign postalAddress = orderContactMechValueMap.postalAddress>
        <fo:block font-weight="bold">${contactMechPurpose.get("description",locale)}:</fo:block>
        <fo:block text-indent="0.2in">
            <#if postalAddress?has_content>
                <#if postalAddress.toName?has_content><fo:block>${postalAddress.toName?if_exists}</fo:block></#if>
                <#if postalAddress.attnName?has_content><fo:block>${postalAddress.attnName?if_exists}</fo:block></#if>
                <fo:block>${postalAddress.address1?if_exists}</fo:block>
                <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2?if_exists}</fo:block></#if>
                <fo:block>
                    <#assign stateGeo = (delegator.findOne("Geo", {"geoId", postalAddress.stateProvinceGeoId?if_exists}, false))?if_exists />
                    ${postalAddress.city}<#if stateGeo?has_content>, ${stateGeo.geoName?if_exists}</#if> ${postalAddress.postalCode?if_exists}
                </fo:block>
                <fo:block>
                    <#assign countryGeo = (delegator.findOne("Geo", {"geoId", postalAddress.countryGeoId?if_exists}, false))?if_exists />
                    <#if countryGeo?has_content>${countryGeo.geoName?if_exists}</#if>
                </fo:block>
            </#if>
        </fo:block>
    </#if>
</#list>

<fo:block space-after="0.2in"/>

<#if orderPaymentPreferences?has_content>
    <fo:block font-weight="bold">${uiLabelMap.AccountingPaymentInformation}:</fo:block>
    <#list orderPaymentPreferences as orderPaymentPreference>
        <fo:block text-indent="0.2in">
            <#assign paymentMethodType = orderPaymentPreference.getRelatedOne("PaymentMethodType", false)?if_exists>
            <#if (orderPaymentPreference?? && (orderPaymentPreference.getString("paymentMethodTypeId") == "CREDIT_CARD") && (orderPaymentPreference.getString("paymentMethodId")?has_content))>
                <#assign creditCard = orderPaymentPreference.getRelatedOne("PaymentMethod", false).getRelatedOne("CreditCard", false)>
                ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
            <#else>
                ${paymentMethodType.get("description",locale)?if_exists}
            </#if>
        </fo:block>
    </#list>
</#if>
<#if orderHeader.getString("orderTypeId") == "SALES_ORDER" && shipGroups?has_content>
    <fo:block font-weight="bold">${uiLabelMap.OrderShipmentInformation}:</fo:block>
    <#list shipGroups as shipGroup>
        <fo:block text-indent="0.2in">
            <#if shipGroups.size() gt 1>${shipGroup.shipGroupSeqId} - </#if>
            <#if (shipGroup.shipmentMethodTypeId)?exists>
                ${(shipGroup.getRelatedOne("ShipmentMethodType", false).get("description", locale))?default(shipGroup.shipmentMethodTypeId)}
            </#if>
            <#if (shipGroup.shipAfterDate)?exists || (shipGroup.shipByDate)?exists>
                <#if (shipGroup.shipAfterDate)?exists> - ${uiLabelMap.OrderShipAfterDate}: ${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(shipGroup.shipAfterDate)}</#if><#if (shipGroup.shipByDate)?exists> - ${uiLabelMap.OrderShipBeforeDate}: ${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(shipGroup.shipByDate)}</#if>
            </#if>
        </fo:block>
    </#list>
</#if>

<#if orderTerms?has_content && orderTerms.size() gt 0>
    <fo:block font-weight="bold">${uiLabelMap.OrderOrderTerms}:</fo:block>
    <#list orderTerms as orderTerm>
        <fo:block text-indent="0.2in">
            ${orderTerm.getRelatedOne("TermType", false).get("description",locale)} ${orderTerm.termValue?default("")} ${orderTerm.termDays?default("")} ${orderTerm.textValue?default("")}
        </fo:block>
    </#list>
</#if>

<fo:block space-after="0.2in"/>
</#escape>
