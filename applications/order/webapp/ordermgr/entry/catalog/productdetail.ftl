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
<#assign price = priceMap?if_exists/>
<#-- end variable setup -->

<#-- virtual product javascript -->
${virtualJavaScript?if_exists}
<script language="JavaScript" type="text/javascript">
<!--
    var detailImageUrl = null;
    function setAddProductId(name) {
        document.addform.add_product_id.value = name;
        if (document.addform.quantity == null) return;
        if (name == '' || name == 'NULL' || isVirtual(name) == true) {
            document.addform.quantity.disabled = true;
            var elem = document.getElementById('product_id_display');
            var txt = document.createTextNode('');
            elem.replaceChild(txt, elem.firstChild);
        } else {
            document.addform.quantity.disabled = false;
            var elem = document.getElementById('product_id_display');
            var txt = document.createTextNode(name);
            elem.replaceChild(txt, elem.firstChild);
        }
    }
    function isVirtual(product) {
        var isVirtual = false;
        <#if virtualJavaScript?exists>
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
           alert("Please select all of the required options.");
           return;
       } else {
           if (isVirtual(document.addform.add_product_id.value)) {
               document.location = '<@ofbizUrl>product?category_id=${categoryId?if_exists}&product_id=</@ofbizUrl>' + document.addform.add_product_id.value;
               return;
           } else {
               document.addform.submit();
           }
       }
    }

    function popupDetail() {
        var defaultDetailImage = "${firstDetailImage?default(mainDetailImageUrl?default("_NONE_"))}";
        if (defaultDetailImage == null || defaultDetailImage == "null") {
            defaultDetailImage = "_NONE_";
        }

        if (detailImageUrl == null || detailImageUrl == "null") {
            detailImageUrl = defaultDetailImage;
        }

        if (detailImageUrl == "_NONE_") {
            alert("No detail image available to display.");
            return;
        }
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
            eval("list" + OPT[(currentFeatureIndex+1)] + selectedValue + "()");

            // set the product ID to NULL to trigger the alerts
            setAddProductId('NULL');
        } else {
            // this is the final selection -- locate the selected index of the last selection
            var indexSelected = document.forms["addform"].elements[name].selectedIndex;

            // using the selected index locate the sku
            var sku = document.forms["addform"].elements[name].options[indexSelected].value;

            // set the product ID
            setAddProductId(sku);

            // check for amount box
            toggleAmt(checkAmtReq(sku));
        }
    }

    function validate(x){
        var msg=new Array();
        msg[0]="Please use correct date format [yyyy-mm-dd]";
        
        var y=x.split("-");
        if(y.length!=3){ alert(msg[0]);return false; }
        if((y[2].length>2)||(parseInt(y[2])>31)) { alert(msg[0]); return false; }
        if(y[2].length==1){ y[2]="0"+y[2]; }
        if((y[1].length>2)||(parseInt(y[1])>12)){ alert(msg[0]); return false; }
        if(y[1].length==1){ y[1]="0"+y[1]; }            
        if(y[0].length>4){ alert(msg[0]); return false; }
        if(y[0].length<4) {
            if(y[0].length==2) {
                y[0]="20"+y[0];
            } else {
                alert(msg[0]);
                return false;
            }
        }
        return (y[0]+"-"+y[1]+"-"+y[2]);
    }    

    function additemSubmit(){
        <#if product.productTypeId?if_exists == "ASSET_USAGE">
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
        <#if product.productTypeId?if_exists == "ASSET_USAGE">
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
 //-->
 </script>

<div id="productdetail">

<table border="0" cellpadding="2" cellspacing="0">
  <#-- Category next/previous -->
  <#if category?exists>
    <tr>
      <td colspan="2" align="right">
        <#if previousProductId?exists>
          <a href="<@ofbizUrl>product/~category_id=${categoryId?if_exists}/~product_id=${previousProductId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>&nbsp;|&nbsp;
        </#if>
        <a href="<@ofbizUrl>category/~category_id=${categoryId?if_exists}</@ofbizUrl>" class="linktext">${(category.categoryName)?default(category.description)?if_exists}</a>
        <#if nextProductId?exists>
          &nbsp;|&nbsp;<a href="<@ofbizUrl>product/~category_id=${categoryId?if_exists}/~product_id=${nextProductId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
      </td>
    </tr>
  </#if>

  <tr><td colspan="2"><hr class="sepbar"/></td></tr>

  <#-- Product image/name/price -->
  <tr>
    <td align="left" valign="top" width="0">
      <#assign productLargeImageUrl = productContentWrapper.get("LARGE_IMAGE_URL")?if_exists>
      <#-- remove the next two lines to always display the virtual image first (virtual images must exist) -->
      <#if firstLargeImage?has_content>
        <#assign productLargeImageUrl = firstLargeImage>
      </#if>
      <#if productLargeImageUrl?has_content>
        <a href="javascript:popupDetail();"><img src="<@ofbizContentUrl>${contentPathPrefix?if_exists}${productLargeImageUrl?if_exists}</@ofbizContentUrl>" name="mainImage" vspace="5" hspace="5" border="0" width="200" align="left"></a>
      </#if>
    </td>
    <td align="right" valign="top">
      <div class="head2">${productContentWrapper.get("PRODUCT_NAME")?if_exists}</div>
      <div class="tabletext">${productContentWrapper.get("DESCRIPTION")?if_exists}</div>
      <div class="tabletext"><b>${product.productId?if_exists}</b></div>
      <#-- example of showing a certain type of feature with the product -->
      <#if sizeProductFeatureAndAppls?has_content>
        <div class="tabletext">
          <#if (sizeProductFeatureAndAppls?size == 1)>
            Size:
          <#else>
            Sizes Available:
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
      <#if price.competitivePrice?exists && price.price?exists && price.price?double < price.competitivePrice?double>
        <div class="tabletext">${uiLabelMap.ProductCompareAtPrice}: <span class="basePrice"><@ofbizCurrency amount=price.competitivePrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
        <div class="tabletext">${uiLabelMap.ProductListPrice}: <span class="basePrice"><@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <#if price.listPrice?exists && price.defaultPrice?exists && price.price?exists && price.price?double < price.defaultPrice?double && price.defaultPrice?double < price.listPrice?double>
        <div class="tabletext">${uiLabelMap.ProductRegularPrice}: <span class="basePrice"><@ofbizCurrency amount=price.defaultPrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <#if price.specialPromoPrice?exists>
        <div class="tabletext">${uiLabelMap.ProductSpecialPromoPrice}: <span class="basePrice"><@ofbizCurrency amount=price.specialPromoPrice isoCode=price.currencyUsed/></span></div>
      </#if>
      <div class="tabletext">
        <b>
          <#if price.isSale?exists && price.isSale>
            <span class="salePrice">${uiLabelMap.EcommerceOnSale}!</span>
            <#assign priceStyle = "salePrice">
          <#else>
            <#assign priceStyle = "regularPrice">
          </#if>
            ${uiLabelMap.EcommerceYourPrice}: <#if "Y" = product.isVirtual?if_exists> ${uiLabelMap.CommonFrom} </#if><span class="${priceStyle}"><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></span>
             <#if product.productTypeId?if_exists == "ASSET_USAGE">
            <#if product.reserv2ndPPPerc?exists && product.reserv2ndPPPerc != 0><br/><span class="${priceStyle}">${uiLabelMap.ProductReserv2ndPPPerc}<#if !product.reservNthPPPerc?exists || product.reservNthPPPerc == 0>${uiLabelMap.CommonUntil} ${product.reservMaxPersons}</#if> <@ofbizCurrency amount=product.reserv2ndPPPerc*price.price/100 isoCode=price.currencyUsed/></span></#if>
            <#if product.reservNthPPPerc?exists &&product.reservNthPPPerc != 0><br/><span class="${priceStyle}">${uiLabelMap.ProductReservNthPPPerc} <#if !product.reserv2ndPPPerc?exists || product.reserv2ndPPPerc == 0>${uiLabelMap.ProductReservSecond} <#else> ${uiLabelMap.ProductReservThird} </#if> ${uiLabelMap.CommonUntil} ${product.reservMaxPersons}, ${uiLabelMap.ProductEach}: <@ofbizCurrency amount=product.reservNthPPPerc*price.price/100 isoCode=price.currencyUsed/></span></#if>
            <#if (!product.reserv2ndPPPerc?exists || product.reserv2ndPPPerc == 0) && (!product.reservNthPPPerc?exists || product.reservNthPPPerc == 0)><br/>${uiLabelMap.ProductMaximum} ${product.reservMaxPersons} ${uiLabelMap.ProductPersons}.</#if>
             </#if>
         </b>
      </div>
      <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
        <#assign priceSaved = price.listPrice?double - price.price?double>
        <#assign percentSaved = (priceSaved?double / price.listPrice?double) * 100>
        <div class="tabletext">${uiLabelMap.EcommerceSave}: <span class="basePrice"><@ofbizCurrency amount=priceSaved isoCode=price.currencyUsed/> (${percentSaved?int}%)</span></div>
      </#if>
      <#-- show price details ("showPriceDetails" field can be set in the screen definition) -->
      <#if (showPriceDetails?exists && showPriceDetails?default("N") == "Y")>
          <#if price.orderItemPriceInfos?exists>
              <#list price.orderItemPriceInfos as orderItemPriceInfo>
                  <div class="tabletext">${orderItemPriceInfo.description?if_exists}</div>
              </#list>
          </#if>
      </#if>

      <#-- Included quantities/pieces -->
      <#if product.piecesIncluded?exists && product.piecesIncluded?long != 0>
        <div class="tabletext">
          ${uiLabelMap.EcommercePieces}: ${product.piecesIncluded}
        </div>
      </#if>
      <#if (product.quantityIncluded?exists && product.quantityIncluded?double != 0) || product.quantityUomId?has_content>
        <#assign quantityUom = product.getRelatedOneCache("QuantityUom")?if_exists/>
        <div class="tabletext">
          ${uiLabelMap.CommonQuantity}: ${product.quantityIncluded?if_exists} ${((quantityUom.abbreviation)?default(product.quantityUomId))?if_exists}
        </div>
      </#if>

      <#if (product.weight?exists && product.weight?double != 0) || product.weightUomId?has_content>
        <#assign weightUom = product.getRelatedOneCache("WeightUom")?if_exists/>
        <div class="tabletext">
          ${uiLabelMap.CommonWeight}: ${product.weight?if_exists} ${((weightUom.abbreviation)?default(product.weightUomId))?if_exists}
        </div>
      </#if>
      <#if (product.productHeight?exists && product.productHeight?double != 0) || product.heightUomId?has_content>
        <#assign heightUom = product.getRelatedOneCache("HeightUom")?if_exists/>
        <div class="tabletext">
          ${uiLabelMap.CommonHeight}: ${product.productHeight?if_exists} ${((heightUom.abbreviation)?default(product.heightUomId))?if_exists}
        </div>
      </#if>
      <#if (product.productWidth?exists && product.productWidth?double != 0) || product.widthUomId?has_content>
        <#assign widthUom = product.getRelatedOneCache("WidthUom")?if_exists/>
        <div class="tabletext">
          ${uiLabelMap.CommonWidth}: ${product.productWidth?if_exists} ${((widthUom.abbreviation)?default(product.widthUomId))?if_exists}
        </div>
      </#if>
      <#if (product.productDepth?exists && product.productDepth?double != 0) || product.depthUomId?has_content>
        <#assign depthUom = product.getRelatedOneCache("DepthUom")?if_exists/>
        <div class="tabletext">
          ${uiLabelMap.CommonDepth}: ${product.productDepth?if_exists} ${((depthUom.abbreviation)?default(product.depthUomId))?if_exists}
        </div>
      </#if>

      <#if daysToShip?exists>
        <div class="tabletext"><b>${uiLabelMap.ProductUsuallyShipsIn} <font color="red">${daysToShip}</font> ${uiLabelMap.CommonDays}!<b></div>
      </#if>

      <#-- tell a friend -->
      <div class="tabletext">&nbsp;</div>
      <div class="tabletext">
        <a href="javascript:popUpSmall('<@ofbizUrl>tellafriend?productId=${product.productId}</@ofbizUrl>','tellafriend');" class="buttontext">${uiLabelMap.CommonTellAFriend}</a>
      </div>
      <br/>

      <#if disFeatureList?exists && 0 < disFeatureList.size()>
      <p>&nbsp;</p>
        <#list disFeatureList as currentFeature>
            <#assign disFeatureType = currentFeature.getRelatedOneCache("ProductFeatureType")/>
            <div class="tabletext">
                <#if disFeatureType.description?exists>${disFeatureType.get("description", locale)}<#else>${currentFeature.productFeatureTypeId}</#if>:&nbsp;${currentFeature.description}
            </div>
        </#list>
            <div class="tabletext">&nbsp;</div>
      </#if>

      <form method="post" action="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="addform"  style="margin: 0;">
        <#assign inStock = true>
        <#-- Variant Selection -->
        <#if product.isVirtual?if_exists?upper_case == "Y">
          <#if variantTree?exists && (variantTree.size() > 0)>
            <#list featureSet as currentType>
              <div class="tabletext">
                <select name="FT${currentType}" class="selectBox" onchange="javascript:getList(this.name, (this.selectedIndex-1), 1);">
                  <option>${featureTypes.get(currentType)}</option>
                </select>
              </div>
            </#list>
            <input type="hidden" name="product_id" value="${product.productId}"/>
            <input type="hidden" name="add_product_id" value="NULL"/>
            <div class="tabletext">
                <b><span id="product_id_display"> </span></b>
            </div>
          <#else>
            <input type="hidden" name="product_id" value="${product.productId}"/>
            <input type="hidden" name="add_product_id" value="NULL"/>
            <div class="tabletext"><b>${uiLabelMap.ProductItemOutofStock}.</b></div>
            <#assign inStock = false>
          </#if>
        <#else>
          <input type="hidden" name="product_id" value="${product.productId}"/>
          <input type="hidden" name="add_product_id" value="${product.productId}"/>
          <#assign isStoreInventoryNotAvailable = !(Static["org.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryAvailable(request, product, 1.0?double))>
          <#assign isStoreInventoryRequired = Static["org.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryRequired(request, product)>
          <#if isStoreInventoryNotAvailable>
            <#if isStoreInventoryRequired>
              <div class="tabletext"><b>${uiLabelMap.ProductItemOutofStock}.</b></div>
              <#assign inStock = false>
            <#else>
              <div class="tabletext"><b>${product.inventoryMessage?if_exists}</b></div>
            </#if>
          </#if>
        </#if>
      </td></tr><tr><td colspan="2" align="right">
        <#-- check to see if introductionDate hasnt passed yet -->
        <#if product.introductionDate?exists && nowTimestamp.before(product.introductionDate)>
        <p>&nbsp;</p>
          <div class="tabletext" style="color: red;">${uiLabelMap.ProductProductNotYetMadeAvailable}.</div>
        <#-- check to see if salesDiscontinuationDate has passed -->
        <#elseif product.salesDiscontinuationDate?exists && nowTimestamp.after(product.salesDiscontinuationDate)>
          <div class="tabletext" style="color: red;">${uiLabelMap.ProductProductNoLongerAvailable}.</div>
        <#-- check to see if the product requires inventory check and has inventory -->
        <#else>
          <#if inStock>
            <#if product.requireAmount?default("N") == "Y">
              <#assign hiddenStyle = "tabletext">
            <#else>
              <#assign hiddenStyle = "tabletexthidden">
            </#if>           
            <div id="add_amount" class="${hiddenStyle}">
              <span style="white-space: nowrap;"><b>${uiLabelMap.CommonAmount}:</b></span>&nbsp;
              <input type="text" class="inputBox" size="5" name="add_amount" value=""/>
            </div>
            <#if product.productTypeId?if_exists == "ASSET_USAGE">
                <table width="100%"><tr>
                <td class="tabletext" nowrap align="right">Start Date<br/>(yyyy-mm-dd)</td><td><input type="text" class="inputBox" size="10" name="reservStart"/><a href="javascript:call_cal(document.addform.reservStart, '${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString().substring(0,10)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a></td>
                <td class="tabletext" nowrap align="right">End Date<br/>(yyyy-mm-dd)</td><td><input type="text" class="inputBox" size="10" name="reservEnd"/><a href="javascript:call_cal(document.addform.reservEnd, '${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString().substring(0,10)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a></td></tr>
                <tr>
                <#--td class="tabletext" nowrap align="right">Number<br/>of days</td><td><input type="textt" class="inputBox" size="4" name="reservLength"/></td></tr><tr><td>&nbsp;</td><td class="tabletext" align="right" nowrap>&nbsp;</td-->
                <td class="tabletext" nowrap align="right">Number of persons</td><td><input type="text" class="inputBox" size="4" name="reservPersons" value="2"/></td>
                <td class="tabletext" nowrap align="right">Number of rooms</td><td><input type="text" class="inputBox" size="5" name="quantity" value="1"/></td></tr></table>
            <#else/>
                <input type="text" class="inputBox" size="5" name="quantity" value="1"<#if product.isVirtual?if_exists?upper_case == "Y"> disabled="disabled"</#if>/>
            </#if>
            <#-- This calls addItem() so that variants of virtual products cant be added before distinguishing features are selected, it should not be changed to additemSubmit() -->
            <a href="javascript:addItem()" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.EcommerceAddtoCart}</span></a>&nbsp;
          </#if>
          <#if requestParameters.category_id?exists>
            <input type="hidden" name="category_id" value="${requestParameters.category_id}"/>
          </#if>
        </#if>
      </form>
    <div class="tabletext">
      <#if sessionAttributes.userLogin?has_content && sessionAttributes.userLogin.userLoginId != "anonymous">
        <hr class="sepbar"/>
        <form name="addToShoppingList" method="post" action="<@ofbizUrl>addItemToShoppingList<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>">
          <input type="hidden" name="productId" value="${requestParameters.product_id}"/>
          <input type="hidden" name="product_id" value="${requestParameters.product_id}"/>
          <input type="hidden" name="productStoreId" value="${productStoreId}"/>
          <input type="hidden" name="reservStart" value= ""/>
          <select name="shoppingListId" class="selectBox">
            <#if shoppingLists?has_content>
              <#list shoppingLists as shoppingList>
                <option value="${shoppingList.shoppingListId}">${shoppingList.listName}</option>
              </#list>
            </#if>
            <option value="">---</option>
            <option value="">${uiLabelMap.EcommerceNewShoppingList}</option>
          </select>
          &nbsp;&nbsp;
          <#if product.productTypeId?if_exists == "ASSET_USAGE">
              <table><tr><td>&nbsp;</td><td class="tabletext" align="right">${uiLabelMap.CommonStartDate} (yyyy-mm-dd)</td><td><input type="text" class="inputBox" size="10" name="reservStartStr" ></td><td class="tabletext">Number of&nbsp;days</td><td><input type="text" class="inputBox" size="4" name="reservLength"></td><td>&nbsp;</td><td class="tabletext" align="right">Number of&nbsp;persons</td><td><input type="text" class="inputBox" size="4" name="reservPersons" value="1"></td><td class="tabletext" align="right">Qty&nbsp;</td><td><input type="text" class="inputBox" size="5" name="quantity" value="1"></td></tr></table>
          <#else>
              <input type="text" class="inputBox" size="5" name="quantity" value="1"/>
              <input type="hidden" name="reservStartStr" value= ""/>
          </#if>
          <a href="javascript:addShoplistSubmit();" class="buttontext">${uiLabelMap.EcommerceAddtoShoppingList}</a>
        </form>
      <#else> <br/>
        ${uiLabelMap.EcommerceYouMust} <a href="<@ofbizUrl>checkLogin/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonLogin}</a>
        ${uiLabelMap.EcommerceToAddSelectedItemsToShoppingList}.&nbsp;
      </#if>
      </div>
      <#-- Prefill first select box (virtual products only) -->
      <#if variantTree?exists && 0 < variantTree.size()>
        <script language="JavaScript" type="text/javascript">eval("list" + "${featureOrderFirst}" + "()");</script>
      </#if>

      <#-- Swatches (virtual products only) -->
      <#if variantSample?exists && 0 < variantSample.size()>
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
                <#assign imageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(swatchProduct, "SMALL_IMAGE_URL", request)?if_exists>
                <#if !imageUrl?has_content>
                  <#assign imageUrl = productContentWrapper.get("SMALL_IMAGE_URL")?if_exists>
                </#if>
                <#if !imageUrl?has_content>
                  <#assign imageUrl = "/images/defaultImage.jpg">
                </#if>
                <td align="center" valign="bottom">
                  <a href="javascript:getList('FT${featureOrderFirst}','${indexer}',1);"><img src="<@ofbizContentUrl>${contentPathPrefix?if_exists}${imageUrl}</@ofbizContentUrl>" border="0" width="60" height="60"></a>
                  <br/>
                  <a href="javascript:getList('FT${featureOrderFirst}','${indexer}',1);" class="linktext">${key}</a>
                </td>
              </#if>
              <#assign indexer = indexer + 1>
            </#list>
            <#if (indexer > maxIndex)>
              <div class="tabletext"><b>${uiLabelMap.ProductMoreOptions}</b></div>
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
      <div class="tableheadtext">${uiLabelMap.EcommerceDownloadFilesTitle}:</div>
      <#list downloadProductContentAndInfoList as downloadProductContentAndInfo>
        <div class="tabletext">${downloadProductContentAndInfo.contentName}<#if downloadProductContentAndInfo.description?has_content> - ${downloadProductContentAndInfo.description}</#if></div>
      </#list>
    </div>
  </#if>

  <#-- Long description of product -->
  <div id="long-description">
      <div class="tabletext">${productContentWrapper.get("LONG_DESCRIPTION")?if_exists}</div>
      <div class="tabletext">${productContentWrapper.get("WARNINGS")?if_exists}</div>
  </div>

  <#-- Any attributes/etc may go here -->

  <#-- Product Reviews -->
  <div id="reviews">
      <div class="tableheadtext">${uiLabelMap.EcommerceCustomerReviews}:</div>
      <#if averageRating?exists && (averageRating?double > 0) && numRatings?exists && (numRatings?double > 1)>
          <div class="tabletext">${uiLabelMap.EcommerceAverageRating}: ${averageRating} <#if numRatings?exists>(${uiLabelMap.CommonFrom} ${numRatings} ${uiLabelMap.EcommerceRatings})</#if></div>
      </#if>
      <tr><td colspan="2"><hr class="sepbar"/></td></tr>
      <#if productReviews?has_content>
        <#list productReviews as productReview>
          <#assign postedUserLogin = productReview.getRelatedOne("UserLogin")>
          <#assign postedPerson = postedUserLogin.getRelatedOne("Person")?if_exists>
              <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                  <td>
                    <div class="tabletext"><b>${uiLabelMap.CommonBy}: </b><#if productReview.postedAnonymous?default("N") == "Y"> ${uiLabelMap.EcommerceAnonymous}<#else> ${postedPerson.firstName} ${postedPerson.lastName}&nbsp;</#if></div>
                  </td>
                  <td>
                    <div class="tabletext"><b>${uiLabelMap.CommonAt}: </b>${productReview.postedDateTime?if_exists}&nbsp;</div>
                  </td>
                  <td>
                    <div class="tabletext"><b>${uiLabelMap.EcommerceRanking}: </b>${productReview.productRating?if_exists?string}</div>
                  </td>
                </tr>
                <tr>
                  <td colspan="3">
                    <div class="tabletext">&nbsp;</div>
                  </td>
                </tr>
                <tr>
                  <td colspan="3">
                    <div class="tabletext">${productReview.productReview?if_exists}</div>
                  </td>
                </tr>
                <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              </table>
        </#list>
        <div>
            <a href="<@ofbizUrl>reviewProduct?category_id=${categoryId?if_exists}&product_id=${product.productId}</@ofbizUrl>" class="linktext">${uiLabelMap.ProductReviewThisProduct}!</a>
        </div>
      <#else>
        <div class="tabletext">${uiLabelMap.ProductProductNotReviewedYet}.</div>
        <div>
            <a href="<@ofbizUrl>reviewProduct?category_id=${categoryId?if_exists}&product_id=${product.productId}</@ofbizUrl>" class="linktext">${uiLabelMap.ProductBeTheFirstToReviewThisProduct}!</a>
        </div>
      </#if>
  </div>

