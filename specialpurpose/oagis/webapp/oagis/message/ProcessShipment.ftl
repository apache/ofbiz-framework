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
<n:PROCESS_SHIPMENT_001 
    xmlns:n="http://www.openapplications.org/161B_PROCESS_SHIPMENT_001" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	xsi:schemaLocation="http://www.openapplications.org/161B_PROCESS_SHIPMENT_001 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/REL%201%20-%20VER%202/161B_process_shipment_005.xsd" 
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
      <of:AUTHID>${authId?if_exists}</of:AUTHID>
    </os:SENDER>
    <os:DATETIMEISO>${sentDate?if_exists}</os:DATETIMEISO>
  </os:CNTROLAREA>
  <n:DATAAREA>
    <n:PROCESS_SHIPMENT>
      <n:SHIPMENT>
        <of:DOCUMENTID>${shipment.shipmentId?if_exists}</of:DOCUMENTID>
        <#if shipperId?has_content>
          <of:SHIPPERID>${shipperId}</of:SHIPPERID><#-- TODO: fill in from PartyCarrierAccount.accountNumber; make sure filter by from/thru date and PartyCarrierAccount.carrierPartyId==orderItemShipGroup.carrierPartyId; get most recent fromDate -->
        </#if>
        <of:CARRIER>${orderItemShipGroup.carrierPartyId?if_exists}</of:CARRIER>
        <#if shipperId?has_content>
          <of:FRGHTTERMS>COLLECT</of:FRGHTTERMS><#-- TODO: if SHIPPERID?has_content then set to COLLECT -->
        <#else>
          <of:FRGHTTERMS>PREPAID</of:FRGHTTERMS>
        </#if>
        <of:NOTES>${orderItemShipGroup.shippingInstructions?if_exists}</of:NOTES>
        <of:SHIPNOTES>${shipnotes?if_exists}</of:SHIPNOTES><#-- if order was a return replacement order (associated with return), then set to RETURNLABEL otherwise leave blank -->
        <of:TRANSMETHD>${orderItemShipGroup.shipmentMethodTypeId?if_exists}</of:TRANSMETHD>
        <os:PARTNER>
          <#if address?has_content>
            <of:NAME>${address.toName?if_exists}</of:NAME>
            <of:PARTNRTYPE>SHIPTO</of:PARTNRTYPE>
            <of:CURRENCY>USD</of:CURRENCY>
            <os:ADDRESS>
              <of:ADDRLINE>${address.address1?if_exists}</of:ADDRLINE>
                <#if address.address2?exists>
                  <of:ADDRLINE>${address.address2}</of:ADDRLINE>
                </#if>
              <of:CITY>${address.city?if_exists}</of:CITY>
              <of:COUNTRY>${address.countryGeoId?if_exists}</of:COUNTRY>
              <of:DESCRIPTN></of:DESCRIPTN>
              <of:FAX></of:FAX>
              <of:POSTALCODE>${address.postalCode?if_exists}</of:POSTALCODE>
              <of:STATEPROVN>${address.stateProvinceGeoId?if_exists}</of:STATEPROVN>
              <of:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode}-</#if>${telecomNumber.areaCode?if_exists}-${telecomNumber.contactNumber?if_exists}</of:TELEPHONE>
            </os:ADDRESS>
            <os:CONTACT>
              <of:NAME>${address.toName?if_exists}</of:NAME>
              <of:EMAIL>${emailString?if_exists}</of:EMAIL>
              <of:FAX></of:FAX>
              <of:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode}-</#if>${telecomNumber.areaCode?if_exists}-${telecomNumber.contactNumber?if_exists}</of:TELEPHONE>
            </os:CONTACT>
          </#if>
        </os:PARTNER>
        <n:SHIPITEM>
          <#list shipmentItems as shipmentItem>
            <os:QUANTITY>
              <of:VALUE>${shipmentItem.quantity?if_exists}</of:VALUE>
              <of:NUMOFDEC>0</of:NUMOFDEC>
              <of:SIGN>+</of:SIGN>
              <of:UOM>EACH</of:UOM>
            </os:QUANTITY>
            <of:ITEM>${shipmentItem.productId?if_exists}</of:ITEM>
            <of:DISPOSITN>FIFO</of:DISPOSITN><#-- TODO: figure out if this is a reviewer order, if so set this to LIFO -->
            <n:DOCUMNTREF>
              <of:DOCTYPE>SHIPMENT</of:DOCTYPE>
              <of:DOCUMENTID>${shipment.shipmentId?if_exists}</of:DOCUMENTID>
              <of:LINENUM>${shipmentItem.shipmentItemSeqId?if_exists}</of:LINENUM>
            </n:DOCUMNTREF>
          </#list> 
        </n:SHIPITEM>
        <#-- TODO: data preparation code to create the externalIdSet -->
        <#list externalIdSet?if_exists as externalId>
        <n:DOCUMNTREF>
          <of:DOCTYPE>PARTNER_SO</of:DOCTYPE>
          <of:DOCUMENTID>${externalId?if_exists}</of:DOCUMENTID>
        </n:DOCUMNTREF>
        </#list>
        <#-- TODO: data preparation code to create the correspondingPoIdSet -->
        <#list correspondingPoIdSet?if_exists as correspondingPoId>
        <n:DOCUMNTREF>
          <of:DOCTYPE>CUST_PO</of:DOCTYPE>
          <of:DOCUMENTID>${correspondingPoId?if_exists}</of:DOCUMENTID>
        </n:DOCUMNTREF>
        </#list>
      </n:SHIPMENT>
    </n:PROCESS_SHIPMENT>
  </n:DATAAREA>
</n:PROCESS_SHIPMENT_001>
