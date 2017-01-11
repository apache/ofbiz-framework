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

<#--
     Standard fields for this template are: cardNumber, pinNumber, amount, previousAmount, processResult, responseCode
     All other fields in this template are designed to work with the values (responses) from surveyId 1001
-->

<#if giftCardNumber?has_content>
  <#assign displayNumber = "">
  <#assign numSize = giftCardNumber?length - 4>
  <#if 0 < numSize>
    <#list 0 .. numSize-1 as foo>
      <#assign displayNumber = displayNumber + "*">
    </#list>
    <#assign displayNumber = displayNumber + giftCardNumber[numSize .. numSize + 3]>
  <#else>
    <#assign displayNumber = giftCardNumber>
  </#if>
</#if>

<#if processResult>
  <#-- success -->
  <br/>
  ${uiLabelMap.EcommerceYourGiftCard} ${displayNumber} ${uiLabelMap.EcommerceYourGiftCardReloaded}
  <br/>
  ${uiLabelMap.EcommerceGiftCardNewBalance} ${amount} ${uiLabelMap.CommonFrom} ${previousAmount}
  <br/>
<#else>
  <#-- fail -->
  <br/>
  ${uiLabelMap.EcommerceGiftCardReloadFailed} ${responseCode}
  <br/>
  ${uiLabelMap.EcommerceGiftCardRefunded}
  <br/>
</#if>
