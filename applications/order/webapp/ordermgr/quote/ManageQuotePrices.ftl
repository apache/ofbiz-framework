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

<#list quoteCoefficients as quoteCoefficient>
    <div class="tabletext">${quoteCoefficient.coeffName}:&nbsp;${quoteCoefficient.coeffValue}</div>
</#list>
<br/>
<div class="tabletext">${uiLabelMap.CommonTotalCostMult}:&nbsp;${costMult}</div>
<div class="tabletext">${uiLabelMap.CommonTotalCostToPriceMult}:&nbsp;${costToPriceMult}</div>
<br/>

<form action="" method="post" style="margin: 0;">
    <div class="tableheadtext">
        ${uiLabelMap.CommonSelectAll}&nbsp;<input type="checkbox" class="checkBox" name="selectAll" value="Y" onClick="javascript:toggleAll(this, 'ManageQuotePrices');"/>
    </div>
</form>
