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
<#-- end variable setup -->

<#-- virtual product javascript -->
${virtualJavaScript!}
${virtualVariantJavaScript!}
<script type="application/javascript">
    var detailImageUrl = null;
    function setAddProductId2(sku, with_bak) {
        document.addform.add_product_id.value = sku;
        var disable = (sku == '' || sku == 'NULL' || isVirtual(sku) == true);
        if (document.addform.quantity != null) {
            document.addform.quantity.disabled = disable;
        }
        var txt = disable ? '' : sku;
        $('#product_id_display').text(txt);
        if (with_bak) {
            document.addform.product_id_bak.value = txt;
        }
    }
    function setVariantPrice2(sku) {
        var disable = (sku == '' || sku == 'NULL' || isVirtual(sku) == true);
        var txt = disable ? '' : getVariantPrice2(sku);
        $('#variant_price_display').text(txt || '');
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

    function popupDetail() {
        var defaultDetailImage = "${firstDetailImage?default(mainDetailImageUrl?default("_NONE_"))}";
        if (defaultDetailImage == null || "null" == defaultDetailImage || "" == defaultDetailImage) {
            defaultDetailImage = "_NONE_";
        }

        if (detailImageUrl == null || "null" == detailImageUrl) {
            detailImageUrl = defaultDetailImage;
        }

        if ("_NONE_" == detailImageUrl) {
            hack = document.createElement('span');
            hack.innerHTML="${uiLabelMap.CommonNoDetailImageAvailableToDisplay}";
            showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonNoDetailImageAvailableToDisplay}");
            return;
        }
        detailImageUrl = detailImageUrl.replace(/\&\#47;/g, "/");
        popUp("<@ofbizUrl>detailImage?detail=" + detailImageUrl + "</@ofbizUrl>", 'detailImage', '400', '550');
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

            // reset uom select
            $('#product_uom').text('');

            // set the product ID to NULL to trigger the alerts
            setAddProductId2('NULL');

            // set the variant price to NULL
            setVariantPrice2('NULL');
        } else {
            // this is the final selection -- locate the selected index of the last selection
            var indexSelected = document.forms["addform"].elements[name].selectedIndex;

            // using the selected index locate the sku
            var sku = document.forms["addform"].elements[name].options[indexSelected].value;

            // display alternative packaging dropdown
            ajaxUpdateArea("product_uom", "<@ofbizUrl>ProductUomDropDownOnly</@ofbizUrl>", "productId=" + sku);

            // set the product ID
            setAddProductId2(sku, true);

            // set the variant price
            setVariantPrice2(sku);

            // check for amount box
            toggleAmt(checkAmtReq2(sku));
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

    function showAlert(msg) {
        showErrorAlert("${uiLabelMap.CommonErrorMessage2}", msg);
    }

    function additemSubmit(){
        <#if "ASSET_USAGE" == product.productTypeId! || "ASSET_USAGE_OUT_IN" == product.productTypeId!>
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
        <#if "ASSET_USAGE" == product.productTypeId! || "ASSET_USAGE_OUT_IN" == product.productTypeId!>
        if ("" == document.addToShoppingList.reservStartStr.value) {
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

    <#if "VV_FEATURETREE" == product.virtualVariantMethodEnum! && featureLists?has_content>
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
 </script>

${screens.render("component://order/widget/ordermgr/OrderEntryCatalogScreens.xml#productvariantjs")}
${variantInfoJavaScript!}
<div id="productdetail">

<table border="0" cellpadding="2" cellspacing="0" width="100%">
  <#-- Category next/previous -->
  <#if category??>
    <tr>
      <td colspan="2" align="right">
        <#if previousProductId??>
          <a href="<@ofbizUrl>product/~category_id=${categoryId!}/~product_id=${previousProductId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>&nbsp;|&nbsp;
        </#if>
        <a href="<@ofbizUrl>category/~category_id=${categoryId!}</@ofbizUrl>" class="linktext">${(category.categoryName)?default(category.description)!}</a>
        <#if nextProductId??>
          &nbsp;|&nbsp;<a href="<@ofbizUrl>product/~category_id=${categoryId!}/~product_id=${nextProductId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
      </td>
    </tr>
  </#if>

  <tr><td colspan="2"><hr /></td></tr>

  <#-- Product image/name/price -->
  <tr>
    <td valign="top" width="0">
      <#assign productLargeImageUrl = productContentWrapper.get("LARGE_IMAGE_URL", "url")!>
      <#-- remove the next two lines to always display the virtual image first (virtual images must exist) -->
      <#if firstLargeImage?has_content>
        <#assign productLargeImageUrl = firstLargeImage>
      </#if>
      <#if productLargeImageUrl?string?has_content>
        <a href="javascript:popupDetail();"><img src="<@ofbizContentUrl>${contentPathPrefix!}${productLargeImageUrl!}</@ofbizContentUrl>" name="mainImage" vspace="5" hspace="5" class='cssImgLarge' alt="" /></a>
      </#if>
    </td>
    <td align="right" valign="top">
      <h2>${productContentWrapper.get("PRODUCT_NAME", "html")!}</h2>
      <div>${productContentWrapper.get("DESCRIPTION", "html")!}</div>
      <div><b>${product.productId!}</b></div>
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
      <#if price.competitivePrice?? && price.price?? && price.price?double < price.competitivePrice?double>
        <div>${uiLabelMap.ProductCompareAtPrice}: <span class="basePrice"><@ofbizCurrency amount=price.competitivePrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <#if price.listPrice?? && price.price?? && price.price?double < price.listPrice?double>
        <div>${uiLabelMap.ProductListPrice}: <span class="basePrice"><@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <#if price.listPrice?? && price.defaultPrice?? && price.price?? && price.price?double < price.defaultPrice?double && price.defaultPrice?double < price.listPrice?double>
        <div>${uiLabelMap.ProductRegularPrice}: <span class="basePrice"><@ofbizCurrency amount=price.defaultPrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <#if price.specialPromoPrice??>
        <div>${uiLabelMap.ProductSpecialPromoPrice}: <span class="basePrice"><@ofbizCurrency amount=price.specialPromoPrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <div>
        <b>
          <#if price.isSale?? && price.isSale>
            <span class="salePrice">${uiLabelMap.OrderOnSale}!</span>
            <#assign priceStyle = "salePrice">
          <#else>
            <#assign priceStyle = "regularPrice">
          </#if>
            ${uiLabelMap.OrderYourPrice}: <#if "Y" = product.isVirtual!> ${uiLabelMap.CommonFrom} </#if><span class="${priceStyle}"><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></span>
             <#if "ASSET_USAGE" == product.productTypeId! || "ASSET_USAGE_OUT_IN" == product.productTypeId!>
            <#if product.reserv2ndPPPerc?? && product.reserv2ndPPPerc != 0><br /><span class="${priceStyle}">${uiLabelMap.ProductReserv2ndPPPerc}<#if !product.reservNthPPPerc?? || product.reservNthPPPerc == 0>${uiLabelMap.CommonUntil} ${product.reservMaxPersons!1}</#if> <@ofbizCurrency amount=product.reserv2ndPPPerc*price.price/100 isoCode=price.currencyUsed/></span></#if>
            <#if product.reservNthPPPerc?? &&product.reservNthPPPerc != 0><br /><span class="${priceStyle}">${uiLabelMap.ProductReservNthPPPerc} <#if !product.reserv2ndPPPerc?? || product.reserv2ndPPPerc == 0>${uiLabelMap.ProductReservSecond} <#else> ${uiLabelMap.ProductReservThird} </#if> ${uiLabelMap.CommonUntil} ${product.reservMaxPersons!1}, ${uiLabelMap.ProductEach}: <@ofbizCurrency amount=product.reservNthPPPerc*price.price/100 isoCode=price.currencyUsed/></span></#if>
            <#if (!product.reserv2ndPPPerc?? || product.reserv2ndPPPerc == 0) && (!product.reservNthPPPerc?? || product.reservNthPPPerc == 0)><br />${uiLabelMap.ProductMaximum} ${product.reservMaxPersons!1} ${uiLabelMap.ProductPersons}.</#if>
             </#if>
         </b>
      </div>
      <#if price.listPrice?? && price.price?? && price.price?double < price.listPrice?double>
        <#assign priceSaved = price.listPrice?double - price.price?double>
        <#assign percentSaved = (priceSaved?double / price.listPrice?double) * 100>
        <div>${uiLabelMap.OrderSave}: <span class="basePrice"><@ofbizCurrency amount=priceSaved isoCode=price.currencyUsed/> (${percentSaved?int}%)</span></div>
      </#if>
      <#-- show price details ("showPriceDetails" field can be set in the screen definition) -->
      <#if (showPriceDetails?? && "Y" == showPriceDetails?default("N"))>
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
      <#if (product.quantityIncluded?? && product.quantityIncluded?double != 0) || product.quantityUomId?has_content>
        <#assign quantityUom = product.getRelatedOne("QuantityUom", true)!/>
        <div>
          ${uiLabelMap.CommonQuantity}: ${product.quantityIncluded!} ${((quantityUom.abbreviation)?default(product.quantityUomId))!}
        </div>
      </#if>

      <#if (product.productWeight?? && product.productWeight?double != 0) || product.weightUomId?has_content>
        <#assign weightUom = product.getRelatedOne("WeightUom", true)!/>
        <div>
          ${uiLabelMap.CommonWeight}: ${product.productWeight!} ${((weightUom.abbreviation)?default(product.weightUomId))!}
        </div>
      </#if>
      <#if (product.productHeight?? && product.productHeight?double != 0) || product.heightUomId?has_content>
        <#assign heightUom = product.getRelatedOne("HeightUom", true)!/>
        <div>
          ${uiLabelMap.CommonHeight}: ${product.productHeight!} ${((heightUom.abbreviation)?default(product.heightUomId))!}
        </div>
      </#if>
      <#if (product.productWidth?? && product.productWidth?double != 0) || product.widthUomId?has_content>
        <#assign widthUom = product.getRelatedOne("WidthUom", true)!/>
        <div>
          ${uiLabelMap.CommonWidth}: ${product.productWidth!} ${((widthUom.abbreviation)?default(product.widthUomId))!}
        </div>
      </#if>
      <#if (product.productDepth?? && product.productDepth?double != 0) || product.depthUomId?has_content>
        <#assign depthUom = product.getRelatedOne("DepthUom", true)!/>
        <div>
          ${uiLabelMap.CommonDepth}: ${product.productDepth!} ${((depthUom.abbreviation)?default(product.depthUomId))!}
        </div>
      </#if>

      <#if daysToShip??>
        <div><b>${uiLabelMap.ProductUsuallyShipsIn} <font color="red">${daysToShip}</font> ${uiLabelMap.CommonDays}!<b></div>
      </#if>

      <#if disFeatureList?? && 0 < disFeatureList.size()>
      <p>&nbsp;</p>
        <#list disFeatureList as currentFeature>
            <#assign disFeatureType = currentFeature.getRelatedOne("ProductFeatureType", true)/>
            <div>
                <#if disFeatureType.description??>${disFeatureType.get("description", locale)}<#else>${currentFeature.productFeatureTypeId}</#if>:&nbsp;${currentFeature.description}
            </div>
        </#list>
            <div>&nbsp;</div>
      </#if>

      <form method="post" action="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_??>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="addform"  style="margin: 0;">
        <#if requestAttributes.paramMap?has_content>
          <input type="hidden" name="itemComment" value="${requestAttributes.paramMap.itemComment!}" />
          <input type="hidden" name="useAsDefaultComment" value="${requestAttributes.paramMap.useAsDefaultComment!}" />
          <input type="hidden" name="shipBeforeDate" value="${requestAttributes.paramMap.shipBeforeDate!}" />
          <input type="hidden" name="shipAfterDate" value="${requestAttributes.paramMap.shipAfterDate!}" />
          <input type="hidden" name="itemDesiredDeliveryDate" value="${requestAttributes.paramMap.itemDesiredDeliveryDate!}" />
          <input type="hidden" name="useAsDefaultDesiredDeliveryDate" value="${requestAttributes.paramMap.useAsDefaultDesiredDeliveryDate!}" />
        </#if>
        <#assign inStock = true>
        <#-- Variant Selection -->
        <#if "Y" == product.isVirtual!?upper_case>
          <#if "VV_FEATURETREE" == product.virtualVariantMethodEnum! && featureLists?has_content>
            <#list featureLists as featureList>
                <#list featureList as feature>
                    <#if feature_index == 0>
                        <div>${feature.description}: <select id="FT${feature.productFeatureTypeId}" name="FT${feature.productFeatureTypeId}" onchange="javascript:checkRadioButton();">
                        <option value="select" selected="selected"> select option </option>
                    <#else>
                        <option value="${feature.productFeatureId}">${feature.description} <#if feature.price??>(+ <@ofbizCurrency amount=feature.price?string isoCode=feature.currencyUomId/>)</#if></option>
                    </#if>
                </#list>
                </select>
                </div>
            </#list>
              <input type="hidden" name="product_id" value="${product.productId}"/>
              <input type="hidden" name="add_product_id" value="${product.productId}"/>
            <div id="addCart1" style="display:none;>
              <span style="white-space: nowrap;"><b>${uiLabelMap.CommonQuantity}:</b></span>&nbsp;
              <input type="text" size="5" name="quantity" value="1"/>
              <a href="javascript:javascript:addItem();" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.OrderAddToCart}</span></a>
              &nbsp;
            </div>
            <div id="addCart2" style="display:block;>
              <span style="white-space: nowrap;"><b>${uiLabelMap.CommonQuantity}:</b></span>&nbsp;
              <input type="text" size="5" value="1" disabled="disabled"/>
              <a href="javascript:showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonPleaseSelectAllFeaturesFirst}");" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.OrderAddToCart}</span></a>
              &nbsp;
            </div>
          </#if>
          <#if !product.virtualVariantMethodEnum?? || "VV_VARIANTTREE" == product.virtualVariantMethodEnum>
           <#if variantTree?? && (variantTree.size() > 0)>
            <#list featureSet as currentType>
              <div>
                <select name="FT${currentType}" onchange="javascript:getList(this.name, (this.selectedIndex-1), 1);">
                  <option>${featureTypes.get(currentType)}</option>
                </select>
              </div>
            </#list>
            <span id="product_uom"></span><br/>
            <input type="hidden" name="product_id_bak" value=""/>
            <div class="variant-price" style="display: inline-block;">
                <strong><span id="product_id_display" class="product_id_display"> </span></strong>
                <strong><span id="variant_price_display" class="variant_price_display"> </span></strong>
            </div>
            <input type="hidden" name="product_id" value="${product.productId}"/>
            <input type="hidden" name="add_product_id" value="NULL"/>
          <#else>
            <input type="hidden" name="product_id" value="${product.productId}"/>
            <input type="hidden" name="add_product_id" value="NULL"/>
            <div><b>${uiLabelMap.ProductItemOutOfStock}.</b></div>
            <#assign inStock = false>
          </#if>
         </#if>
        <#else>
          <input type="hidden" name="product_id" value="${product.productId}"/>
          <input type="hidden" name="add_product_id" value="${product.productId}"/>
          <#if mainProducts?has_content>
            <select name="productVariantId" class="form-control" onchange="javascript:variantUomSelection(this);">
              <option value="">${uiLabelMap.CommonSelect} ${uiLabelMap.ProductUnitOfMeasure}</option>
              <#list mainProducts as mainProduct>
                <option value="${mainProduct.productId}">${mainProduct.uomDesc} : ${mainProduct.piecesIncluded}</option>
              </#list>
            </select><br/>
            <input type="hidden" name="product_id_bak" value="${product.productId}"/>
            <div class="variant-price" style="display: inline-block;">
                <strong><span class="product_id_display"> </span></strong>
                <strong><span class="variant_price_display"> </span></strong>
            </div>
          </#if>
          <#if productStoreId??>
            <#assign isStoreInventoryNotAvailable = !(Static["org.apache.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryAvailable(request, product, 1.0?double))>
            <#assign isStoreInventoryRequired = Static["org.apache.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryRequired(request, product)>
            <#if isStoreInventoryNotAvailable>
              <#if isStoreInventoryRequired>
                <div><b>${uiLabelMap.ProductItemOutOfStock}.</b></div>
                <#assign inStock = false>
              <#else>
                <div><b>${product.inventoryMessage!}</b></div>
              </#if>
            </#if>
          </#if>
        </#if>
      <div>
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
            <#if "Y" == product.requireAmount?default("N")>
              <#assign hiddenStyle = "visible">
            <#else>
              <#assign hiddenStyle = "hidden">
            </#if>
            <div id="add_amount" class="${hiddenStyle}">
              <span style="white-space: nowrap;"><b>${uiLabelMap.CommonAmount}:</b></span>&nbsp;
              <input type="text" size="5" name="add_amount" value=""/>
            </div>
            <#if "ASSET_USAGE" == product.productTypeId! || "ASSET_USAGE_OUT_IN" == product.productTypeId!>
                <table width="100%"><tr>
                    <@htmlTemplate.renderDateTimeField name="reservStart" event="" action="" value="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="startDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <@htmlTemplate.renderDateTimeField name="reservEnd" event="" action="" value="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="endDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                  <tr>
                    <#if (product.reservMaxPersons!)?is_number>
                      <td nowrap="nowrap" align="right">Number of persons</td>
                      <td><input type="text" size="4" name="reservPersons" value="2"/></td>
                    </#if>
                      <td nowrap="nowrap" align="right">${uiLabelMap.CommonQuantity}</td>
                      <td><input type="text" size="5" name="quantity" value="1"/></td>
                  </tr>
                </table>
            <#else>
                <input type="text" size="5" name="quantity" value="1"<#if "Y" == product.isVirtual!?upper_case> disabled="disabled"</#if>/>
            </#if>
            <#-- This calls addItem() so that variants of virtual products cant be added before distinguishing features are selected, it should not be changed to additemSubmit() -->
            <a href="javascript:addItem()" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.OrderAddToCart}</span></a>&nbsp;
          </#if>
          <#if requestParameters.category_id??>
            <input type="hidden" name="category_id" value="${requestParameters.category_id}"/>
          </#if>
        </#if>
      </div>
      </form>
      </td></tr>
      <tr><td colspan="2" align="right">
    <div>
      <#if sessionAttributes.userLogin?has_content && sessionAttributes.userLogin.userLoginId != "anonymous">
        <hr />
        <form name="addToShoppingList" method="post" action="<@ofbizUrl>addItemToShoppingList<#if requestAttributes._CURRENT_VIEW_??>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>">
          <input type="hidden" name="productId" value="${product.productId}"/>
          <input type="hidden" name="product_id" value="${product.productId}"/>
          <input type="hidden" name="productStoreId" value="${productStoreId!}"/>
          <input type="hidden" name="reservStart" value= ""/>
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
          <#if "ASSET_USAGE" == product.productTypeId! || "ASSET_USAGE_OUT_IN" == product.productTypeId!>
              <table><tr><td>&nbsp;</td><td align="right">${uiLabelMap.CommonStartDate} (yyyy-mm-dd)</td><td><input type="text" size="10" name="reservStartStr" /></td><td>Number of&nbsp;days</td><td><input type="text" size="4" name="reservLength" /></td><td>&nbsp;</td><td align="right">Number of&nbsp;persons</td><td><input type="text" size="4" name="reservPersons" value="1" /></td><td align="right">Qty&nbsp;</td><td><input type="text" size="5" name="quantity" value="1" /></td></tr></table>
          <#else>
              <input type="text" size="5" name="quantity" value="1"/>
              <input type="hidden" name="reservStartStr" value= ""/>
          </#if>
          <a href="javascript:addShoplistSubmit();" class="buttontext">${uiLabelMap.OrderAddToShoppingList}</a>
        </form>
      <#else> <br />
        ${uiLabelMap.OrderYouMust} <a href="<@ofbizUrl>checkLogin/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBeLogged}</a>
        ${uiLabelMap.OrderToAddSelectedItemsToShoppingList}.&nbsp;
      </#if>
      </div>
      <#-- Prefill first select box (virtual products only) -->
      <#if variantTree?? && 0 < variantTree.size()>
        <script type="application/javascript">eval("list" + "${featureOrderFirst}" + "()");</script>
      </#if>

      <#-- Swatches (virtual products only) -->
      <#if variantSample?? && 0 < variantSample.size()>
        <#assign imageKeys = variantSample.keySet()>
        <#assign imageMap = variantSample>
        <p>&nbsp;</p>
        <table cellspacing="0" cellpadding="0">
          <tr>
            <#assign maxIndex = 7>
            <#assign indexer = 0>
            <#list imageKeys as key>
              <#assign swatchProduct = imageMap.get(key)>
              <#if swatchProduct?has_content && indexer < maxIndex>
                <#assign imageUrl = Static["org.apache.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(swatchProduct, "SMALL_IMAGE_URL", request, "url")!>
                <#if !imageUrl?string?has_content>
                  <#assign imageUrl = productContentWrapper.get("SMALL_IMAGE_URL", "url")!>
                </#if>
                <#if !imageUrl?string?has_content>
                  <#assign imageUrl = "/images/defaultImage.jpg">
                </#if>
                <td align="center" valign="bottom">
                  <a href="javascript:getList('FT${featureOrderFirst}','${indexer}',1);"><img src="<@ofbizContentUrl>${contentPathPrefix!}${imageUrl}</@ofbizContentUrl>" class='cssImgSmall' alt="" /></a>
                  <br />
                  <a href="javascript:getList('FT${featureOrderFirst}','${indexer}',1);" class="linktext">${key}</a>
                </td>
              </#if>
              <#assign indexer = indexer + 1>
            </#list>
            <#if (indexer > maxIndex)>
              <div><b>${uiLabelMap.ProductMoreOptions}</b></div>
            </#if>
          </tr>
        </table>
      </#if>
    </td>
  </tr>
</table>

  <#-- Digital Download Files Associated with this Product -->
  <#if downloadProductContentAndInfoList?has_content>
    <div id="download-files">
      <div>${uiLabelMap.OrderDownloadFilesTitle}:</div>
      <#list downloadProductContentAndInfoList as downloadProductContentAndInfo>
        <div>${downloadProductContentAndInfo.contentName}<#if downloadProductContentAndInfo.description?has_content> - ${downloadProductContentAndInfo.description}</#if></div>
      </#list>
    </div>
  </#if>

  <#-- Long description of product -->
  <div id="long-description">
      <div>${productContentWrapper.get("LONG_DESCRIPTION", "html")!}</div>
      <div>${productContentWrapper.get("WARNINGS", "html")!}</div>
  </div>

  <#-- Any attributes/etc may go here -->

  <#-- Upgrades/Up-Sell/Cross-Sell -->
  <#macro associated assocProducts beforeName showName afterName formNamePrefix targetRequestName>
  <#local targetRequest = "product">
  <#if targetRequestName?has_content>
    <#local targetRequest = targetRequestName>
  </#if>
  <#if assocProducts?has_content>
    <h2>${beforeName!}<#if "Y" == showName>${productContentWrapper.get("PRODUCT_NAME", "html")!}</#if>${afterName!}</h2>

    <div class="productsummary-container">
    <#list assocProducts as productAssoc>
        <div>
          <a href="<@ofbizUrl>${targetRequest}/<#if categoryId??>~category_id=${categoryId}/</#if>~product_id=${productAssoc.productIdTo!}</@ofbizUrl>" class="buttontext">
            ${productAssoc.productIdTo!}
          </a>
          - <b>${productAssoc.reason!}</b>
        </div>
      ${setRequestAttribute("optProductId", productAssoc.productIdTo)}
      ${setRequestAttribute("listIndex", listIndex)}
      ${setRequestAttribute("formNamePrefix", formNamePrefix)}
      <#if targetRequestName?has_content>
        ${setRequestAttribute("targetRequestName", targetRequestName)}
      </#if>
          ${screens.render(productsummaryScreen)}
      <#local listIndex = listIndex + 1>
    </#list>
    </div>

    ${setRequestAttribute("optProductId", "")}
    ${setRequestAttribute("formNamePrefix", "")}
    ${setRequestAttribute("targetRequestName", "")}
  </#if>
</#macro>

<#assign productValue = product>
<#assign listIndex = 1>
${setRequestAttribute("productValue", productValue)}
<div id="associated-products">
    <#-- obsolete -->
    <@associated assocProducts=obsoleteProducts beforeName="" showName="Y" afterName=" ${uiLabelMap.ProductObsolete}" formNamePrefix="obs" targetRequestName=""/>
    <#-- cross sell -->
    <@associated assocProducts=crossSellProducts beforeName="" showName="N" afterName="${uiLabelMap.ProductCrossSell}" formNamePrefix="cssl" targetRequestName="crosssell"/>
    <#-- up sell -->
    <@associated assocProducts=upSellProducts beforeName="${uiLabelMap.ProductUpSell} " showName="Y" afterName=":" formNamePrefix="upsl" targetRequestName="upsell"/>
    <#-- obsolescence -->
    <@associated assocProducts=obsolenscenseProducts beforeName="" showName="Y" afterName=" ${uiLabelMap.ProductObsolescense}" formNamePrefix="obce" targetRequestName=""/>
</div>

<#-- special cross/up-sell area using commonFeatureResultIds (from common feature product search) -->
<#if commonFeatureResultIds?has_content>
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
</div>
