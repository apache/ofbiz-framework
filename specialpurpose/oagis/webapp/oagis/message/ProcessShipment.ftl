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
<n:PROCESS_SHIPMENT_001 xmlns:n="http://www.openapplications.org/161B_PROCESS_SHIPMENT_001" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openapplications.org/161B_PROCESS_SHIPMENT_001 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/REL%201%20-%20VER%202/161B_process_shipment_005.xsd" xmlns:N1="http://www.openapplications.org/oagis_segments" xmlns:N2="http://www.openapplications.org/oagis_fields">
  <N1:CNTROLAREA>
    <N1:BSR>
      <N2:VERB>PROCESS</N2:VERB> 
      <N2:NOUN>SHIPMENT</N2:NOUN>
      <N2:REVISION>001</N2:REVISION>
    </N1:BSR>
    <N1:SENDER>
      <N2:LOGICALID>${logicalId}</N2:LOGICALID>
      <N2:COMPONENT>INVENTORY</N2:COMPONENT>
      <N2:TASK>SHIPREQUEST</N2:TASK>
      <N2:REFERENCEID>${referenceId}</N2:REFERENCEID>
      <N2:CONFIRMATION>1</N2:CONFIRMATION>
      <N2:LANGUAGE>ENG</N2:LANGUAGE>
      <N2:CODEPAGE>NONE</N2:CODEPAGE>
      <N2:AUTHID>${authId?if_exists}</N2:AUTHID>
    </N1:SENDER>
    <N1:DATETIMEISO>${sentDate?if_exists}</N1:DATETIMEISO>
  </N1:CNTROLAREA>
  <n:DATAAREA>
    <n:PROCESS_SHIPMENT>
      <n:SHIPMENT>
        <N2:DOCUMENTID>${shipment.shipmentId?if_exists}</N2:DOCUMENTID>
        <#if shipperId?has_content>
          <N2:SHIPPERID>${shipperId}</N2:SHIPPERID><#-- TODO: fill in from PartyCarrierAccount.accountNumber; make sure filter by from/thru date and PartyCarrierAccount.carrierPartyId==orderItemShipGroup.carrierPartyId; get most recent fromDate -->
        </#if>
        <N2:CARRIER>${orderItemShipGroup.carrierPartyId?if_exists}</N2:CARRIER>
        <#if shipperId?has_content>
          <N2:FRGHTTERMS>PREPAID</N2:FRGHTTERMS><#-- TODO: if SHIPPERID?has_content then set to COLLECT -->
        <#else>
          <N2:FRGHTTERMS>COLLECT</N2:FRGHTTERMS>
        </#if>
        <N2:NOTES>${orderItemShipGroup.shippingInstructions?if_exists}</N2:NOTES>
        <N2:SHIPNOTES></N2:SHIPNOTES><#-- if order was a return replacement order (associated with return), then set to RETURNLABEL otherwise leave blank -->
        <N2:TRANSMETHD>${orderItemShipGroup.shipmentMethodTypeId?if_exists}</N2:TRANSMETHD>
        <N1:PARTNER>
          <#if address?has_content>
            <N2:NAME>${address.toName?if_exists}</N2:NAME>
            <N2:PARTNRTYPE>SHIPTO</N2:PARTNRTYPE>
            <N2:CURRENCY>USD</N2:CURRENCY>
            <N1:ADDRESS>
              <N2:ADDRLINE>${address.address1?if_exists}</N2:ADDRLINE>
                <#if address.address2?exists>
                  <N2:ADDRLINE>${address.address2}</N2:ADDRLINE>
                </#if>
              <N2:CITY>${address.city?if_exists}</N2:CITY>
              <N2:COUNTRY>${address.countryGeoId?if_exists}</N2:COUNTRY>
              <N2:DESCRIPTN></N2:DESCRIPTN>
              <N2:FAX></N2:FAX>
              <N2:POSTALCODE>${address.postalCode?if_exists}</N2:POSTALCODE>
              <N2:STATEPROVN>${address.stateProvinceGeoId?if_exists}</N2:STATEPROVN>
              <N2:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode}-</#if>${telecomNumber.areaCode?if_exists}-${telecomNumber.contactNumber?if_exists}</N2:TELEPHONE>
            </N1:ADDRESS>
            <N1:CONTACT>
              <N2:NAME>${address.attnName?if_exists}</N2:NAME>
              <N2:EMAIL>${emailString?if_exists}</N2:EMAIL>
              <N2:FAX></N2:FAX>
              <N2:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode}-</#if>${telecomNumber.areaCode?if_exists}-${telecomNumber.contactNumber?if_exists}</N2:TELEPHONE>
            </N1:CONTACT>
          </#if>
        </N1:PARTNER>
        <n:SHIPITEM>
          <#list shipmentItems as shipmentItem>
            <N1:QUANTITY>
              <N2:VALUE>${shipmentItem.quantity?if_exists}</N2:VALUE>
              <N2:NUMOFDEC>0</N2:NUMOFDEC>
              <N2:SIGN>+</N2:SIGN>
              <N2:UOM>EACH</N2:UOM>
            </N1:QUANTITY>
            <N2:ITEM>${shipmentItem.productId?if_exists}</N2:ITEM>
            <N2:DISPOSITN>FIFO</N2:DISPOSITN><#-- TODO: figure out if this is a reviewer order, if so set this to LIFO -->
            <n:DOCUMNTREF>
              <N2:DOCTYPE>SHIPMENT</N2:DOCTYPE>
              <N2:DOCUMENTID>${shipment.shipmentId?if_exists}</N2:DOCUMENTID>
              <N2:LINENUM>${shipmentItem.shipmentItemSeqId?if_exists}</N2:LINENUM>
            </n:DOCUMNTREF>
          </#list> 
        </n:SHIPITEM>
        <#-- TODO: data preparation code to create the externalIdSet -->
        <#list externalIdSet?if_exists as externalId>
        <n:DOCUMNTREF>
          <N2:DOCTYPE>PARTNER_SO</N2:DOCTYPE>
          <N2:DOCUMENTID>${externalId}</N2:DOCUMENTID>
        </n:DOCUMNTREF>
        </#list>
        <#-- TODO: data preparation code to create the correspondingPoIdSet -->
        <#list correspondingPoIdSet?if_exists as correspondingPoId>
        <n:DOCUMNTREF>
          <N2:DOCTYPE>CUST_PO</N2:DOCTYPE>
          <N2:DOCUMENTID>${correspondingPoId}</N2:DOCUMENTID>
        </n:DOCUMNTREF>
        </#list>
      </n:SHIPMENT>
    </n:PROCESS_SHIPMENT>
  </n:DATAAREA>
</n:PROCESS_SHIPMENT_001>
