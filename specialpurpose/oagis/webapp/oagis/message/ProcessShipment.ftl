<?xml version="1.0" encoding="UTF-8"?>
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
<#-- xsi:schemaLocation="http://www.openapplications.org/161B_PROCESS_SHIPMENT_001 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/REL%201%20-%20VER%202/161B_process_shipment_005.xsd" -->
<#-- NOTE: not all of these are used -->
<#assign partnerNameSize = 35/>
<#assign partnerAddressLineSize = 35/>
<#assign partnerAddressCitySize = 15/>
<#assign partnerAddressStateSize = 2/>
<#assign partnerAddressPostalSize = 9/>
<#assign partnerAddressCountrySize = 2/>
<#assign partnerContactNameSize = 35/>
<#assign partnerContactPhoneSize = 21/>
<#assign partnerContactFaxSize = 10/>
<#assign partnerContactEmailSize = 65/>
<#assign shipmentNotesSize = 65/>
<n:PROCESS_SHIPMENT_001
    xmlns:n="http://www.openapplications.org/161B_PROCESS_SHIPMENT_001"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:os="http://www.openapplications.org/oagis_segments"
    xmlns:of="http://www.openapplications.org/oagis_fields">
  <os:CNTROLAREA>
    <os:BSR>
      <of:VERB>PROCESS</of:VERB>
      <of:NOUN>SHIPMENT</of:NOUN>
      <of:REVISION>001</of:REVISION>
    </os:BSR>
    <os:SENDER>
      <of:LOGICALID>${logicalId}</of:LOGICALID>
      <of:COMPONENT>INVENTORY</of:COMPONENT>
      <of:TASK>SHIPREQUEST</of:TASK>
      <of:REFERENCEID>${referenceId}</of:REFERENCEID>
      <of:CONFIRMATION>1</of:CONFIRMATION>
      <of:LANGUAGE>ENG</of:LANGUAGE>
      <of:CODEPAGE>NONE</of:CODEPAGE>
      <of:AUTHID>${authId!}</of:AUTHID>
    </os:SENDER>
    <os:DATETIMEISO>${sentDate!}</os:DATETIMEISO>
  </os:CNTROLAREA>
  <n:DATAAREA>
    <n:PROCESS_SHIPMENT>
      <n:SHIPMENT>
        <of:DOCUMENTID>${shipment.shipmentId!}</of:DOCUMENTID>
        <#if shipperId?has_content>
          <of:SHIPPERID>${shipperId}</of:SHIPPERID><#-- fill in from PartyCarrierAccount.accountNumber; make sure filter by from/thru date and PartyCarrierAccount.carrierPartyId==orderItemShipGroup.carrierPartyId; get most recent fromDate -->
          <#-- this we will also only send if there is a SHIPPERID, normally fulfillment partner will select based on TRANSMETHD, address, weight, etc -->
          <of:CARRIER>${orderItemShipGroup.carrierPartyId!}</of:CARRIER>
        </#if>
        <#if shipperId?has_content>
          <of:FRGHTTERMS>COLLECT</of:FRGHTTERMS>
        <#else>
          <of:FRGHTTERMS>PREPAID</of:FRGHTTERMS>
        </#if>
        <of:NOTES>${orderItemShipGroup.shippingInstructions!?xml}</of:NOTES>
        <of:SHIPNOTES>${shipnotes!?xml}</of:SHIPNOTES><#-- if order was a return replacement order (associated with return), then set to RETURNLABEL otherwise leave blank -->
        <of:TRANSMETHD>${orderItemShipGroup.shipmentMethodTypeId!}</of:TRANSMETHD>
        <os:PARTNER>
          <#if address?has_content>
            <#if (partyNameView.firstName)?has_content><#assign partyName = partyNameView.firstName/></#if>
            <#if (partyNameView.middleName)?has_content><#assign partyName = partyName + " " + partyNameView.middleName/></#if>
            <#if (partyNameView.lastName)?has_content><#assign partyName = partyName + " " + partyNameView.lastName/></#if>

            <#-- NOTE: this is the to name -->
            <#assign toName = (address.toName)?default(partyName)!/>
            <#if (toName?length > partnerNameSize)><#assign toName = (toName?substring(0,partnerNameSize))!/></#if>
            <of:NAME>${toName!?xml}</of:NAME>
            <of:PARTNRTYPE>SHIPTO</of:PARTNRTYPE>
            <of:CURRENCY>USD</of:CURRENCY>
            <os:ADDRESS>
              <#assign address1 = address.address1!/>
              <#if (address1?length > partnerAddressLineSize)><#assign address1 = (address1?substring(0,partnerAddressLineSize))!/></#if>
              <of:ADDRLINE>${address1!?xml}</of:ADDRLINE>
              <#if address.address2??>
                <#assign address2 = address.address2!/>
                <#if (address2?length > partnerAddressLineSize)><#assign address2 = (address2?substring(0,partnerAddressLineSize))!/></#if>
                <of:ADDRLINE>${address2?xml}</of:ADDRLINE>
              </#if>
              <#assign city = address.city!/>
              <#if (city?length > partnerAddressCitySize)><#assign city = (city?substring(0,partnerAddressCitySize))!/></#if>
              <of:CITY>${city!?xml}</of:CITY>
              <#assign countryGeoId = address.countryGeoId!/>
              <#if (countryGeoId?length > partnerAddressCountrySize)><#assign countryGeoId = (countryGeoId?substring(0,partnerAddressCountrySize))!/></#if>
              <of:COUNTRY>${countryGeoId!}</of:COUNTRY>
              <#--<of:DESCRIPTN></of:DESCRIPTN>
              <of:FAX></of:FAX>-->
              <of:POSTALCODE>${address.postalCode!?xml}</of:POSTALCODE>
              <of:STATEPROVN>${address.stateProvinceGeoId!}</of:STATEPROVN>
              <#if telecomNumber?has_content>
              <of:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode?xml}-</#if>${telecomNumber.areaCode!?xml}-${telecomNumber.contactNumber!?xml}</of:TELEPHONE>
              </#if>
            </os:ADDRESS>
            <os:CONTACT>
              <#-- NOTE: this is the attention name -->
              <#assign attnName = (address.attnName)?default(partyName)!/>
              <#if (attnName?length > partnerContactNameSize)><#assign attnName = (attnName?substring(0,partnerContactNameSize))!/></#if>
              <of:NAME>${attnName!?xml}</of:NAME>
              <of:EMAIL>${emailString!?xml}</of:EMAIL>
              <#--<of:FAX></of:FAX>-->
              <of:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode?xml}-</#if>${telecomNumber.areaCode!?xml}-${telecomNumber.contactNumber!?xml}</of:TELEPHONE>
            </os:CONTACT>
          </#if>
        </os:PARTNER>
        <#list shipmentItems as shipmentItem>
        <#assign product = shipmentItem.getRelatedOne("Product", false)>
        <#assign productType = product.getRelatedOne("ProductType", false)>
        <#if productType.isPhysical == "Y" || productType.isPhysical == "y">
        <n:SHIPITEM>
            <os:QUANTITY>
              <of:VALUE>${shipmentItem.quantity!}</of:VALUE>
              <of:NUMOFDEC>0</of:NUMOFDEC>
              <of:SIGN>+</of:SIGN>
              <of:UOM>EACH</of:UOM>
            </os:QUANTITY>
            <of:ITEM>${shipmentItem.productId!}</of:ITEM>
            <of:DISPOSITN>FIFO</of:DISPOSITN><#-- TODO: figure out if this is a reviewer order, if so set this to LIFO -->
            <n:DOCUMNTREF>
              <of:DOCTYPE>SHIPMENT</of:DOCTYPE>
              <of:DOCUMENTID>${shipment.shipmentId!}</of:DOCUMENTID>
              <of:LINENUM>${shipmentItem.shipmentItemSeqId!}</of:LINENUM>
            </n:DOCUMNTREF>
        </n:SHIPITEM>
        </#if>
        </#list>
        <#list externalIdSet! as externalId>
        <n:DOCUMNTREF>
          <of:DOCTYPE>PARTNER_SO</of:DOCTYPE>
          <of:DOCUMENTID>${externalId!?xml}</of:DOCUMENTID>
        </n:DOCUMNTREF>
        </#list>
        <#list correspondingPoIdSet! as correspondingPoId>
        <n:DOCUMNTREF>
          <of:DOCTYPE>CUST_PO</of:DOCTYPE>
          <of:DOCUMENTID>${correspondingPoId!?xml}</of:DOCUMENTID>
        </n:DOCUMNTREF>
        </#list>
        <#-- data preparation code to create the replacementReturnId; this is the returnId if the order is a return replacement order -->
        <#if replacementReturnId??>
        <n:DOCUMNTREF>
          <of:DOCTYPE>RMA</of:DOCTYPE>
          <of:DOCUMENTID>${replacementReturnId}</of:DOCUMENTID>
        </n:DOCUMNTREF>
        </#if>
        <n:DOCUMNTREF>
          <of:DOCTYPE>SO</of:DOCTYPE>
          <of:DOCUMENTID>${orderId}</of:DOCUMENTID>
        </n:DOCUMNTREF>
      </n:SHIPMENT>
    </n:PROCESS_SHIPMENT>
  </n:DATAAREA>
</n:PROCESS_SHIPMENT_001>
