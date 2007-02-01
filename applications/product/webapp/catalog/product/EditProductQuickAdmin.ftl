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
<#assign externalKeyParam = "&externalLoginKey=" + requestAttributes.externalLoginKey?if_exists>

  <#if product?has_content>

<!-- First some general forms and scripts -->
<form name="removeAssocForm" action="<@ofbizUrl>quickAdminUpdateProductAssoc</@ofbizUrl>">
    <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
    <input type="hidden" name="PRODUCT_ID" value="${product.productId?if_exists}"/>
    <input type="hidden" name="PRODUCT_ID_TO" value=""/>
    <input type="hidden" name="PRODUCT_ASSOC_TYPE_ID" value="PRODUCT_VARIANT"/>
    <input type="hidden" name="FROM_DATE" value=""/>
    <input type="hidden" name="UPDATE_MODE" value="DELETE"/>
    <input type="hidden" name="useValues" value="true"/>
</form>
<form name="removeSelectable" action="<@ofbizUrl>updateProductQuickAdminDelFeatureTypes</@ofbizUrl>">
    <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
    <input type="hidden" name="productFeatureTypeId" value=""/>
</form>
<script language="JavaScript" type="text/javascript">

function removeAssoc(productIdTo, fromDate) {
    if (confirm("Are you sure you want to remove the association of " + productIdTo + "?")) {
        document.removeAssocForm.PRODUCT_ID_TO.value = productIdTo;
        document.removeAssocForm.FROM_DATE.value = fromDate;
        document.removeAssocForm.submit();
    }
}

function removeSelectable(typeString, productFeatureTypeId, productId) {
    if (confirm("Are you sure you want to remove all the selectable features of type " + typeString + "?")) {
        document.removeSelectable.productId.value = productId;
        document.removeSelectable.productFeatureTypeId.value = productFeatureTypeId;
        document.removeSelectable.submit();
    }
}

function doPublish() {
    window.open('/ecommerce/control/product?product_id=${productId?if_exists}');
    document.publish.submit();
}

