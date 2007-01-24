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
<#if requestAttributes.errorMessageList?has_content><#assign errorMessageList=requestAttributes.errorMessageList></#if>
<#if requestAttributes.eventMessageList?has_content><#assign eventMessageList=requestAttributes.eventMessageList></#if>
<#if requestAttributes.serviceValidationException?exists><#assign serviceValidationException = requestAttributes.serviceValidationException></#if>
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

<#-- special error message override -->
<#if serviceValidationException?exists>
  <#assign serviceName = serviceValidationException.getServiceName()?if_exists>
  <#assign missingList = serviceValidationException.getMissingFields()?if_exists>
  <#assign extraList = serviceValidationException.getExtraFields()?if_exists>

  <#--  need if statement for EACH service (see the controller.xml file for service names) -->
  <#if serviceName?has_content && serviceName == "createPartyContactMechPurpose">
    <#-- create the inital message prefix -->
    <#assign message = "${uiLabelMap.CommonErrorMessage1}:">

    <#-- loop through all the missing fields -->
    <#list missingList as missing>
      <#--
           check for EACH required field (see the service definition)
           then append a message for the missing field; some fields may be
           and not needed; this example show ALL fields for the service.
           ** The value inside quotes must match 100% case included.
       -->

      <#if missing == "partyId">
        <#assign message = message + "<li>${uiLabelMap.CommonPartyID}</li>">
      </#if>
      <#if missing == "contactMechId">
        <#assign message = message + "<li>${uiLabelMap.CommonContactMechID}</li>">
      </#if>
      <#if missing == "contactMechPurposeTypeId">
        <#assign message = message + "<li>${uiLabelMap.CommonContactPurpose}</li>">
      </#if>
    </#list>

    <#-- this will replace the current error message with the new one -->
    <#assign errorMsgReq = message>
  </#if>
</#if>

<#-- display the error messages -->
<#if errorMessageList?has_content>
<div class="errorMessage">${uiLabelMap.CommonErrorMessage2}:</div><br/>
<ul>
  <#list errorMessageList as errorMsg>
    <li class="errorMessage">${errorMsg}</li>
  </#list>
</ul>
<br/>
</#if>
<#if eventMessageList?has_content>
<div class="eventMessage">${uiLabelMap.CommonErrorMessage3}:</div><br/>
<ul>
  <#list eventMessageList as eventMsg>
    <li class="eventMessage">${eventMsg}</li>
  </#list>
</ul>
<br/>
</#if>
