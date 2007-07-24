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
<n:RECEIVE_DELIVERY_001 xmlns:n="http://www.openapplications.org/197_receive_delivery_001" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openapplications.org/197_receive_delivery_001 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/OAG%20721/197_receive_delivery_001.xsd" xmlns:N1="http://www.openapplications.org/oagis_segments" xmlns:N2="http://www.openapplications.org/oagis_fields">
  <N1:CNTROLAREA>
    <N1:BSR>
      <N2:VERB>RECEIVE</N2:VERB>
      <N2:NOUN>DELIVERY</N2:NOUN>
      <N2:REVISION>001</N2:REVISION>
    </N1:BSR>
    <N1:SENDER>
      <N2:LOGICALID>${logicalId}</N2:LOGICALID>
      <N2:COMPONENT>INVENTORY</N2:COMPONENT>
      <N2:TASK>RMA</N2:TASK>
      <N2:REFERENCEID>${referenceId?if_exists}</N2:REFERENCEID/>
      <N2:CONFIRMATION>1</N2:CONFIRMATION>
      <N2:LANGUAGE>ENG</N2:LANGUAGE>
      <N2:CODEPAGE>NONE</N2:CODEPAGE>
      <N2:AUTHID>${authId}</N2:AUTHID>
    </N1:SENDER>
    <N1:DATETIMEISO>${sentDate?if_exists}</N1:DATETIMEISO>
  </N1:CNTROLAREA>
  <n:DATAAREA>
    <n:RECEIVE_DELIVERY>
      <n:RECEIPTHDR>
        <N1:DATETIMEISO>${entryDate?if_exists}</N1:DATETIMEISO>
        <N2:RECEIPTID>${returnId?if_exists}</N2:RECEIPTID>
        <N2:CARRIER></N2:CARRIER>
        <N2:NOTES></N2:NOTES>
        <N2:RECEIPTYPE>RMA</N2:RECEIPTYPE>
        <N1:PARTNER>
          <N2:NAME>${postalAddress.toName?if_exists}</N2:NAME>
          <N2:PARTNRTYPE>SHIPFROM</N2:PARTNRTYPE>
          <N2:CURRENCY>USD</N2:CURRENCY>
          <#if postalAddress?has_content>
            <N1:ADDRESS>
              <N2:ADDRLINE>${postalAddress.address1?if_exists}</N2:ADDRLINE>
              <#if postalAddress.address2?exists>
                <N2:ADDRLINE>${postalAddress.address2?if_exists}</N2:ADDRLINE>            
              </#if>  
              <N2:CITY>${postalAddress.city?if_exists}</N2:CITY>
              <N2:COUNTRY>${postalAddress.countryGeoId?if_exists}</N2:COUNTRY>
              <N2:FAX></N2:FAX>
              <N2:POSTALCODE>${postalAddress.postalCode?if_exists}</N2:POSTALCODE>
              <N2:STATEPROVN>${postalAddress.stateProvinceGeoId?if_exists}</N2:STATEPROVN>
              <N2:TELEPHONE>${telecomNumber.countryCode?if_exists}${telecomNumber.areaCode?if_exists}-${telecomNumber.contactNumber?if_exists}</N2:TELEPHONE>
            </N1:ADDRESS>
            <N1:CONTACT>
              <N2:NAME>${postalAddress.toName?if_exists}</N2:NAME>
              <N2:EMAIL>${emailString?if_exists}</N2:EMAIL>
              <N2:FAX></N2:FAX>
              <N2:TELEPHONE>${telecomNumber.countryCode?if_exists}${telecomNumber.areaCode?if_exists}-${telecomNumber.contactNumber?if_exists}</N2:TELEPHONE>
            </N1:CONTACT>
          </#if>
        </N1:PARTNER>
      </n:RECEIPTHDR>
      <n:RECEIPTUNT>
        <N1:QUANTITY>
          <N2:VALUE>${totalQty?if_exists}</N2:VALUE>
          <N2:NUMOFDEC>0</N2:NUMOFDEC>
          <N2:SIGN>+</N2:SIGN>
          <N2:UOM>EACH</N2:UOM>
        </N1:QUANTITY>
        <n:RECEIPTITM>
        <#list returnItems as returnItem>
          <#assign returnReason = returnItem.getRelatedOne("ReturnReason")>
          <N1:QUANTITY>
            <N2:VALUE>${returnItem.returnQuantity?if_exists}</N2:VALUE>
            <N2:NUMOFDEC>0</N2:NUMOFDEC>
            <N2:SIGN>+</N2:SIGN>
            <N2:UOM>EACH</N2:UOM>
          </N1:QUANTITY>
          <N2:ITEM>${returnItem.productId?if_exists}</N2:ITEM>
          <N2:NOTES>${returnReason.description?if_exists}</N2:NOTES>
          <N1:DOCUMNTREF>
            <N2:DOCTYPE>RMA</N2:DOCTYPE>
            <N2:DOCUMENTID>${returnId?if_exists}</N2:DOCUMENTID>
            <N2:LINENUM>${returnItem.returnItemSeqId}</N2:LINENUM>
          </N1:DOCUMNTREF>
          </#list>
          <n:INVDETAIL>
            <N2:SERIALNUM></N2:SERIALNUM>
          </n:INVDETAIL>
        </n:RECEIPTITM>
      </n:RECEIPTUNT>
    </n:RECEIVE_DELIVERY>
  </n:DATAAREA>
</n:RECEIVE_DELIVERY_001>
