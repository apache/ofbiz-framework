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

<#if configItemId?has_content>
    <br/>
    <div class="button-bar tab-bar">
        <ul>
            <li><a href="<@ofbizUrl>EditProductConfigItem?configItemId=${configItemId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigItem?default(unselectedClassName)}">${uiLabelMap.ProductConfigItem}</a></li>
            <li><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${configItemId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigOptions?default(unselectedClassName)}">${uiLabelMap.ProductConfigOptions}</a></li>
            <li><a href="<@ofbizUrl>EditProductConfigItemContent?configItemId=${configItemId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigItemContent?default(unselectedClassName)}">${uiLabelMap.ProductContent}</a></li>
        </ul>
        <br/>
    </div>
</#if>
