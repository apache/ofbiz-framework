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
<#-- xsi:schemaLocation="http://www.openapplications.org/197_receive_delivery_001 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/OAG%20721/197_receive_delivery_001.xsd" -->
<n:RECEIVE_DELIVERY_001
    xmlns:n="http://www.openapplications.org/197_receive_delivery_001"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:os="http://www.openapplications.org/oagis_segments"
    xmlns:of="http://www.openapplications.org/oagis_fields">
    <os:CNTROLAREA>
        <os:BSR>
            <of:VERB>RECEIVE</of:VERB>
            <of:NOUN>DELIVERY</of:NOUN>
            <of:REVISION>001</of:REVISION>
        </os:BSR>
        <os:SENDER>
            <of:LOGICALID>${logicalId}</of:LOGICALID>
            <of:COMPONENT>INVENTORY</of:COMPONENT>
            <of:TASK>RMA</of:TASK>
            <of:REFERENCEID>${referenceId!}</of:REFERENCEID>
            <of:CONFIRMATION>1</of:CONFIRMATION>
            <of:LANGUAGE>ENG</of:LANGUAGE>
            <of:CODEPAGE>NONE</of:CODEPAGE>
            <of:AUTHID>${authId}</of:AUTHID>
        </os:SENDER>
        <os:DATETIMEISO>${sentDate!}</os:DATETIMEISO>
    </os:CNTROLAREA>
    <n:DATAAREA>
        <n:RECEIVE_DELIVERY>
            <n:RECEIPTHDR>
                <os:DATETIMEISO>${entryDate!}</os:DATETIMEISO>
                <of:RECEIPTID>${returnId!}</of:RECEIPTID>
                <of:CARRIER></of:CARRIER>
                <of:NOTES></of:NOTES>
                <of:RECEIPTYPE>RMA</of:RECEIPTYPE>
                <os:PARTNER>
                <#if postalAddress?has_content>
                    <#if (partyNameView.firstName)?has_content><#assign partyName = partyNameView.firstName/></#if>
                    <#if (partyNameView.middleName)?has_content><#assign partyName = partyName + " " + partyNameView.middleName/></#if>
                    <#if (partyNameView.lastName)?has_content><#assign partyName = partyName + " " + partyNameView.lastName/></#if>
                    <of:NAME>${postalAddress.toName?default(partyName)!?xml}</of:NAME>
                    <of:PARTNRTYPE>SHIPFROM</of:PARTNRTYPE>
                    <of:CURRENCY>USD</of:CURRENCY>
                    <os:ADDRESS>
                        <of:ADDRLINE>${postalAddress.address1!?xml}</of:ADDRLINE>
                        <#if postalAddress.address2?has_content>
                        <of:ADDRLINE>${postalAddress.address2!?xml}</of:ADDRLINE>
                        </#if>
                        <of:CITY>${postalAddress.city!?xml}</of:CITY>
                        <of:COUNTRY>${postalAddress.countryGeoId!}</of:COUNTRY>
                        <#-- <of:FAX></of:FAX> -->
                        <of:POSTALCODE>${postalAddress.postalCode!?xml}</of:POSTALCODE>
                        <of:STATEPROVN>${postalAddress.stateProvinceGeoId!}</of:STATEPROVN>
                        <of:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode?xml}-</#if>${telecomNumber.areaCode!?xml}-${telecomNumber.contactNumber!?xml}</of:TELEPHONE>
                    </os:ADDRESS>
                    <os:CONTACT>
                        <of:NAME>${postalAddress.attnName?default(partyName)!?xml}</of:NAME>
                        <of:EMAIL>${emailString!?xml}</of:EMAIL>
                        <#-- <of:FAX></of:FAX> -->
                        <of:TELEPHONE><#if telecomNumber.countryCode?has_content>${telecomNumber.countryCode?xml}-</#if>${telecomNumber.areaCode!?xml}-${telecomNumber.contactNumber!?xml}</of:TELEPHONE>
                    </os:CONTACT>
                </#if>
                </os:PARTNER>
            </n:RECEIPTHDR>
            <n:RECEIPTUNT>
                <os:QUANTITY>
                    <of:VALUE>${totalQty!}</of:VALUE>
                    <of:NUMOFDEC>0</of:NUMOFDEC>
                    <of:SIGN>+</of:SIGN>
                    <of:UOM>EACH</of:UOM>
                </os:QUANTITY>
                <#list returnItems as returnItem>
                    <#assign returnReason = returnItem.getRelatedOne("ReturnReason", false)/>
                    <#assign serialNumberList = serialNumberListByReturnItemSeqIdMap.get(returnItem.returnItemSeqId)!/>
                <n:RECEIPTITM>
                    <os:QUANTITY>
                        <of:VALUE>${returnItem.returnQuantity!}</of:VALUE>
                        <of:NUMOFDEC>0</of:NUMOFDEC>
                        <of:SIGN>+</of:SIGN>
                        <of:UOM>EACH</of:UOM>
                    </os:QUANTITY>
                    <of:ITEM>${returnItem.productId!}</of:ITEM>
                    <of:NOTES>${returnReason.description!?xml}</of:NOTES>
                    <os:DOCUMNTREF>
                        <of:DOCTYPE>RMA</of:DOCTYPE>
                        <of:DOCUMENTID>${returnItem.returnId}</of:DOCUMENTID>
                        <of:LINENUM>${returnItem.returnItemSeqId}</of:LINENUM>
                    </os:DOCUMNTREF>
                    <#list serialNumberList! as serialNumber>
                    <n:INVDETAIL>
                        <of:SERIALNUM>${serialNumber?xml}</of:SERIALNUM>
                    </n:INVDETAIL>
                    </#list>
                </n:RECEIPTITM>
                </#list>
            </n:RECEIPTUNT>
        </n:RECEIVE_DELIVERY>
    </n:DATAAREA>
</n:RECEIVE_DELIVERY_001>
