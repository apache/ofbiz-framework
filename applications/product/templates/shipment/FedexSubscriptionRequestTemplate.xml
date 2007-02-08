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
    
    <#-- FreeMarker template for Fedex FDXSubscriptionRequest -->

    <FDXSubscriptionRequest xmlns:api="http://www.fedex.com/fsmapi" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="FDXSubscriptionRequest.xsd">
        <RequestHeader>
            <AccountNumber>${AccountNumber?xml}</AccountNumber>
        </RequestHeader>
        <Contact>
            <PersonName>${PersonName?xml}</PersonName>
            <CompanyName>${CompanyName?xml}</CompanyName>
            <PhoneNumber>${PhoneNumber?xml}</PhoneNumber>
            <#if FaxNumber?exists>        
                <FaxNumber>${FaxNumber?xml}</FaxNumber>
            </#if>        
            <#if EMailAddress?exists>
                <#-- Freemarker has a problem with the E-MailAddress tag name, so the opening and closing tags need to be wrapped in the noparse directive. -->
                <#noparse><E-MailAddress></#noparse>${EMailAddress?xml}<#noparse></E-MailAddress></#noparse>
            </#if>
        </Contact>
        <Address>
            <Line1>${Line1?xml}</Line1>
            <#if Line2?exists>
                <Line2>${Line2?xml}</Line2>
            </#if>
            <City>${City?xml}</City>
            <#if StateOrProvinceCode?exists>
                <StateOrProvinceCode>${StateOrProvinceCode?xml}</StateOrProvinceCode>
            </#if>
            <PostalCode>${PostalCode?xml}</PostalCode>
            <CountryCode>${CountryCode?xml}</CountryCode>
        </Address>
    </FDXSubscriptionRequest>

</#compress>
