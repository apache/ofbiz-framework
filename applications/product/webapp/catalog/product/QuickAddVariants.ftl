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
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
<script>
function setProductVariantId(e, value, fieldname) {
    var cform = document.selectAllForm;
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        if (element.name == fieldname) {
            if (e.checked) {
                if (element.value == null || element.value == "") {
                    element.value = value;
                }
            } else {
                element.value = "";
            }
            return;
        }
    }
}
function clickAll(e) {
    var cform = document.selectAllForm;
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];                   
        if (element.name.substring(0, 10) == "_rowSubmit" && element.checked != e.checked) {
            element.click();
        } 
    }     
}
</script>
    <#if (product.isVirtual)?if_exists != "Y">
        ${uiLabelMap.ProductWarningProductNotVirtual}
    </#if>
    
    <br/>
    <div style="float: right;">
        <div class="tabletext"><b>${uiLabelMap.ProductVariantAdd}:</b></div>
        <form action="<@ofbizUrl>addVariantsToVirtual</@ofbizUrl>" method="post" style="margin: 0;" name="addVariantsToVirtual">
            <input type="hidden" name="productId" value="${productId}"/>
            <div><span class="tabletext">${uiLabelMap.ProductVariantProductIds}:</span></div>
            <div><textarea name="variantProductIdsBag" rows="6" cols="20"></textarea></div>
            <div><input type="submit" class="smallSubmit" value="${uiLabelMap.ProductVariantAdd}"/></div>
        </form>
    </div>
    
    <#if (featureTypes.size() > 0)>
        <table border="1" cellpadding="2" cellspacing="0">
            <#assign rowCount = 0>
            <form method="post" action="<@ofbizUrl>QuickAddChosenVariants</@ofbizUrl>" name="selectAllForm">
                <input type="hidden" name="productId" value="${productId}">
                <input type="hidden" name="_useRowSubmit" value="Y">
                <input type="hidden" name="_checkGlobalScope" value="Y">
            <tr>
                <#list featureTypes as featureType>
                    <td><div class="tabletext"><b>${featureType}</b></div></td>
                </#list>
                <td><div class="tabletext"><b>${uiLabelMap.ProductNewProductCreate} !</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.ProductSequenceNum}</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.ProductExistingVariant} :</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:clickAll(this);"></div></td>
            </tr>
        
            <#assign defaultSequenceNum = 10>
            <#list featureCombinationInfos as featureCombinationInfo>
                <#assign curProductFeatureAndAppls = featureCombinationInfo.curProductFeatureAndAppls>
                <#assign existingVariantProductIds = featureCombinationInfo.existingVariantProductIds>
                <#assign defaultVariantProductId = featureCombinationInfo.defaultVariantProductId>
                <tr valign="middle">
                    <#assign productFeatureIds = "">
                    <#list curProductFeatureAndAppls as productFeatureAndAppl>
                    <td>
                        <div class="tabletext">${productFeatureAndAppl.description?if_exists}</div>
                        <#assign productFeatureIds = productFeatureIds + "|" + productFeatureAndAppl.productFeatureId>
                    </td>
                    </#list>
                    <input type="hidden" name="productFeatureIds_o_${rowCount}" value="${productFeatureIds}"/>
                    <td>
                        <input type="text" size="20" maxlength="20" name="productVariantId_o_${rowCount}" value=""/>
                    </td>
                    <td align="right">
                        <input type"text" size="5" maxlength="10" name="sequenceNum_o_${rowCount}" value="${defaultSequenceNum}"/>
                    </td>
                    <td>
                        <div class="tabletext">&nbsp;
                        <#list existingVariantProductIds as existingVariantProductId>
                            [<a href="<@ofbizUrl>EditProduct?productId=${existingVariantProductId}</@ofbizUrl>" class="buttontext">${existingVariantProductId}</a>] &nbsp;
                        </#list>
                        </div>
                    </td>
                            <td align="right">              
                              <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:setProductVariantId(this, '${defaultVariantProductId}', 'productVariantId_o_${rowCount}');">
                            </td>

                </tr>
                <#assign defaultSequenceNum = defaultSequenceNum + 10>
                <#assign rowCount = rowCount + 1>
            </#list>
<tr>
<#assign columns = featureTypes.size() + 4>
<td colspan="${columns}" align="center">
<input type="hidden" name="_rowCount" value="${rowCount}">
<input type="submit" class="smallSubmit" value="${uiLabelMap.CommonCreate}"/>
</td>
</tr>
                </form>
        </table>
    <#else>
        <div class="tabletext"><b>${uiLabelMap.ProductNoSelectableFeaturesFound}</b></div>
    </#if>
