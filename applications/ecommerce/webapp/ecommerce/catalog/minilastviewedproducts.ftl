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
 *@since      2.2
-->

<#assign maxToShow = 4/>
<#assign lastViewedProducts = sessionAttributes.lastViewedProducts?if_exists/>
<#if lastViewedProducts?has_content>
    <#if (lastViewedProducts?size > maxToShow)><#assign limit=maxToShow/><#else><#assign limit=(lastViewedProducts?size-1)/></#if>
    <div class="screenlet">
        <div class="screenlet-header">
            <div style="float: right;">
                <a href="<@ofbizUrl>clearLastViewed</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonClear}]</a>
                <#if (lastViewedProducts?size > maxToShow)>
                    <a href="<@ofbizUrl>lastviewedproducts</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonMore}]</a>
                </#if>
            </div>
            <div class="boxhead">${uiLabelMap.EcommerceLastProducts}</div>
        </div>
        <div class="screenlet-body">
            <#list lastViewedProducts[0..limit] as productId>
                <div>
                    ${setRequestAttribute("miniProdQuantity", "1")}
                    ${setRequestAttribute("optProductId", productId)}
                    ${setRequestAttribute("miniProdFormName", "lastviewed" + productId_index + "form")}
                    ${screens.render("component://ecommerce/widget/CatalogScreens.xml#miniproductsummary")}
                </div>
                <#if productId_has_next>
                    <div><hr class='sepbar'/></div>
                </#if>
            </#list>
        </div>
    </div>
</#if>