<#-- Upgrades/Up-Sell/Cross-Sell -->
  <#macro associated assocProducts beforeName showName afterName formNamePrefix targetRequestName>
  <#assign targetRequest = "product">
  <#if targetRequestName?has_content>
    <#assign targetRequest = targetRequestName>
  </#if>
  <#if assocProducts?has_content>
    <div class="head2">${beforeName?if_exists}<#if showName == "Y">${productContentWrapper.get("PRODUCT_NAME")?if_exists}</#if>${afterName?if_exists}</div>

    <div class="productsummary-container">
    <#list assocProducts as productAssoc>
        <div class="tabletext">
          <a href="<@ofbizUrl>${targetRequest}/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${productAssoc.productIdTo?if_exists}</@ofbizUrl>" class="buttontext">
            ${productAssoc.productIdTo?if_exists}
          </a>
          - <b>${productAssoc.reason?if_exists}</b>
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
    <@associated assocProducts=crossSellProducts beforeName="" showName="N" afterName="${uiLabelMap.ProducrCrossSell}" formNamePrefix="cssl" targetRequestName="crosssell"/>
    <#-- up sell -->
    <@associated assocProducts=upSellProducts beforeName="${uiLabelMap.ProductUpSell} " showName="Y" afterName=":" formNamePrefix="upsl" targetRequestName="upsell"/>
    <#-- obsolescence -->
    <@associated assocProducts=obsolenscenseProducts beforeName="" showName="Y" afterName=" ${uiLabelMap.ProductObsolescense}" formNamePrefix="obce" targetRequestName=""/>
</div>

<#-- special cross/up-sell area using commonFeatureResultIds (from common feature product search) -->
<#if commonFeatureResultIds?has_content>
    <div class="head2">${uiLabelMap.ProductSimilarProducts}</div>

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
