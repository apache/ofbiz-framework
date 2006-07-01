<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
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