</script>

    <!-- Name update section -->
    <form action="<@ofbizUrl>updateProductQuickAdminName</@ofbizUrl>" method="post" style="margin: 0;" name="editProduct">
        <input type="hidden" name="productId" value="${productId?if_exists}"/>
        <#if (product.isVirtual)?if_exists == "Y">
            <input type="hidden" name="isVirtual" value="Y"/>
        </#if>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td><span class="head2">[${productId?if_exists}]</span></td>
                <td><input type="text" class="inputBox" name="productName" size="40" maxlength="40" value="${product.productName?if_exists}"/></td>
                <td><input type="submit" value="${uiLabelMap.UpdateName}"/></td>
            </tr>
        </table>
    </form>

    <!-- ***************************************************** Selectable features section -->
    <#if (product.isVirtual)?if_exists == "Y">
        <hr/>
        <form action="<@ofbizUrl>EditProductQuickAdmin</@ofbizUrl>" method="post" style="margin: 0;" name="selectableFeatureTypeSelector">
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td colspan="2"><span class="head2">${uiLabelMap.SelectableFeatures}</span></td>
                <td colspan="2">${uiLabelMap.CommonType}
                    <select name="productFeatureTypeId" onchange="javascript:document.selectableFeatureTypeSelector.submit();" class="selectBox">
                        <option value="~~any~~">${uiLabelMap.AnyFeatureType}</option>
                        <#list featureTypes as featureType>
                            <#if (featureType.productFeatureTypeId)?if_exists == (productFeatureTypeId)?if_exists>
                                <#assign selected="selected"/>
                            <#else>
                                <#assign selected=""/>
                            </#if>
                            <option ${selected} value="${featureType.productFeatureTypeId?if_exists}">${featureType.get("description",locale)?if_exists}</option>
                        </#list>
                    </select>
                </td>
            </tr>
        </table>
        </form>
        <form action="<@ofbizUrl>updateProductQuickAdminSelFeat</@ofbizUrl>" method="post" style="margin: 0;" name="selectableFeature">
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <input type="hidden" name="productFeatureTypeId" value="${(productFeatureTypeId)?if_exists}"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td>${uiLabelMap.ProductProductId}</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>${uiLabelMap.ProductSRCH}</td>
                <td>${uiLabelMap.ProductDL}</td>
            </tr>

        <#assign idx=0/>
        <#list productAssocs as productAssoc>
            <#assign assocProduct = productAssoc.getRelatedOne("AssocProduct")/>
            <input type="hidden" name="productId${idx}" value="${assocProduct.productId?if_exists}"/>
            <tr>
                <td nowrap><a class="buttontext" href="<@ofbizUrl>EditProduct?productId=${assocProduct.productId}</@ofbizUrl>">[${assocProduct.productId?if_exists}]</a></td>
                <td width="100%"><a class="buttontext" href="<@ofbizUrl>EditProduct?productId=${assocProduct.productId}</@ofbizUrl>">${assocProduct.internalName?if_exists}</a></td>
                <td colspan="2">
                    <input class="inputBox" name="description${idx}" size="70" maxlength="100" value="${selFeatureDesc[assocProduct.productId]?if_exists}"/>
                </td>
                <#assign checked=""/>
                <#if ((assocProduct.smallImageUrl?if_exists != "") && (assocProduct.smallImageUrl?if_exists == product.smallImageUrl?if_exists) &&
                        (assocProduct.smallImageUrl?if_exists != "") && (assocProduct.smallImageUrl?if_exists == product.smallImageUrl?if_exists)) >
                    <#assign checked = "checked"/>
                </#if>
                <td><input type="radio" ${checked} name="useImages" value="${assocProduct.productId}"/></td>
                <#assign fromDate = Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productAssoc.getTimestamp("fromDate").toString())/>
                <td><a class="buttontext" href="javascript:removeAssoc('${productAssoc.productIdTo}','${fromDate}');">[x]</a></td>
            </tr>
            <#assign idx = idx + 1/>
        </#list>
            <tr>
                <td colspan="2">&nbsp;</td>
                <td>
                    <table border="0" cellpadding="0" cellspacing="0" class="tabletext">
                        <#list selectableFeatureTypes as selectableFeatureType>
                        <tr><td><a class="buttontext" href="javascript:removeSelectable('${(selectableFeatureType.get("description",locale))?if_exists}','${selectableFeatureType.productFeatureTypeId}','${product.productId}')">[x]</a>
                            <a class="buttontext" href="<@ofbizUrl>EditProductQuickAdmin?productFeatureTypeId=${(selectableFeatureType.productFeatureTypeId)?if_exists}&amp;productId=${product.productId?if_exists}</@ofbizUrl>">${(selectableFeatureType.get("description",locale))?if_exists}</a></td></tr>
                        </#list>
                    </table>
                </td>
                <td align="right">
                    <table border="0" cellpadding="0" cellspacing="0" class="tabletext">
                        <tr><td align="right"><input name="applyToAll" type="submit" value="${uiLabelMap.AddSelectableFeature}"/></td></tr>
                    </table>
                </td>
            </tr>
            </table>
        </form>
        <hr/>
    </#if>
    <#if (product.isVariant)?if_exists == "Y">
        <form action="<@ofbizUrl>updateProductQuickAdminDistFeat</@ofbizUrl>" method="post" style="margin: 0;" name="distFeature">
            <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
            <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td colspan="3"><span class="head2">${uiLabelMap.DistinguishingFeatures}</span></td>
            </tr>
            <tr>
                <td>${uiLabelMap.ProductProductId}</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
        <#assign idx=0/>
        <#list distinguishingFeatures as distinguishingFeature>
            <tr>
                <td><a href="<@ofbizUrl>quickAdminRemoveProductFeature?productId=${productId}&productFeatureId=${distinguishingFeature.productFeatureId}</@ofbizUrl>">[x]</a></td>
                <td>[${distinguishingFeature.productFeatureId}] ${productFeatureTypeLookup.get(distinguishingFeature.productFeatureId).get("description",locale)}: ${distinguishingFeature.get("description",locale)} </td>
            </tr>
        </#list>

            </table>
        </form>
    </#if>

    <!-- ***************************************************** end Selectable features section -->


    <!-- ***************************************************** Shipping dimensions section -->
    <hr/>
    <form action="<@ofbizUrl>updateProductQuickAdminShipping</@ofbizUrl>" method="post" style="margin: 0;" name="updateShipping">
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <input type="hidden" name="heightUomId" value="LEN_in"/>
        <input type="hidden" name="widthUomId" value="LEN_in"/>
        <input type="hidden" name="depthUomId" value="LEN_in"/>
        <input type="hidden" name="weightUomId" value="WT_oz"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td colspan=2><span class="head2">${uiLabelMap.ShippingDimensionsAndWeights}</span></td>
                <td>${uiLabelMap.ProductProductHeight}</td>
                <td>${uiLabelMap.ProductProductWidth}</td>
                <td>${uiLabelMap.ProductProductDepth}</td>
                <td>${uiLabelMap.ProductWeight}</td>
                <td>${uiLabelMap.ProductFlOz}</td>
                <td>${uiLabelMap.ProductML}</td>
                <td>${uiLabelMap.ProductNtWt}</td>
                <td>${uiLabelMap.ProductGrams}</td>
                <td>${uiLabelMap.ProductHZ}</td>
                <td>${uiLabelMap.ProductST}</td>
                <td>${uiLabelMap.ProductTD}</td>
            </tr>

    <#if (product.isVirtual)?if_exists == "Y">
        <#assign idx=0/>
        <#list assocProducts as assocProduct>
            <tr>
                    <td nowrap>[${assocProduct.productId?if_exists}]</td>
                    <td width="100%">${assocProduct.internalName?if_exists}</td>
                    <input type="hidden" name="productId${idx}" value="${assocProduct.productId?if_exists}"/>
                    <td><input class="inputBox" name="productHeight${idx}" size="6" maxlength="20" value="${assocProduct.productHeight?if_exists}"/></td>
                    <td><input class="inputBox" name="productWidth${idx}" size="6" maxlength="20" value="${assocProduct.productWidth?if_exists}"/></td>
                    <td><input class="inputBox" name="productDepth${idx}" size="6" maxlength="20" value="${assocProduct.productDepth?if_exists}"/></td>
                    <td><input class="inputBox" name="weight${idx}" size="6" maxlength="20" value="${assocProduct.weight?if_exists}"/></td>
                    <td><input class="inputBox" name="~floz${idx}" size="6" maxlength="20" value="${featureFloz.get(assocProduct.productId)?if_exists}"/></td>
                    <td><input class="inputBox" name="~ml${idx}" size="6" maxlength="20" value="${featureMl.get(assocProduct.productId)?if_exists}"/></td>
                    <td><input class="inputBox" name="~ntwt${idx}" size="6" maxlength="20" value="${featureNtwt.get(assocProduct.productId)?if_exists}"/></td>
                    <td><input class="inputBox" name="~grams${idx}" size="6" maxlength="20" value="${featureGrams.get(assocProduct.productId)?if_exists}"/></td>
                    <td><a class="buttontext" href="<@ofbizUrl>EditProductFeatures?productId=${assocProduct.productId}</@ofbizUrl>">[${featureHazmat.get(assocProduct.productId)?if_exists}]</a></td>
                    <td><a class="buttontext" href="<@ofbizUrl>EditProduct?productId=${assocProduct.productId}</@ofbizUrl>">${featureSalesThru.get(assocProduct.productId)?if_exists}</a></td>
                    <td><a class="buttontext" href="<@ofbizUrl>EditProductAssoc?productId=${assocProduct.productId}</@ofbizUrl>">${featureThruDate.get(assocProduct.productId)?if_exists}</a></td>
                </tr>
            <#assign idx = idx + 1/>
        </#list>
            <tr>
                <td colspan=10 align="right"><input name="applyToAll" type="submit" value="${uiLabelMap.ApplyToAll}"/>
                &nbsp;&nbsp;<input name="updateShipping" type="submit" value="${uiLabelMap.UpdateShipping}"/></td>
            </tr>
    <#else>
            <tr>
                <td>[${productId?if_exists}]</td>
                <td>${product.internalName?if_exists}</td>
                <td><input class="inputBox" name="productHeight" size="6" maxlength="20" value="${product.productHeight?if_exists}"></td>
                <td><input class="inputBox" name="productWidth" size="6" maxlength="20" value="${product.productWidth?if_exists}"></td>
                <td><input class="inputBox" name="productDepth" size="6" maxlength="20" value="${product.productDepth?if_exists}"></td>
                <td><input class="inputBox" name="weight" size="6" maxlength="20" value="${product.weight?if_exists}"></td>
                <td><input class="inputBox" name="~floz" size="6" maxlength="20" value="${floz?if_exists}"></td>
                <td><input class="inputBox" name="~ml" size="6" maxlength="20" value="${ml?if_exists}"></td>
                <td><input class="inputBox" name="~ntwt" size="6" maxlength="20" value="${ntwt?if_exists}"></td>
                <td><input class="inputBox" name="~grams" size="6" maxlength="20" value="${grams?if_exists}"></td>
                <td><a class="buttontext" href="<@ofbizUrl>EditProductFeatures?productId=${product.productId}</@ofbizUrl>">[${hazmat?if_exists}]</a></td>
                <td><a class="buttontext" href="<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>">${salesthru?if_exists}</a></td>
                <td><a class="buttontext" href="<@ofbizUrl>EditProductAssoc?productId=${product.productId}</@ofbizUrl>">${thrudate?if_exists}</a></td>
            </tr>
            <tr>
                <td colspan=10 align="right"><input type="submit" value="${uiLabelMap.UpdateShipping}"></td>
            </tr>
    </#if>

        </table>
    </form>
    <!--  **************************************************** end - Shipping dimensions section -->

    <!--  **************************************************** Standard Features section -->
    <hr/>
    <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
    <tr>
    <td>
        <form method="post" action="<@ofbizUrl>quickAdminApplyFeatureToProduct</@ofbizUrl>" style="margin: 0;" name="addFeatureById">
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <input type="hidden" name="productFeatureApplTypeId" value="STANDARD_FEATURE"/>
        <input type="hidden" name="fromDate" value="${nowTimestampString}"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td colspan=2><span class="head2">${uiLabelMap.StandardFeatures}</span></td>
            </tr>
            <#list addedFeatureTypeIds as addedFeatureTypeId>
                <tr>
                    <td align="right">${addedFeatureTypes.get(addedFeatureTypeId).description}</td>
                    <td>
                        <select name="productFeatureId" class="selectBox">
                            <option value="~~any~~">${uiLabelMap.AnyFeatureType}</option>
                        <#list featuresByType.get(addedFeatureTypeId) as feature>
                            <option value="${feature.getString("productFeatureId")}">${feature.description}</option>
                        </#list>
                        </select>
                    </td>
                </tr>
            </#list>
                <tr><td colspan=2 align="right"><input type="submit" value="${uiLabelMap.AddFeatures}"/></td></tr>

        </table>
        </form>
    </td>
    <td width="20">&nbsp;</td>
    <td valign="top">
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <#list standardFeatureAppls as standardFeatureAppl>
                <#assign featureId = standardFeatureAppl.productFeatureId/>
                <tr>
                    <td><a href='<@ofbizUrl>quickAdminRemoveFeatureFromProduct?productId=${standardFeatureAppl.productId?if_exists}&productFeatureId=${featureId?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(standardFeatureAppl.getTimestamp("fromDate").toString())}</@ofbizUrl>' class="buttontext">[x]</a></td>
                    <td>${productFeatureTypeLookup.get(featureId).description}:
                            ${standardFeatureLookup.get(featureId).description}</td>
                </tr>
            </#list>
        </table>
    </td>
    </tr>
    </table>
    <hr/>
        <form action="<@ofbizUrl>EditProductQuickAdmin</@ofbizUrl>">
        <input type="hidden" name="productFeatureTypeId" value="${(productFeatureTypeId)?if_exists}"/>
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td align="right">${uiLabelMap.FeatureTypes}</td>
                <td>
                    <select multiple name="addFeatureTypeId" class="selectBox">
                        <#list featureTypes as featureType>
                            <option value="${featureType.productFeatureTypeId?if_exists}">${featureType.get("description",locale)?if_exists}
                        </#list>
                    </select>&nbsp;
                </td>
                <td><input type="submit" value="${uiLabelMap.AddFeatureType}"/></td>
            </tr>
        </table>
        </form>
    <!--  **************************************************** end - Standard Features section -->

    <!--  **************************************************** Categories section -->
    <hr/>
    <form action="<@ofbizUrl>quickAdminAddCategories</@ofbizUrl>">
    <input type="hidden" name="fromDate" value="${nowTimestampString}"/>
    <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
    <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
    <tr>
    <td>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td align="right">${uiLabelMap.Categories}</td>
                <td>
                    <select multiple="true" name="categoryId" class="selectBox">
                        <#list allCategories as category>
                            <option value="${category.productCategoryId?if_exists}">${category.description?if_exists} [${category.productCategoryId}]</option>
                        </#list>
                    </select>&nbsp;
                </td>
            </tr>
        </table>
    <td valign="top">
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <#list productCategoryMembers as prodCatMemb>
                <#assign prodCat = prodCatMemb.getRelatedOne("ProductCategory")/>
                <tr>
                    <td><a href='<@ofbizUrl>quickAdminRemoveProductFromCategory?productId=${prodCatMemb.productId?if_exists}&amp;productCategoryId=${prodCatMemb.productCategoryId}&amp;fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(prodCatMemb.getTimestamp("fromDate").toString())}</@ofbizUrl>' class="buttontext">[x]</a></td>
                    <td>${prodCat.description?if_exists} [${prodCat.productCategoryId}]</td>
                </tr>
            </#list>
        </table>
    </td>
    </tr>
    <tr>
        <td colspan=2 align="right"><input type="submit" value="${uiLabelMap.UpdateCategories}"/></td>
    </tr>
    </table>
    </form>

    <!--  **************************************************** end - Categories section -->

    <!--  **************************************************** publish section -->
    <hr/>
    <#if (showPublish == "true")>
        <form action="<@ofbizUrl>quickAdminAddCategories</@ofbizUrl>" name="publish">
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <input type="hidden" name="categoryId" value="${allCategoryId?if_exists}"/>
        <input type="text" size="25" name="fromDate" class="inputBox"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td>
                    <a href="javascript:call_cal(document.publish.fromDate,'${nowTimestampString}');">
                        <img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/>
                    </a>
                </td>
                <td>
                    <input type=button value="${uiLabelMap.PublishAndView}" onClick="doPublish();"/>
                </td>
            </tr>
        </table>
        </form>
    <#else>
        <form action="<@ofbizUrl>quickAdminUnPublish</@ofbizUrl>" name="unpublish">
        <input type="hidden" name="productId" value="${product.productId?if_exists}"/>
        <input type="hidden" name="productCategoryId" value="${allCategoryId?if_exists}"/>
        <table border="0" cellpadding="2" cellspacing="0" class="tabletext">
            <tr>
                <td>
                    <input type="text" size="25" name="thruDate" class="inputBox"/>
                    <a href="javascript:call_cal(document.unpublish.thruDate,'${nowTimestampString}');">
                        <img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/>
                    </a>
                </td>
                <td><input type="submit" value="${uiLabelMap.RemoveFromSite}"/></td>
            </tr>
        </table>
        </form>
    </#if>


    <!--  **************************************************** end - publish section -->
    
  <#else>
    <h3>${uiLabelMap.ProductProductNotFound} [${productId?if_exists}]</h3>
  </#if>
