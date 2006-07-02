<#--
$Id: $

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

    <div class='tabContainer'>
        <a href="<@ofbizUrl>EditFixedAsset?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.EditFixedAsset?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAsset}</a>
<!--   <a href="<@ofbizUrl>ListFixedAssetRollUp?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.ListFixedAssetRollUp?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetRollUp}</a>
        <a href="<@ofbizUrl>ListFixedAssetParties?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.ListFixedAssetParties?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetParties}</a> -->
        <a href="<@ofbizUrl>ListFixedAssetProducts?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.ListFixedAssetProducts?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetProducts}</a>
        <a href="<@ofbizUrl>ListFixedAssetCalendar?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.ListFixedAssetCalendar?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetCalendar}</a>
        <a href="<@ofbizUrl>ListFixedAssetStdCosts?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.ListFixedAssetStdCosts?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetStdCosts}</a>
        <a href="<@ofbizUrl>EditFixedAssetIdents?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.EditFixedAssetIdents?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetIdents}</a>        
        <a href="<@ofbizUrl>EditFixedAssetRegistrations?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.EditFixedAssetRegistrations?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetRegistrations}</a>        
        <a href="<@ofbizUrl>ListFixedAssetMaints?fixedAssetId=${fixedAsset.fixedAssetId}</@ofbizUrl>" class="${selectedClassMap.ListFixedAssetMaints?default(unselectedClassName)}">${uiLabelMap.AccountingFixedAssetMaints}</a>        
    </div>
