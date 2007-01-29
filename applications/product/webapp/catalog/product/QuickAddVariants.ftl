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
    <#if (featureTypes.size() > 0)>
        <div class="col">
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
        </div>
    <#else>
        <div class="tabletext" class="col"><b>${uiLabelMap.ProductNoSelectableFeaturesFound}</b></div>
    </#if>
    <div class="boxlink">
        <div class="tabletext"><b>${uiLabelMap.ProductVariantAdd}:</b></div>
        <form action="<@ofbizUrl>addVariantsToVirtual</@ofbizUrl>" method="post" name="addVariantsToVirtual">
            <input type="hidden" name="productId" value="${productId}"/>
            <div><span class="tabletext">${uiLabelMap.ProductVariantProductIds}:</span></div>
            <div><textarea name="variantProductIdsBag" rows="6" cols="20"></textarea></div>
            <div><input type="submit" class="smallSubmit" value="${uiLabelMap.ProductVariantAdd}"/></div>
        </form>
    </div>
