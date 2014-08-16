<?xml version="1.0" encoding="UTF-8"?>
<#compress>

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

    <#-- FreeMarker template for Fedex FDXShipRequest -->

    <FDXShipRequest xmlns:api="http://www.fedex.com/fsmapi" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="FDXShipRequest.xsd">
        <RequestHeader>
            <AccountNumber>${AccountNumber?xml}</AccountNumber>
            <MeterNumber>${MeterNumber?xml}</MeterNumber>
            <CarrierCode>${CarrierCode?xml}</CarrierCode>
        </RequestHeader>
        <ShipDate>${ShipDate?string("yyyy-MM-dd")}</ShipDate>
        <ShipTime>${ShipTime?string("hh:mm:ss")}</ShipTime>
        <DropoffType>${DropoffType?xml}</DropoffType>
        <Service>${Service?xml}</Service>
        <Packaging>${Packaging?xml}</Packaging>
        <WeightUnits>${WeightUnits?xml}</WeightUnits>
        <Weight>${Weight?xml}</Weight>
        <CurrencyCode>${CurrencyCode?xml}</CurrencyCode>
        <Origin>
            <Contact>
                <#if OriginContactPersonName??>
                    <PersonName>${OriginContactPersonName?xml}</PersonName>
                <#elseif OriginContactCompanyName??>
                    <CompanyName>${OriginContactCompanyName?xml}</CompanyName>
                </#if>
                <PhoneNumber>${OriginContactPhoneNumber?xml}</PhoneNumber>
            </Contact>
            <Address>
                <Line1>${OriginAddressLine1?xml}</Line1>
                <#if OriginAddressLine2??>
                    <Line2>${OriginAddressLine2?xml}</Line2>
                </#if>
                <City>${OriginAddressCity?xml}</City>
                <#if OriginAddressStateOrProvinceCode??>
                    <StateOrProvinceCode>${OriginAddressStateOrProvinceCode?xml}</StateOrProvinceCode>
                </#if>
                <PostalCode>${OriginAddressPostalCode?xml}</PostalCode>
                <CountryCode>${OriginAddressCountryCode}</CountryCode>
            </Address>
        </Origin>
        <Destination>
            <Contact>
                <#if DestinationContactPersonName??>
                    <PersonName>${DestinationContactPersonName?xml}</PersonName>
                <#elseif DestinationContactCompanyName??>
                    <CompanyName>${DestinationContactCompanyName?xml}</CompanyName>
                </#if>
                <PhoneNumber>${DestinationContactPhoneNumber?xml}</PhoneNumber>
            </Contact>
            <Address>
                <Line1>${DestinationAddressLine1?xml}</Line1>
                <#if DestinationAddressLine2??>
                    <Line2>${DestinationAddressLine2?xml}</Line2>
                </#if>
                <City>${DestinationAddressCity?xml}</City>
                <#if DestinationAddressStateOrProvinceCode??>
                    <StateOrProvinceCode>${DestinationAddressStateOrProvinceCode?xml}</StateOrProvinceCode>
                </#if>
                <PostalCode>${DestinationAddressPostalCode?xml}</PostalCode>
                <CountryCode>${DestinationAddressCountryCode}</CountryCode>
            </Address>
        </Destination>
        <Payment>
            <PayorType>${PayorType?xml}</PayorType>
        </Payment>
        <ReferenceInfo>
            <CustomerReference>${CustomerReference?xml}</CustomerReference>
        </ReferenceInfo>
        <#if DimensionsUnits??>
            <Dimensions>
                <#if DimensionsLength??>
                    <Length>${DimensionsLength?xml}</Length>
                </#if>
                <#if DimensionsWidth??>
                    <Width>${DimensionsWidth?xml}</Width>
                </#if>
                <#if DimensionsHeight??>
                    <Height>${DimensionsHeight?xml}</Height>
                </#if>
                <Units>${DimensionsUnits?xml}</Units>
            </Dimensions>
        </#if>
        <#if HomeDeliveryType??>
            <HomeDelivery>
                <Type>${HomeDeliveryType?xml}</Type>
                <#if HomeDeliveryDate??>
                    <Date>${HomeDeliveryDate?string("yyyy-MM-dd")}</Date>
                </#if>
                <PhoneNumber>${DestinationContactPhoneNumber?xml}</PhoneNumber>
            </HomeDelivery>
        </#if>
        <Label>
            <Type>${LabelType?xml}</Type>
            <ImageType>${LabelImageType?xml}</ImageType>
        </Label>
        <#if HomeDeliveryType??>
            <SpecialServices>
                <ResidentialDelivery>true</ResidentialDelivery>
            </SpecialServices>
        </#if>

    </FDXShipRequest>

</#compress>