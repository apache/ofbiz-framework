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
 *@author     Brad Steiner (bsteiner@thehungersite.com)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
    <#if productId?exists>
        <hr class="sepbar"/>
        <div class="head2">${uiLabelMap.ProductDuplicateProduct}</div>
        <form action="<@ofbizUrl>DuplicateProduct</@ofbizUrl>" method="post" style="margin: 0;">
            <input type="hidden" name="oldProductId" value="${productId}"/>
            <div>
                <span class="tabletext">${uiLabelMap.ProductDuplicateRemoveSelectedWithNewId}:</span>
                <input type="text" class="inputBox" size="20" maxlength="20" name="productId"/>&nbsp;<input type="submit" class="smallSubmit" value="${uiLabelMap.CommonDuplicate}!"/>
            </div>
            <div class="tabletext">
                <b>${uiLabelMap.CommonDuplicate}:</b>
                ${uiLabelMap.ProductPrices}&nbsp;<input type="checkbox" class="checkBox" name="duplicatePrices" value="Y" checked="checked"/>
                ${uiLabelMap.CommonId}&nbsp;<input type="checkbox" class="checkBox" name="duplicateIDs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="duplicateContent" value="Y" checked="checked"/>
                ${uiLabelMap.ProductCategoryMembers}&nbsp;<input type="checkbox" class="checkBox" name="duplicateCategoryMembers" value="Y" checked="checked"/>
                ${uiLabelMap.ProductAssocs}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAssocs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAttributes" value="Y" checked="checked"/>
                ${uiLabelMap.ProductFeatureAppls}&nbsp;<input type="checkbox" class="checkBox" name="duplicateFeatureAppls" value="Y" checked="checked"/>
                ${uiLabelMap.ProductInventoryItems}&nbsp;<input type="checkbox" class="checkBox" name="duplicateInventoryItems" value="Y"/>
            </div>
            <div class="tabletext">
                <b>${uiLabelMap.CommonRemove}:</b>
                ${uiLabelMap.ProductPrices}&nbsp;<input type="checkbox" class="checkBox" name="removePrices" value="Y"/>
                ${uiLabelMap.CommonId}&nbsp;<input type="checkbox" class="checkBox" name="removeIDs" value="Y"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="removeContent" value="Y"/>
                ${uiLabelMap.ProductCategoryMembers}&nbsp;<input type="checkbox" class="checkBox" name="removeCategoryMembers" value="Y"/>
                ${uiLabelMap.ProductAssocs}&nbsp;<input type="checkbox" class="checkBox" name="removeAssocs" value="Y"/>
                ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="removeAttributes" value="Y"/>
                ${uiLabelMap.ProductFeatureAppls}&nbsp;<input type="checkbox" class="checkBox" name="removeFeatureAppls" value="Y"/>
                ${uiLabelMap.ProductInventoryItems}&nbsp;<input type="checkbox" class="checkBox" name="removeInventoryItems" value="Y"/>
            </div>
        </form>
        <#if product?exists && product.isVirtual?if_exists == "Y">
        <hr class="sepbar"/>
        <div class="head2">${uiLabelMap.ProductUpdateProductVariants}</div>
        <form action="<@ofbizUrl>UpdateProductVariants?productId=${productId}</@ofbizUrl>" method="post" style="margin: 0;">
            <input type="hidden" name="virtualProductId" value="${productId}"/>
            <div class="tabletext">
                <b>${uiLabelMap.ProductUpdateProductVariants}:</b>
                ${uiLabelMap.ProductRemoveBefore}&nbsp;<input type="checkbox" class="checkBox" name="removeBefore" value="Y"/>
                ${uiLabelMap.ProductPrices}&nbsp;<input type="checkbox" class="checkBox" name="duplicatePrices" value="Y" checked="checked"/>
                ${uiLabelMap.CommonId}&nbsp;<input type="checkbox" class="checkBox" name="duplicateIDs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="duplicateContent" value="Y" checked="checked"/>
                ${uiLabelMap.ProductCategoryMembers}&nbsp;<input type="checkbox" class="checkBox" name="duplicateCategoryMembers" value="Y" checked="checked"/>
                ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAttributes" value="Y" checked="checked"/>
                ${uiLabelMap.ProductFacilities}&nbsp;<input type="checkbox" class="checkBox" name="duplicateFacilities" value="Y" checked="checked"/>
                ${uiLabelMap.ProductLocations}&nbsp;<input type="checkbox" class="checkBox" name="duplicateLocations" value="Y" checked="checked"/>
            </div>
            <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonGo}"/>
        </form>
        </#if>
        <br/>
    </#if>
