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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Brad Steiner (bsteiner@thehungersite.com)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
<#if productId?exists && product?exists>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductFacility}</b></div></td>
        <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductMinimumStockReorderQuantityDaysToShip}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#list productFacilities as productFacility>
        <#assign facility = productFacility.getRelatedOneCache("Facility")>
        <tr valign="middle">
            <td><div class="tabletext"><#if facility?exists>${facility.facilityName}<#else>[${productFacility.facilityId}]</#if></div></td>
            <td align="center">
                <form method="post" action="<@ofbizUrl>updateProductFacility</@ofbizUrl>" name="lineForm${productFacility_index}">
                    <input type="hidden" name="productId" value="${(productFacility.productId)?if_exists}"/>
                    <input type="hidden" name="facilityId" value="${(productFacility.facilityId)?if_exists}"/>
                    <input type="text" size="10" name="minimumStock" value="${(productFacility.minimumStock)?if_exists}" class="inputBox"/>
                    <input type="text" size="10" name="reorderQuantity" value="${(productFacility.reorderQuantity)?if_exists}" class="inputBox"/>
                    <input type="text" size="10" name="daysToShip" value="${(productFacility.daysToShip)?if_exists}" class="inputBox"/>
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
                </form>
            </td>
            <td align="center">
                <a href="<@ofbizUrl>deleteProductFacility?productId=${(productFacility.productId)?if_exists}&facilityId=${(productFacility.facilityId)?if_exists}</@ofbizUrl>" class="buttontext">
                [${uiLabelMap.CommonDelete}]</a>
            </td>
        </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>createProductFacility</@ofbizUrl>" style="margin: 0;" name="createProductFacilityForm">
        <input type="hidden" name="productId" value="${productId?if_exists}"/>
        <input type="hidden" name="useValues" value="true"/>
    
        <div class="head2">${uiLabelMap.ProductAddFacility}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductFacility}:
            <select name="facilityId" class="selectBox">
                <#list facilities as facility>
                    <option value="${(facility.facilityId)?if_exists}">${(facility.facilityName)?if_exists}</option>
                </#list>
            </select>
            ${uiLabelMap.ProductMinimumStock}:&nbsp;<input type="text" size="10" name="minimumStock" class="inputBox"/>
            ${uiLabelMap.ProductReorderQuantity}:&nbsp;<input type="text" size="10" name="reorderQuantity" class="inputBox"/>
            ${uiLabelMap.ProductDaysToShip}:&nbsp;<input type="text" size="10" name="daysToShip" class="inputBox"/>
            <input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;"/>
        </div>
    </form>
</#if>    
