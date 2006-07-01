<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *@version    $Rev$
-->

<#if quote?exists>
<form action="<@ofbizUrl>copyQuote</@ofbizUrl>" method="post" style="margin: 0;">
    <input type="hidden" name="quoteId" value="${quoteId}"/>
    <div class="tabletext">
        <b>${uiLabelMap.OrderCopyQuote}:</b>
        ${uiLabelMap.OrderOrderQuoteItems}&nbsp;<input type="checkbox" class="checkBox" name="copyQuoteItems" value="Y" checked/>
        ${uiLabelMap.OrderOrderQuoteAdjustments}&nbsp;<input type="checkbox" class="checkBox" name="copyQuoteAdjustments" value="Y" checked/>
        ${uiLabelMap.OrderOrderQuoteRoles}&nbsp;<input type="checkbox" class="checkBox" name="copyQuoteRoles" value="Y" checked/>
        ${uiLabelMap.OrderOrderQuoteAttributes}&nbsp;<input type="checkbox" class="checkBox" name="copyQuoteAttributes" value="Y" checked/>
        ${uiLabelMap.OrderOrderQuoteCoefficients}&nbsp;<input type="checkbox" class="checkBox" name="copyQuoteCoefficients" value="Y" checked/>
    </div>
    <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonCopy}"/>
</form>
</#if>
