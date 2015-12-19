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
<#-- variable setup -->
<#assign price = priceMap! />
<#assign productImageList = productImageList! />
<#-- end variable setup -->

<#-- virtual product javascript -->
${virtualJavaScript!}
${virtualVariantJavaScript!}
<script type="text/javascript">
//<![CDATA[
    var detailImageUrl = null;
    function setAddProductId(name) {
        document.addform.add_product_id.value = name;
        if (document.addform.quantity == null) return;
        if (name == '' || name == 'NULL' || isVirtual(name) == true) {
            document.addform.quantity.disabled = true;
            var elem = document.getElementById('product_id_display');
            var txt = document.createTextNode('');
            if(elem.hasChildNodes()) {
                elem.replaceChild(txt, elem.firstChild);
            } else {
                elem.appendChild(txt);
            }
        } else {
            document.addform.quantity.disabled = false;
            var elem = document.getElementById('product_id_display');
            var txt = document.createTextNode(name);
            if(elem.hasChildNodes()) {
                elem.replaceChild(txt, elem.firstChild);
            } else {
                elem.appendChild(txt);
            }
        }
    }
    function setVariantPrice(sku) {
        if (sku == '' || sku == 'NULL' || isVirtual(sku) == true) {
            var elem = document.getElementById('variant_price_display');
            var txt = document.createTextNode('');
            if(elem.hasChildNodes()) {
                elem.replaceChild(txt, elem.firstChild);
            } else {
                elem.appendChild(txt);
            }
        }
        else {
            var elem = document.getElementById('variant_price_display');
            var price = getVariantPrice(sku);
            var txt = document.createTextNode(price);
            if(elem.hasChildNodes()) {
                elem.replaceChild(txt, elem.firstChild);
            } else {
                elem.appendChild(txt);
            }
        }
    }
    function isVirtual(product) {
        var isVirtual = false;
        <#if virtualJavaScript??>
        for (i = 0; i < VIR.length; i++) {
            if (VIR[i] == product) {
                isVirtual = true;
            }
        }
        </#if>
        return isVirtual;
    }
    function addItem() {
       if (document.addform.add_product_id.value == 'NULL') {
           showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonPleaseSelectAllRequiredOptions}");
           return;
       } else {
           if (isVirtual(document.addform.add_product_id.value)) {
               document.location = '<@ofbizUrl>product?category_id=${categoryId!}&amp;product_id=</@ofbizUrl>' + document.addform.add_product_id.value;
               return;
           } else {
               document.addform.submit();
           }
       }
    }

    function popupDetail(specificDetailImageUrl) {
        if( specificDetailImageUrl ) {
            detailImageUrl = specificDetailImageUrl;
        }
        else {
            var defaultDetailImage = "${firstDetailImage?default(mainDetailImageUrl?default("_NONE_"))}";
            if (defaultDetailImage == null || defaultDetailImage == "null" || defaultDetailImage == "") {
               defaultDetailImage = "_NONE_";
            }

            if (detailImageUrl == null || detailImageUrl == "null") {
                detailImageUrl = defaultDetailImage;
            }
        }

        if (detailImageUrl == "_NONE_") {
            hack = document.createElement('span');
            hack.innerHTML="${uiLabelMap.CommonNoDetailImageAvailableToDisplay}";
            showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonNoDetailImageAvailableToDisplay}");
            return;
        }
        detailImageUrl = detailImageUrl.replace(/\&\#47;/g, "/");
        popUp("<@ofbizUrl>detailImage?detail=" + detailImageUrl + "</@ofbizUrl>", 'detailImage', '600', '600');
    }

    function toggleAmt(toggle) {
        if (toggle == 'Y') {
            changeObjectVisibility("add_amount", "visible");
        }

        if (toggle == 'N') {
            changeObjectVisibility("add_amount", "hidden");
        }
    }

    function findIndex(name) {
        for (i = 0; i < OPT.length; i++) {
            if (OPT[i] == name) {
                return i;
            }
        }
        return -1;
    }

    function getList(name, index, src) {
        currentFeatureIndex = findIndex(name);

        if (currentFeatureIndex == 0) {
            // set the images for the first selection
            if (IMG[index] != null) {
                if (document.images['mainImage'] != null) {
                    document.images['mainImage'].src = IMG[index];
                    detailImageUrl = DET[index];
                }
            }

            // set the drop down index for swatch selection
            document.forms["addform"].elements[name].selectedIndex = (index*1)+1;
        }

        if (currentFeatureIndex < (OPT.length-1)) {
            // eval the next list if there are more
            var selectedValue = document.forms["addform"].elements[name].options[(index*1)+1].value;
            if (index == -1) {
              <#if featureOrderFirst??>
                var Variable1 = eval("list" + "${featureOrderFirst}" + "()");
              </#if>
            } else {
                var Variable1 = eval("list" + OPT[(currentFeatureIndex+1)] + selectedValue + "()");
            }
            // set the product ID to NULL to trigger the alerts
            setAddProductId('NULL');

            // set the variant price to NULL
            setVariantPrice('NULL');
        } else {
            // this is the final selection -- locate the selected index of the last selection
            var indexSelected = document.forms["addform"].elements[name].selectedIndex;

            // using the selected index locate the sku
            var sku = document.forms["addform"].elements[name].options[indexSelected].value;
            
            // display alternative packaging dropdown
            ajaxUpdateArea("product_uom", "<@ofbizUrl>ProductUomDropDownOnly</@ofbizUrl>", "productId=" + sku);

            // set the product ID
            setAddProductId(sku);

            // set the variant price
            setVariantPrice(sku);

            // check for amount box
            toggleAmt(checkAmtReq(sku));
        }
    }

    function validate(x){
        var msg=new Array();
        msg[0]="Please use correct date format [yyyy-mm-dd]";

        var y=x.split("-");
        if(y.length!=3){ showAlert(msg[0]);return false; }
        if((y[2].length>2)||(parseInt(y[2])>31)) { showAlert(msg[0]); return false; }
        if(y[2].length==1){ y[2]="0"+y[2]; }
        if((y[1].length>2)||(parseInt(y[1])>12)){ showAlert(msg[0]); return false; }
        if(y[1].length==1){ y[1]="0"+y[1]; }
        if(y[0].length>4){ showAlert(msg[0]); return false; }
        if(y[0].length<4) {
            if(y[0].length==2) {
                y[0]="20"+y[0];
            } else {
                showAlert(msg[0]);
                return false;
            }
        }
        return (y[0]+"-"+y[1]+"-"+y[2]);
    }

    function showAlert(msg){
        showErrorAlert("${uiLabelMap.CommonErrorMessage2}", msg);
    }

    function additemSubmit(){
        <#if product.productTypeId! == "ASSET_USAGE" || product.productTypeId! == "ASSET_USAGE_OUT_IN">
        newdatevalue = validate(document.addform.reservStart.value);
        if (newdatevalue == false) {
            document.addform.reservStart.focus();
        } else {
            document.addform.reservStart.value = newdatevalue;
            document.addform.submit();
        }
        <#else>
        document.addform.submit();
        </#if>
    }

    function addShoplistSubmit(){
        <#if product.productTypeId! == "ASSET_USAGE" || product.productTypeId! == "ASSET_USAGE_OUT_IN">
        if (document.addToShoppingList.reservStartStr.value == "") {
            document.addToShoppingList.submit();
        } else {
            newdatevalue = validate(document.addToShoppingList.reservStartStr.value);
            if (newdatevalue == false) {
                document.addToShoppingList.reservStartStr.focus();
            } else {
                document.addToShoppingList.reservStartStr.value = newdatevalue;
                // document.addToShoppingList.reservStart.value = ;
                document.addToShoppingList.reservStartStr.value.slice(0,9)+" 00:00:00.000000000";
                document.addToShoppingList.submit();
            }
        }
        <#else>
        document.addToShoppingList.submit();
        </#if>
    }

    <#if product.virtualVariantMethodEnum! == "VV_FEATURETREE" && featureLists?has_content>
        function checkRadioButton() {
            var block1 = document.getElementById("addCart1");
            var block2 = document.getElementById("addCart2");
            <#list featureLists as featureList>
                <#list featureList as feature>
                    <#if feature_index == 0>
                        var myList = document.getElementById("FT${feature.productFeatureTypeId}");
                         if (myList.options[0].selected == true){
                             block1.style.display = "none";
                             block2.style.display = "block";
                             return;
                         }
                        <#break>
                    </#if>
                </#list>
            </#list>
            block1.style.display = "block";
            block2.style.display = "none";
        }
    </#if>
    
    function displayProductVirtualVariantId(variantId) {
        if(variantId){
            document.addform.product_id.value = variantId;
        }else{
            document.addform.product_id.value = '';
            variantId = '';
        }
        
        var elem = document.getElementById('product_id_display');
        var txt = document.createTextNode(variantId);
        if(elem.hasChildNodes()) {
            elem.replaceChild(txt, elem.firstChild);
        } else {
            elem.appendChild(txt);
        }
        
        var priceElem = document.getElementById('variant_price_display');
        var price = getVariantPrice(variantId);
        var priceTxt = null;
        if(price){
            priceTxt = document.createTextNode(price);
        }else{
            priceTxt = document.createTextNode('');
        }
        if(priceElem.hasChildNodes()) {
            priceElem.replaceChild(priceTxt, priceElem.firstChild);
        } else {
            priceElem.appendChild(priceTxt);
        }
    }
