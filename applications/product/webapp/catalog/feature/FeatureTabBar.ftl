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

<#assign unselectedClassName = "">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "selected"}>
<br/>
<div class="button-bar tab-bar">
    <ul>
        <li><a href="<@ofbizUrl>EditFeatureTypes</@ofbizUrl>" class="${selectedClassMap.FeatureType?default(unselectedClassName)}">${uiLabelMap.ProductFeatureType}</a></li>
        <li><a href="<@ofbizUrl>EditFeatureCategories</@ofbizUrl>" class="${selectedClassMap.FeatureCategory?default(unselectedClassName)}">${uiLabelMap.ProductFeatureCategory}</a></li>
        <li><a href="<@ofbizUrl>EditFeatureGroups</@ofbizUrl>" class="${selectedClassMap.FeatureGroup?default(unselectedClassName)}">${uiLabelMap.ProductFeatureGroup}</a></li>
        <li><a href="<@ofbizUrl>EditFeatureInterActions</@ofbizUrl>" class="${selectedClassMap.FeatureInterAction?default(unselectedClassName)}">${uiLabelMap.ProductFeatureInteraction}</a></li>
    </ul>
</div>
<br/>