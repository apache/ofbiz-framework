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

<#assign maxToShow = 8/>
<#assign lastViewedCategories = sessionAttributes.lastViewedCategories?if_exists/>
<#if lastViewedCategories?has_content>
    <#if (lastViewedCategories?size > maxToShow)><#assign limit=maxToShow/><#else><#assign limit=(lastViewedCategories?size-1)/></#if>
    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxlink">
                <a href="<@ofbizUrl>clearLastViewed</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonClear}]</a>
            </div>
            <div class="boxhead">${uiLabelMap.EcommerceLastCategories}</div>
        </div>
        <div class="screenlet-body">
            <#list lastViewedCategories[0..limit] as categoryId>
                <#assign category = delegator.findByPrimaryKeyCache("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId", categoryId))?if_exists>
                <#if category?has_content>
                    <div>
                        <span class="browsecategorytext">-&nbsp;</span>                      
                        <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?exists>
                            <a href="<@ofbizUrl>category/~category_id=${categoryId}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
                        <#elseif catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("DESCRIPTION")?exists>
                            <a href="<@ofbizUrl>category/~category_id=${categoryId}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
                        <#else>          
                            <a href="<@ofbizUrl>category/~category_id=${categoryId}</@ofbizUrl>" class="browsecategorybutton">${category.description?if_exists}</a>
                        </#if>          
                    </div>
                </#if>
            </#list>
        </div>
    </div>                                    
</#if>