//]]>
$(function(){
    $('a[id^=productTag_]').click(function(){
        var id = $(this).attr('id');
        var ids = id.split('_');
        var productTagStr = ids[1];
        if (productTagStr) {
            $('#productTagStr').val(productTagStr);
            $('#productTagsearchform').submit();
        }
    });
})
 </script>

<#macro showUnavailableVarients>
  <#if unavailableVariants??>
    <ul>
      <#list unavailableVariants as prod>
        <#assign features = prod.getRelated("ProductFeatureAppl", null, null, false)/>
        <li>
          <#list features as feature>
            <em>${feature.getRelatedOne("ProductFeature", false).description}</em><#if feature_has_next>, </#if>
          </#list>
          <span>${uiLabelMap.ProductItemOutOfStock}</span>
        </li>
      </#list>
    </ul>
  </#if>
</#macro>

<div id="productdetail">
    <#assign productAdditionalImage1 = productContentWrapper.get("ADDITIONAL_IMAGE_1", "url")! />
    <#assign productAdditionalImage2 = productContentWrapper.get("ADDITIONAL_IMAGE_2", "url")! />
    <#assign productAdditionalImage3 = productContentWrapper.get("ADDITIONAL_IMAGE_3", "url")! />
    <#assign productAdditionalImage4 = productContentWrapper.get("ADDITIONAL_IMAGE_4", "url")! />
    
      <#-- Category next/previous -->
      <#if category??>
          <div id="paginationBox">
            <#if previousProductId??>
              <a href="<@ofbizCatalogAltUrl productCategoryId=categoryId! productId=previousProductId!/>" class="buttontext">${uiLabelMap.CommonPrevious}</a>&nbsp;|&nbsp;
            </#if>
            <a href="<@ofbizCatalogAltUrl productCategoryId=categoryId!/>" class="linktext">${(category.categoryName)?default(category.description)!}</a>
            <#if nextProductId??>
              &nbsp;|&nbsp;<a href="<@ofbizCatalogAltUrl productCategoryId=categoryId! productId=nextProductId!/>" class="buttontext">${uiLabelMap.CommonNext}</a>
            </#if>
          </div>
      </#if>
    
    <hr />
    <div id="productImageBox">
        <#if productImageList?has_content>
            <#-- Product image/name/price -->
            <div id="detailImageBox">
                <#assign productLargeImageUrl = productContentWrapper.get("LARGE_IMAGE_URL", "url")! />
                <#-- remove the next two lines to always display the virtual image first (virtual images must exist) -->
                <#if firstLargeImage?has_content>
                    <#assign productLargeImageUrl = firstLargeImage />
                </#if>
                <#if productLargeImageUrl?string?has_content>
                    <a href="javascript:popupDetail();"><img id="detailImage" src="<@ofbizContentUrl>${contentPathPrefix!}${productLargeImageUrl!}</@ofbizContentUrl>" name="mainImage" vspace="5" hspace="5" class="cssImgXLarge" alt="" /></a>
                    <input type="hidden" id="originalImage" name="originalImage" value="<@ofbizContentUrl>${contentPathPrefix!}${productLargeImageUrl!}</@ofbizContentUrl>" />
                </#if>
                <#if !productLargeImageUrl?string?has_content>
                    <img id="detailImage" src="/images/defaultImage.jpg" name="mainImage" alt="" />
                </#if>
            </div>
            <#-- Show Image Approved -->
            <div id="additionalImageBox">
                <#if productImageList?has_content>
                    <#list productImageList as productImage>
                        <div class="additionalImage">
                            <a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productImage.productImage}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productImage.productImageThumb}</@ofbizContentUrl>" vspace="5" hspace="5" alt="" /></a>
                        </div>
                    </#list>
                </#if>
            </div>
        <#else>
            <#-- Product image/name/price -->
            <div id="detailImageBox">
                <#assign productLargeImageUrl = productContentWrapper.get("LARGE_IMAGE_URL", "url")! />
                <#-- remove the next two lines to always display the virtual image first (virtual images must exist) -->
                <#if firstLargeImage?has_content>
                    <#assign productLargeImageUrl = firstLargeImage />
                </#if>
                <#if productLargeImageUrl?string?has_content>
                    <a href="javascript:popupDetail();"><img id="detailImage" src="<@ofbizContentUrl>${contentPathPrefix!}${productLargeImageUrl!}</@ofbizContentUrl>" name="mainImage" vspace="5" hspace="5" class="cssImgXLarge" alt="" /></a>
                    <input type="hidden" id="originalImage" name="originalImage" value="<@ofbizContentUrl>${contentPathPrefix!}${productLargeImageUrl!}</@ofbizContentUrl>" />
                </#if>
                <#if !productLargeImageUrl?string?has_content>
                    <img id="detailImage" src="/images/defaultImage.jpg" name="mainImage" alt="" />
                </#if>
            </div>
            <div id="additionalImageBox">
                <#if productAdditionalImage1?string?has_content>
                    <div class="additionalImage">
                        <a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage1}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage1}</@ofbizContentUrl>" vspace="5" hspace="5" class="cssImgXLarge" alt="" /></a>
                    </div>
                </#if>
                <#if productAdditionalImage2?string?has_content>
                    <div class="additionalImage">
                        <a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage2}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage2}</@ofbizContentUrl>" vspace="5" hspace="5" class="cssImgXLarge" alt="" /></a>
                    </div>
                </#if>
                <#if productAdditionalImage3?string?has_content>
                    <div class="additionalImage">
                        <a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage3}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage3}</@ofbizContentUrl>" vspace="5" hspace="5" class="cssImgXLarge" alt="" /></a>
                    </div>
                </#if>
                <#if productAdditionalImage4?string?has_content>
                    <div class="additionalImage">
                        <a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage4}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage4}</@ofbizContentUrl>" vspace="5" hspace="5" class="cssImgXLarge" alt="" /></a>
                    </div>
                </#if>
            </div>
        </#if>
        
        <div id="productDetailBox">
          <h2>${productContentWrapper.get("PRODUCT_NAME", "html")!}</h2>
          <div>${productContentWrapper.get("DESCRIPTION", "html")!}</div>
          <div>${product.productId!}</div>
          <#-- example of showing a certain type of feature with the product -->
          <#if sizeProductFeatureAndAppls?has_content>
            <div>
              <#if (sizeProductFeatureAndAppls?size == 1)>
                ${uiLabelMap.OrderSizeAvailableSingle}:
              <#else>
                ${uiLabelMap.OrderSizeAvailableMultiple}:
              </#if>
              <#list sizeProductFeatureAndAppls as sizeProductFeatureAndAppl>
                ${sizeProductFeatureAndAppl.description?default(sizeProductFeatureAndAppl.abbrev?default(sizeProductFeatureAndAppl.productFeatureId))}<#if sizeProductFeatureAndAppl_has_next>,</#if>
              </#list>
            </div>
          </#if>
    
          <#-- for prices:
                  - if price < competitivePrice, show competitive or "Compare At" price
                  - if price < listPrice, show list price
                  - if price < defaultPrice and defaultPrice < listPrice, show default
                  - if isSale show price with salePrice style and print "On Sale!"
          -->
          <#if price.competitivePrice?? && price.price?? && price.price &lt; price.competitivePrice>
            <div>${uiLabelMap.ProductCompareAtPrice}: <span class="basePrice"><@ofbizCurrency amount=price.competitivePrice isoCode=price.currencyUsed /></span></div>
          </#if>
          <#if price.listPrice?? && price.price?? && price.price &lt; price.listPrice>
            <div>${uiLabelMap.ProductListPrice}: <span class="basePrice"><@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed /></span></div>
          </#if>
          <#if price.listPrice?? && price.defaultPrice?? && price.price?? && price.price &lt; price.defaultPrice && price.defaultPrice &lt; price.listPrice>
            <div>${uiLabelMap.ProductRegularPrice}: <span class="basePrice"><@ofbizCurrency amount=price.defaultPrice isoCode=price.currencyUsed /></span></div>
          </#if>
          <#if price.specialPromoPrice??>
            <div>${uiLabelMap.ProductSpecialPromoPrice}: <span class="basePrice"><@ofbizCurrency amount=price.specialPromoPrice isoCode=price.currencyUsed /></span></div>
          </#if>
          <div>
            <strong>
              <#if price.isSale?? && price.isSale>
                <span class="salePrice">${uiLabelMap.OrderOnSale}!</span>
                <#assign priceStyle = "salePrice" />
              <#else>
                <#assign priceStyle = "regularPrice" />
              </#if>
                ${uiLabelMap.OrderYourPrice}: <#if "Y" = product.isVirtual!> ${uiLabelMap.CommonFrom} </#if><span class="${priceStyle}"><@ofbizCurrency amount=price.price isoCode=price.currencyUsed /></span>
                 <#if product.productTypeId! == "ASSET_USAGE" || product.productTypeId! == "ASSET_USAGE_OUT_IN">
                <#if product.reserv2ndPPPerc?? && product.reserv2ndPPPerc != 0><br /><span class="${priceStyle}">${uiLabelMap.ProductReserv2ndPPPerc}<#if !product.reservNthPPPerc?? || product.reservNthPPPerc == 0>${uiLabelMap.CommonUntil} ${product.reservMaxPersons!1}</#if> <@ofbizCurrency amount=product.reserv2ndPPPerc*price.price/100 isoCode=price.currencyUsed /></span></#if>
                <#if product.reservNthPPPerc?? &&product.reservNthPPPerc != 0><br /><span class="${priceStyle}">${uiLabelMap.ProductReservNthPPPerc} <#if !product.reserv2ndPPPerc?? || product.reserv2ndPPPerc == 0>${uiLabelMap.ProductReservSecond} <#else> ${uiLabelMap.ProductReservThird} </#if> ${uiLabelMap.CommonUntil} ${product.reservMaxPersons!1}, ${uiLabelMap.ProductEach}: <@ofbizCurrency amount=product.reservNthPPPerc*price.price/100 isoCode=price.currencyUsed /></span></#if>
                <#if (!product.reserv2ndPPPerc?? || product.reserv2ndPPPerc == 0) && (!product.reservNthPPPerc?? || product.reservNthPPPerc == 0)><br />${uiLabelMap.ProductMaximum} ${product.reservMaxPersons!1} ${uiLabelMap.ProductPersons}.</#if>
                 </#if>
             </strong>
          </div>
          <#if price.listPrice?? && price.price?? && price.price &lt; price.listPrice>
            <#assign priceSaved = price.listPrice - price.price />
            <#assign percentSaved = (priceSaved / price.listPrice) * 100 />
            <div>${uiLabelMap.OrderSave}: <span class="basePrice"><@ofbizCurrency amount=priceSaved isoCode=price.currencyUsed /> (${percentSaved?int}%)</span></div>
          </#if>
          <#-- show price details ("showPriceDetails" field can be set in the screen definition) -->
          <#if (showPriceDetails?? && showPriceDetails?default("N") == "Y")>
              <#if price.orderItemPriceInfos??>
                  <#list price.orderItemPriceInfos as orderItemPriceInfo>
                      <div>${orderItemPriceInfo.description!}</div>
                  </#list>
              </#if>
          </#if>
    
          <#-- Included quantities/pieces -->
          <#if product.piecesIncluded?? && product.piecesIncluded?long != 0>
            <div>
              ${uiLabelMap.OrderPieces}: ${product.piecesIncluded}
            </div>
          </#if>
          <#if (product.quantityIncluded?? && product.quantityIncluded != 0) || product.quantityUomId?has_content>
            <#assign quantityUom = product.getRelatedOne("QuantityUom", true)! />
            <div>
              ${uiLabelMap.CommonQuantity}: ${product.quantityIncluded!} ${((quantityUom.abbreviation)?default(product.quantityUomId))!}
            </div>
          </#if>
    
          <#if (product.productWeight?? && product.productWeight != 0) || product.weightUomId?has_content>
            <#assign weightUom = product.getRelatedOne("WeightUom", true)! />
            <div>
              ${uiLabelMap.CommonWeight}: ${product.productWeight!} ${((weightUom.abbreviation)?default(product.weightUomId))!}
            </div>
          </#if>
          <#if (product.productHeight?? && product.productHeight != 0) || product.heightUomId?has_content>
            <#assign heightUom = product.getRelatedOne("HeightUom", true)! />
            <div>
              ${uiLabelMap.CommonHeight}: ${product.productHeight!} ${((heightUom.abbreviation)?default(product.heightUomId))!}
            </div>
          </#if>
          <#if (product.productWidth?? && product.productWidth != 0) || product.widthUomId?has_content>
            <#assign widthUom = product.getRelatedOne("WidthUom", true)! />
            <div>
              ${uiLabelMap.CommonWidth}: ${product.productWidth!} ${((widthUom.abbreviation)?default(product.widthUomId))!}
            </div>
          </#if>
          <#if (product.productDepth?? && product.productDepth != 0) || product.depthUomId?has_content>
            <#assign depthUom = product.getRelatedOne("DepthUom", true)! />
            <div>
              ${uiLabelMap.CommonDepth}: ${product.productDepth!} ${((depthUom.abbreviation)?default(product.depthUomId))!}
            </div>
          </#if>
    
          <#if daysToShip??>
            <div><strong>${uiLabelMap.ProductUsuallyShipsIn} ${daysToShip} ${uiLabelMap.CommonDays}!</strong></div>
          </#if>
    
          <#-- show tell a friend details only in ecommerce application -->
          <div>&nbsp;</div>
          <div>
              <a href="javascript:popUpSmall('<@ofbizUrl>tellafriend?productId=${product.productId}<#if categoryId??>&categoryId=${categoryId}/</#if></@ofbizUrl>','tellafriend');" class="buttontext">${uiLabelMap.CommonTellAFriend}</a>
          </div>
    
          <#if disFeatureList?? && 0 &lt; disFeatureList.size()>
          <p>&nbsp;</p>
            <#list disFeatureList as currentFeature>
                <#assign disFeatureType = currentFeature.getRelatedOne("ProductFeatureType", true) />
                <div>
                    <#if disFeatureType.description??>${disFeatureType.get("description", locale)}<#else>${currentFeature.productFeatureTypeId}</#if>:&nbsp;${currentFeature.description}
                </div>
            </#list>
                <div>&nbsp;</div>
          </#if>
        </div>
    
        <div id="addItemForm">
          <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="addform"  style="margin: 0;">
          <fieldset>
            <#assign inStock = true />
            <#assign commentEnable = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("order.properties", "order.item.comment.enable")>
            <#if commentEnable.equals("Y")>
                <#assign orderItemAttr = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("order.properties", "order.item.attr.prefix")>
                ${uiLabelMap.CommonComment}&nbsp;<input type="text" name="${orderItemAttr}comment"/>
            </#if>
            <#-- Variant Selection -->
            <#if product.isVirtual!?upper_case == "Y">
              <#if product.virtualVariantMethodEnum! == "VV_FEATURETREE" && featureLists?has_content>
                <#list featureLists as featureList>
                    <#list featureList as feature>
                        <#if feature_index == 0>
                            <div>${feature.description}: <select id="FT${feature.productFeatureTypeId}" name="FT${feature.productFeatureTypeId}" onchange="javascript:checkRadioButton();">
                            <option value="select" selected="selected">${uiLabelMap.EcommerceSelectOption}</option>
                        <#else>
                            <option value="${feature.productFeatureId}">${feature.description} <#if feature.price??>(+ <@ofbizCurrency amount=feature.price?string isoCode=feature.currencyUomId />)</#if></option>
                        </#if>
                    </#list>
                    </select>
                    </div>
                </#list>
                  <input type="hidden" name="add_product_id" value="${product.productId}" />
                <div id="addCart1" style="display:none;">
                  <span style="white-space: nowrap;"><strong>${uiLabelMap.CommonQuantity}:</strong></span>&nbsp;
                  <input type="text" size="5" name="quantity" value="1" />
                  <a href="javascript:javascript:addItem();" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.OrderAddToCart}</span></a>
                  &nbsp;
                </div>
                <div id="addCart2" style="display:block;">
                  <span style="white-space: nowrap;"><strong>${uiLabelMap.CommonQuantity}:</strong></span>&nbsp;
                  <input type="text" size="5" value="1" disabled="disabled" />
                  <a href="javascript:showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonPleaseSelectAllFeaturesFirst}");" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.OrderAddToCart}</span></a>
                  &nbsp;
                </div>
              </#if>
              <#if !product.virtualVariantMethodEnum?? || product.virtualVariantMethodEnum == "VV_VARIANTTREE">
               <#if variantTree?? && (variantTree.size() &gt; 0)>
                <#list featureSet as currentType>
                  <div>
                    <select name="FT${currentType}" onchange="javascript:getList(this.name, (this.selectedIndex-1), 1);">
                      <option>${featureTypes.get(currentType)}</option>
                    </select>
                  </div>
                </#list>
                <span id="product_uom"></span>
                <input type="hidden" name="product_id" value="${product.productId}"/>
                <input type="hidden" name="add_product_id" value="NULL"/>
                <div>
                  <strong><span id="product_id_display"> </span></strong>
                  <strong><div id="variant_price_display"> </div></strong>
                </div>
              <#else>
                <input type="hidden" name="add_product_id" value="NULL"/>
                <#assign inStock = false />
              </#if>
             </#if>
            <#else>
              <input type="hidden" name="add_product_id" value="${product.productId}" />
              <#if mainProducts?has_content>
                <input type="hidden" name="product_id" value=""/>
                <select name="productVariantId" onchange="javascript:displayProductVirtualVariantId(this.value);">
                    <option value="">Select Unit Of Measure</option>
                    <#list mainProducts as mainProduct>
                        <option value="${mainProduct.productId}">${mainProduct.uomDesc} : ${mainProduct.piecesIncluded}</option>
                    </#list>
                </select><br/>
                <div>
                  <strong><span id="product_id_display"> </span></strong>
                  <strong><div id="variant_price_display"> </div></strong>
                </div>
              </#if>
              <#if (availableInventory??) && (availableInventory <= 0) && product.requireAmount?default("N") == "N">
                <#assign inStock = false />
              </#if>
            </#if>
            <#-- check to see if introductionDate hasnt passed yet -->
            <#if product.introductionDate?? && nowTimestamp.before(product.introductionDate)>
            <p>&nbsp;</p>
              <div style="color: red;">${uiLabelMap.ProductProductNotYetMadeAvailable}.</div>
            <#-- check to see if salesDiscontinuationDate has passed -->
            <#elseif product.salesDiscontinuationDate?? && nowTimestamp.after(product.salesDiscontinuationDate)>
              <div style="color: red;">${uiLabelMap.ProductProductNoLongerAvailable}.</div>
            <#-- check to see if the product requires inventory check and has inventory -->
            <#elseif product.virtualVariantMethodEnum! != "VV_FEATURETREE">
              <#if inStock>
                <#if product.requireAmount?default("N") == "Y">
                  <#assign hiddenStyle = "visible" />
                <#else>
                  <#assign hiddenStyle = "hidden"/>
                </#if>
                <div id="add_amount" class="${hiddenStyle}">
                  <span style="white-space: nowrap;"><strong>${uiLabelMap.CommonAmount}:</strong></span>&nbsp;
                  <input type="text" size="5" name="add_amount" value=""/>
                </div>
                <#if product.productTypeId! == "ASSET_USAGE" || product.productTypeId! == "ASSET_USAGE_OUT_IN">
                  <div>
                    <label>Start Date(yyyy-mm-dd)</label><@htmlTemplate.renderDateTimeField event="" action="" name="reservStart" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${startDate}" size="25" maxlength="30" id="reservStart1" dateType="date" shortDateInput=true timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                  </div>
                  <div>
                    <#--td nowrap="nowrap" align="right">Number<br />of days</td><td><input type="textt" size="4" name="reservLength"/></td></tr><tr><td>&nbsp;</td><td align="right" nowrap="nowrap">&nbsp;</td-->
                    Number of days<input type="text" size="4" name="reservLength" value=""/>
                    Number of persons<input type="text" size="4" name="reservPersons" value="2"/>
                    Number of rooms<input type="text" size="5" name="quantity" value="1"/>
                  </div>
                  <a href="javascript:addItem()" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.OrderAddToCart}</span></a>
                <#else>
                  <span><input name="quantity" id="quantity" value="1" size="4" maxLength="4" type="text" <#if product.isVirtual!?upper_case == "Y">disabled="disabled"</#if> /></span><a href="javascript:addItem()" id="addToCart" name="addToCart" class="buttontext">${uiLabelMap.OrderAddToCart}</a>
                  <@showUnavailableVarients/>
                </#if>
                <#else>
                  <#if productStore??>
                    <#if productStore.requireInventory?? && productStore.requireInventory == "N">
                      <span><input name="quantity" id="quantity" value="1" size="4" maxLength="4" type="text" <#if product.isVirtual!?upper_case == "Y">disabled="disabled"</#if> /></span><a href="javascript:addItem()" id="addToCart" name="addToCart" class="buttontext">${uiLabelMap.OrderAddToCart}</a>
                      <@showUnavailableVarients/>
                    <#else>
                      <span><input name="quantity" id="quantity" value="1" size="4" maxLength="4" type="text" disabled="disabled" /></span><a href="javascript:void(0);" disabled="disabled" class="buttontext">${uiLabelMap.OrderAddToCart}</a><br />
                      <span>${uiLabelMap.ProductItemOutOfStock}<#if product.inventoryMessage??>&mdash; ${product.inventoryMessage}</#if></span>
                    </#if>
                  </#if>
              </#if>
            </#if>
            <#if variantPriceList??>
              <#list variantPriceList as vpricing>
                <#assign variantName = vpricing.get("variantName")!>
                <#assign secondVariantName = vpricing.get("secondVariantName")!>
                <#assign minimumQuantity = vpricing.get("minimumQuantity")>
                <#if minimumQuantity &gt; 0>
                  <div>minimum order quantity for ${secondVariantName!} ${variantName!} is ${minimumQuantity!}</div>
                </#if>
              </#list>
            <#elseif minimumQuantity?? && minimumQuantity?has_content && minimumQuantity &gt; 0>
               <div>minimum order quantity for ${productContentWrapper.get("PRODUCT_NAME", "html")!} is ${minimumQuantity!}</div>
            </#if>
            </fieldset>
          </form>
        </div>
        <div>
          <#if sessionAttributes.userLogin?has_content && sessionAttributes.userLogin.userLoginId != "anonymous">
            <hr />
            <form name="addToShoppingList" method="post" action="<@ofbizUrl>addItemToShoppingList<#if requestAttributes._CURRENT_VIEW_??>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>">
              <fieldset>
              <input type="hidden" name="productId" value="${product.productId}" />
              <input type="hidden" name="product_id" value="${product.productId}" />
              <input type="hidden" name="productStoreId" value="${productStoreId}" />
              <input type="hidden" name="reservStart" value= "" />
              <select name="shoppingListId">
                <#if shoppingLists?has_content>
                  <#list shoppingLists as shoppingList>
                    <option value="${shoppingList.shoppingListId}">${shoppingList.listName}</option>
                  </#list>
                </#if>
                <option value="">---</option>
                <option value="">${uiLabelMap.OrderNewShoppingList}</option>
              </select>
              &nbsp;&nbsp;
              <#--assign nowDate = Static["org.ofbiz.base.util.UtilDateTime"].nowDateString("yyyy-MM-dd")-->
              <#if product.productTypeId! == "ASSET_USAGE">
                  &nbsp;${uiLabelMap.CommonStartDate} (yyyy-mm-dd)<@htmlTemplate.renderDateTimeField name="reservStartStr" event="" action="" value="${startDate}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="15" maxlength="30" id="reservStartStr" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>&nbsp;Number of&nbsp;days&nbsp;&nbsp;<input type="text" size="4" name="reservLength" />&nbsp;<br/>Number of&nbsp;persons&nbsp;&nbsp;<input type="text" size="4" name="reservPersons" value="1" />&nbsp;&nbsp;Qty&nbsp;&nbsp;<input type="text" size="5" name="quantity" value="1" />
              <#elseif product.productTypeId! == "ASSET_USAGE_OUT_IN">
                &nbsp;${uiLabelMap.CommonStartDate} (yyyy-mm-dd)&nbsp;&nbsp;&nbsp;<@htmlTemplate.renderDateTimeField name="reservStartStr" event="" action="" value="${startDate}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="15" maxlength="30" id="reservStartStr" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>&nbsp;&nbsp;Number of&nbsp;days&nbsp;&nbsp;<input type="text" size="4" name="reservLength" /><input type="hidden" size="4" name="reservPersons" value="1" /><br/>
                Qty&nbsp;<input type="text" size="5" name="quantity" value="1" />
              <#else>
                  <input type="text" size="5" name="quantity" value="1" />
                  <input type="hidden" name="reservStartStr" value= "" />
              </#if>
              <a href="javascript:addShoplistSubmit();" class="buttontext">${uiLabelMap.OrderAddToShoppingList}</a>
              </fieldset>
            </form>
          <#else> <br />
            ${uiLabelMap.OrderYouMust} <a href="<@ofbizUrl>checkLogin/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBeLogged}</a>
            ${uiLabelMap.OrderToAddSelectedItemsToShoppingList}.&nbsp;
          </#if>
          </div>
          <#-- Prefill first select box (virtual products only) -->
          <#if variantTree?? && 0 &lt; variantTree.size()>
            <script type="text/javascript">eval("list" + "${featureOrderFirst}" + "()");</script>
          </#if>
    
          <#-- Swatches (virtual products only) -->
          <#if variantSample?? && 0 &lt; variantSample.size()>
            <#assign imageKeys = variantSample.keySet() />
            <#assign imageMap = variantSample />
            <p>&nbsp;</p>
                <#assign maxIndex = 7 />
                <#assign indexer = 0 />
                <#list imageKeys as key>
                  <#assign swatchProduct = imageMap.get(key) />
                  <#if swatchProduct?has_content && indexer &lt; maxIndex>
                    <#assign imageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(swatchProduct, "SMALL_IMAGE_URL", request, "url")! />
                    <#if !imageUrl?string?has_content>
                      <#assign imageUrl = productContentWrapper.get("SMALL_IMAGE_URL", "url")! />
                    </#if>
                    <#if !imageUrl?string?has_content>
                      <#assign imageUrl = "/images/defaultImage.jpg" />
                    </#if>
                      <a href="javascript:getList('FT${featureOrderFirst}','${indexer}',1);" class="linktext">${key}</a>
                      <a href="javascript:getList('FT${featureOrderFirst}','${indexer}',1);"><img src="<@ofbizContentUrl>${contentPathPrefix!}${imageUrl}</@ofbizContentUrl>" class="cssImgSmall" alt="" /></a>
                      <br />
                  </#if>
                  <#assign indexer = indexer + 1 />
                </#list>
                <#if (indexer > maxIndex)>
                  <div><strong>${uiLabelMap.ProductMoreOptions}</strong></div>
                </#if>
          </#if>
    
      <#-- Digital Download Files Associated with this Product -->
      <#if downloadProductContentAndInfoList?has_content>
        <div id="download-files">
          <div>${uiLabelMap.OrderDownloadFilesTitle}:</div>
          <#list downloadProductContentAndInfoList as downloadProductContentAndInfo>
            <div>${downloadProductContentAndInfo.contentName!}<#if downloadProductContentAndInfo.description?has_content> - ${downloadProductContentAndInfo.description}</#if></div>
          </#list>
        </div>
      </#if>
    
      <#-- Long description of product -->
      <div id="long-description">
          <div>${productContentWrapper.get("LONG_DESCRIPTION", "html")!}</div>
          <div>${productContentWrapper.get("WARNINGS", "html")!}</div>
      </div>
    
      <#-- Any attributes/etc may go here -->
    
      <#-- Product Reviews -->
        <div id="reviews">
          <div>${uiLabelMap.OrderCustomerReviews}:</div>
          <#if averageRating?? && (averageRating &gt; 0) && numRatings?? && (numRatings &gt; 1)>
              <div>${uiLabelMap.OrderAverageRating}: ${averageRating} <#if numRatings??>(${uiLabelMap.CommonFrom} ${numRatings} ${uiLabelMap.OrderRatings})</#if></div>
          </#if>
    <hr />
          <#if productReviews?has_content>
            <#list productReviews as productReview>
              <#assign postedUserLogin = productReview.getRelatedOne("UserLogin", false) />
              <#assign postedPerson = postedUserLogin.getRelatedOne("Person", false)! />
                        <div><strong>${uiLabelMap.CommonBy}: </strong><#if productReview.postedAnonymous?default("N") == "Y"> ${uiLabelMap.OrderAnonymous}<#else> ${postedPerson.firstName} ${postedPerson.lastName}&nbsp;</#if></div>
                        <div><strong>${uiLabelMap.CommonAt}: </strong>${productReview.postedDateTime!}&nbsp;</div>
                        <div><strong>${uiLabelMap.OrderRanking}: </strong>${productReview.productRating!?string}</div>
                        <div>&nbsp;</div>
                        <div>${productReview.productReview!}</div>
                    <hr />
            </#list>
            <div>
                <a href="<@ofbizUrl>reviewProduct?category_id=${categoryId!}&amp;product_id=${product.productId}</@ofbizUrl>" class="linktext">${uiLabelMap.ProductReviewThisProduct}!</a>
            </div>
          <#else>
            <div>${uiLabelMap.ProductProductNotReviewedYet}.</div>
            <div>
                <a href="<@ofbizUrl>reviewProduct?category_id=${categoryId!}&amp;product_id=${product.productId}</@ofbizUrl>" class="linktext">${uiLabelMap.ProductBeTheFirstToReviewThisProduct}</a>
            </div>
        </div>
      </#if>
    <#-- Upgrades/Up-Sell/Cross-Sell -->
      <#macro associated assocProducts beforeName showName afterName formNamePrefix targetRequestName>
      <#assign pageProduct = product />
      <#assign targetRequest = "product" />
      <#if targetRequestName?has_content>
        <#assign targetRequest = targetRequestName />
      </#if>
      <#if assocProducts?has_content>
        <h2>${beforeName!}<#if showName == "Y">${productContentWrapper.get("PRODUCT_NAME", "html")!}</#if>${afterName!}</h2>
    
        <div class="productsummary-container">
        <#list assocProducts as productAssoc>
            <#if productAssoc.productId == product.productId>
                <#assign assocProductId = productAssoc.productIdTo />
            <#else>
                <#assign assocProductId = productAssoc.productId />
            </#if>
            <div>
              <a href="<@ofbizUrl>${targetRequest}/<#if categoryId??>~category_id=${categoryId}/</#if>~product_id=${assocProductId}</@ofbizUrl>" class="buttontext">
                ${assocProductId}
              </a>
            <#if productAssoc.reason?has_content>
              - <strong>${productAssoc.reason}</strong>
            </#if>
            </div>
          ${setRequestAttribute("optProductId", assocProductId)}
          ${setRequestAttribute("listIndex", listIndex)}
          ${setRequestAttribute("formNamePrefix", formNamePrefix)}
          <#if targetRequestName?has_content>
            ${setRequestAttribute("targetRequestName", targetRequestName)}
          </#if>
              ${screens.render(productsummaryScreen)}
          <#assign product = pageProduct />
          <#local listIndex = listIndex + 1 />
        </#list>
        </div>
    
        ${setRequestAttribute("optProductId", "")}
        ${setRequestAttribute("formNamePrefix", "")}
        ${setRequestAttribute("targetRequestName", "")}
      </#if>
    </#macro>
    
    <#assign productValue = product />
    <#assign listIndex = 1 />
    ${setRequestAttribute("productValue", productValue)}
    <div id="associated-products">
        <#-- also bought -->
        <@associated assocProducts=alsoBoughtProducts beforeName="" showName="N" afterName="${uiLabelMap.ProductAlsoBought}" formNamePrefix="albt" targetRequestName="" />
        <#-- obsolete -->
        <@associated assocProducts=obsoleteProducts beforeName="" showName="Y" afterName=" ${uiLabelMap.ProductObsolete}" formNamePrefix="obs" targetRequestName="" />
        <#-- cross sell -->
        <@associated assocProducts=crossSellProducts beforeName="" showName="N" afterName="${uiLabelMap.ProductCrossSell}" formNamePrefix="cssl" targetRequestName="crosssell" />
        <#-- up sell -->
        <@associated assocProducts=upSellProducts beforeName="${uiLabelMap.ProductUpSell} " showName="Y" afterName=":" formNamePrefix="upsl" targetRequestName="upsell" />
        <#-- obsolescence -->
        <@associated assocProducts=obsolenscenseProducts beforeName="" showName="Y" afterName=" ${uiLabelMap.ProductObsolescense}" formNamePrefix="obce" targetRequestName="" />
    </div>
    
    <#-- special cross/up-sell area using commonFeatureResultIds (from common feature product search) -->
    <#if comsmonFeatureResultIds?has_content>
        <h2>${uiLabelMap.ProductSimilarProducts}</h2>
    
        <div class="productsummary-container">
            <#list commonFeatureResultIds as commonFeatureResultId>
                ${setRequestAttribute("optProductId", commonFeatureResultId)}
                ${setRequestAttribute("listIndex", commonFeatureResultId_index)}
                ${setRequestAttribute("formNamePrefix", "cfeatcssl")}
                <#-- ${setRequestAttribute("targetRequestName", targetRequestName)} -->
                ${screens.render(productsummaryScreen)}
            </#list>
        </div>
    </#if>
    <div class="product-tags">
        <p class="titleProductTags"><h3>${uiLabelMap.EcommerceProductTags}</h3></p>
        <#if productTags??>
            <p class="titleAddTags"><strong>${uiLabelMap.EcommerceProductTagsDetail}:</strong></p>
            <p>
                <ul>
                    <li>
                    <#assign no = 0 />
                    <#list productTags?keys?sort as productTag>
                        <#assign tagValue = productTags.get(productTag)!/>
                        <#if tagValue?has_content>
                              <span><a href="javascript:void(0);" id="productTag_${productTag}">${productTag}</a> (${tagValue}) <#if no < (productTags.size() - 1)> | </#if></span>
                              <#assign no = no + 1 />
                        </#if>
                    </#list>
                    </li>
                </ul>
            </p>
        </#if>
        
        <p class="titleAddTags"><strong>${uiLabelMap.EcommerceAddYourTags}:</strong></p>
        <p>
            <form method="post" action="<@ofbizUrl>addProductTags</@ofbizUrl>" name="addProductTags">
                <input type="hidden" name="productId" value="${product.productId!}"/>
                <input class="inputProductTags" type="text" value="" name="productTags" id="productTags" size="40"/>
                <input class="buttonProductTags" type="submit" value="${uiLabelMap.EcommerceAddTags}" name="addTag"/>
            </form>
            <span>${uiLabelMap.EcommerceAddTagsDetail}</span>
        </p>
    </div>
    <hr />
    <form action="<@ofbizUrl>tagsearch</@ofbizUrl>" method="post" name="productTagsearchform" id="productTagsearchform">
        <input type="hidden" name="keywordTypeId" value="KWT_TAG"/>
        <input type="hidden" name="statusId" value="KW_APPROVED"/>
        <input type="hidden" name="clearSearch" value="Y"/>
        <input type="hidden" name="VIEW_SIZE" value="10"/>
        <input type="hidden" name="PAGING" value="Y"/>
        <input type="hidden" name="SEARCH_STRING" id="productTagStr"/>
    </form>
    </div>
</div>
