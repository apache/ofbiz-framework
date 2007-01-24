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

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if productPromoId?has_content>
    <div class='tabContainer'>
        <a href="<@ofbizUrl>EditProductPromo?productPromoId=${productPromoId}</@ofbizUrl>" class="${selectedClassMap.EditProductPromo?default(unselectedClassName)}">${uiLabelMap.ProductPromotion}</a>
        <a href="<@ofbizUrl>EditProductPromoRules?productPromoId=${productPromoId}</@ofbizUrl>" class="${selectedClassMap.EditProductPromoRules?default(unselectedClassName)}">${uiLabelMap.ProductRules}</a>
        <a href="<@ofbizUrl>EditProductPromoStores?productPromoId=${productPromoId}</@ofbizUrl>" class="${selectedClassMap.EditProductPromoStores?default(unselectedClassName)}">${uiLabelMap.ProductStores}</a>
        <a href="<@ofbizUrl>FindProductPromoCode?productPromoId=${productPromoId}</@ofbizUrl>" class="${selectedClassMap.FindProductPromoCode?default(unselectedClassName)}">${uiLabelMap.ProductPromotionCode}</a>
    </div>
</#if>
