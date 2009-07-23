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
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

<script language="JavaScript" type="text/javascript">
 <!--
     function changeCategory() {
         document.forms["keywordsearchform"].elements["SEARCH_CATEGORY_ID"].value=document.forms["advancedsearchform"].elements["DUMMYCAT"].value;
         document.forms["advancedsearchform"].elements["SEARCH_CATEGORY_ID"].value=document.forms["advancedsearchform"].elements["DUMMYCAT"].value;
     }
     function submitProductJump() {
         document.forms["productjumpform"].action=document.forms["productjumpform"].elements["DUMMYPAGE"].value;
         document.forms["productjumpform"].submit();
     }
 //-->
 </script>

<form name="keywordsearchform" method="post" action="<@ofbizUrl>keywordsearch?VIEW_SIZE=25&PAGING=Y</@ofbizUrl>" style="margin: 0;">
  <span class="label">${uiLabelMap.ProductKeywords}:</span><input type="text"name="SEARCH_STRING" size="20" maxlength="50" value="${requestParameters.SEARCH_STRING?if_exists}"/>
  <span class="label">
    ${uiLabelMap.ProductCategoryId}:
  </span>
  <input type="text" name="SEARCH_CATEGORY_ID" size="15" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/><a href="javascript:call_fieldlookup2(document.keywordsearchform.SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
  <span class="label">
    ${uiLabelMap.CommonNoContains}
  </span>
  <input type="checkbox" name="SEARCH_CONTAINS" value="N" <#if requestParameters.SEARCH_CONTAINS?if_exists == "N">checked="checked"</#if>/>
  <span class="label">
    ${uiLabelMap.CommonAny}
  </span>
  <input type="radio" name="SEARCH_OPERATOR" value="OR" <#if requestParameters.SEARCH_OPERATOR?if_exists != "AND">checked="checked"</#if>/>
  <span class="label">
    ${uiLabelMap.CommonAll}
  </span>
  <input type="radio" name="SEARCH_OPERATOR" value="AND" <#if requestParameters.SEARCH_OPERATOR?if_exists == "AND">checked="checked"</#if>/>
  &nbsp;
  <a href="javascript:document.keywordsearchform.submit()" class="buttontext">${uiLabelMap.CommonFind}</a>
</form>
<form name="advancedsearchform" method="post" action="<@ofbizUrl>advancedsearch</@ofbizUrl>" style="margin: 0;">
  <span class="label">
    ${uiLabelMap.ProductCategoryId}:
  </span>
  <input type="text" name="SEARCH_CATEGORY_ID" size="15" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/><a href="javascript:call_fieldlookup2(document.advancedsearchform.SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
  <div>
    <a href="javascript:document.advancedsearchform.submit()" class="buttontext">${uiLabelMap.ProductAdvancedSearch}</a>
  </div>
</form>
<form name="productjumpform" method="post" action="<@ofbizUrl>EditProduct</@ofbizUrl>" style="margin: 0;">
    <input type="text" name="productId" size="10" maxlength="20" value="${requestParameters.productId?if_exists}"/>
    <input type="hidden" name="viewSize" value="20"/>
    <input type="hidden" name="viewIndex" value="1"/>
    <a href="javascript:call_fieldlookup2(document.productjumpform.productId,'LookupProduct');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
    <select name="DUMMYPAGE" onchange="submitProductJump()">
        <option value="<@ofbizUrl>EditProduct</@ofbizUrl>">-${uiLabelMap.ProductProductJump}-</option>
        <option value="<@ofbizUrl>EditProductQuickAdmin</@ofbizUrl>">${uiLabelMap.ProductQuickAdmin}</option>
        <option value="<@ofbizUrl>EditProduct</@ofbizUrl>">${uiLabelMap.ProductProduct}</option>
        <option value="<@ofbizUrl>EditProductPrices</@ofbizUrl>">${uiLabelMap.ProductPrices}</option>
        <option value="<@ofbizUrl>EditProductContent</@ofbizUrl>">${uiLabelMap.ProductContent}</option>
        <option value="<@ofbizUrl>EditProductGoodIdentifications</@ofbizUrl>">${uiLabelMap.CommonIds}</option>
        <option value="<@ofbizUrl>EditProductCategories</@ofbizUrl>">${uiLabelMap.ProductCategories}</option>
        <option value="<@ofbizUrl>EditProductKeyword</@ofbizUrl>">${uiLabelMap.ProductKeywords}</option>
        <option value="<@ofbizUrl>EditProductAssoc</@ofbizUrl>">${uiLabelMap.ProductAssociations}</option>
        <option value="<@ofbizUrl>ViewProductManufacturing</@ofbizUrl>">${uiLabelMap.ProductManufacturing}</option>
        <option value="<@ofbizUrl>EditProductCosts</@ofbizUrl>">${uiLabelMap.ProductCosts}</option>
        <option value="<@ofbizUrl>EditProductAttributes</@ofbizUrl>">${uiLabelMap.ProductAttributes}</option>
        <option value="<@ofbizUrl>EditProductFeatures</@ofbizUrl>">${uiLabelMap.ProductFeatures}</option>
        <option value="<@ofbizUrl>EditProductFacilities</@ofbizUrl>">${uiLabelMap.ProductFacilities}</option>
        <option value="<@ofbizUrl>EditProductFacilityLocations</@ofbizUrl>">${uiLabelMap.ProductLocations}</option>
        <option value="<@ofbizUrl>EditProductInventoryItems</@ofbizUrl>">${uiLabelMap.ProductInventory}</option>
        <option value="<@ofbizUrl>EditProductSuppliers</@ofbizUrl>">${uiLabelMap.ProductSuppliers}</option>
        <option value="<@ofbizUrl>ViewProductAgreements</@ofbizUrl>">${uiLabelMap.ProductAgreements}</option>
        <option value="<@ofbizUrl>EditProductGlAccounts</@ofbizUrl>">${uiLabelMap.ProductAccounts}</option>
        <option value="<@ofbizUrl>EditProductPaymentMethodTypes</@ofbizUrl>">${uiLabelMap.ProductPaymentTypes}</option>
        <option value="<@ofbizUrl>EditProductMaints</@ofbizUrl>">${uiLabelMap.ProductMaintenance}</option>
        <option value="<@ofbizUrl>EditProductMeters</@ofbizUrl>">${uiLabelMap.ProductMeters}</option>
        <option value="<@ofbizUrl>EditProductSubscriptionResources</@ofbizUrl>">${uiLabelMap.ProductSubscriptionResources}</option>
        <option value="<@ofbizUrl>QuickAddVariants</@ofbizUrl>">${uiLabelMap.ProductVariants}</option>
        <option value="<@ofbizUrl>EditProductConfigs</@ofbizUrl>">${uiLabelMap.ProductConfigs}</option>
        <option value="<@ofbizUrl>viewProductOrder</@ofbizUrl>">${uiLabelMap.OrderOrders}</option>
        <option value="<@ofbizUrl>EditProductCommunicationEvents</@ofbizUrl>">${uiLabelMap.PartyCommunications}</option>
    </select>
</form>
