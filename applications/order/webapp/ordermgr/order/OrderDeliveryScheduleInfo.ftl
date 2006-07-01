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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      2.2
-->

<#if hasPermission>
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <#if orderId?exists>
                <a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="submenutext">[${uiLabelMap.OrderViewOrder}]</a>
            </#if>
        </div>
        <div class="boxhead">${uiLabelMap.OrderScheduleDelivery}</div>
    </div>
    <div class="screenlet-body">
        <#if orderId?has_content>
          ${updatePODeliveryInfoWrapper.renderFormString()}
        <#else>
          <div class="tabletext">${uiLabelMap.OrderNoPurchaseSpecified}</div>
        </#if>
    </div>
</div>
<#else>
 <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
