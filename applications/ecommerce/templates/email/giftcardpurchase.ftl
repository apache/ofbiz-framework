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

<#-- Three standard fields cardNumber, pinNumber and amount are available from the activation
     All other fields in this tempalte are designed to work with the values (responses)
     from surveyId 1000 - The gift card purchase survey.
 -->

<#if recipientName?exists>${recipientName},</#if>
<br/>

<#-- MyCompany.com (not a variable why?) must be adapted - JLR 1/6/5 -->
${uiLabelMap.EcommerceYouHaveBeenSent} MyCompany.com <#if senderName?exists> ${uiLabelMap.EcommerceGiftCardFrom} ${senderName}</#if>!
<br/><br/>
<#if giftMessage?has_content>
  ${uiLabelMap.OrderGiftMessage}
  <br/><br/>
  ${giftMessage}
  <br/><br/>
</#if>

<pre>
  ${uiLabelMap.EcommerceYourCardNumber} ${cardNumber?if_exists}
  ${uiLabelMap.EcommerceYourPinNumber} ${pinNumber?if_exists}
  ${uiLabelMap.EcommerceGiftAmount} ${amount?if_exists}
</pre>
