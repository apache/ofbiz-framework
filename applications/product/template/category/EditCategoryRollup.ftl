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

<#if productCategoryId?has_content>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductCategoryRollupParentCategories}</h3>
    </div>
    <div class="screenlet-body">
        <#if currentProductCategoryRollups.size() == 0>
            <table cellspacing="0" class="basic-table">
               <tr class="header-row">
                  <td><b>${uiLabelMap.ProductParentCategoryId}</b></td>
                  <td><b>${uiLabelMap.CommonFromDate}</b></td>
                  <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                  <td><b>&nbsp;</b></td>
              </tr>
              <tr valign="middle">
                  <td colspan="4">${uiLabelMap.ProductNoParentCategoriesFound}.</td>
              </tr>
           </table>
        <#else>        
           <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryForm">
           <input type="hidden" name="showProductCategoryId" value="${productCategoryId}" />
            <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td><b>${uiLabelMap.ProductParentCategoryId}</b></td>
                <td><b>${uiLabelMap.CommonFromDate}</b></td>
                <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                <td><b>&nbsp;</b></td>
            </tr>
                    <#assign rowClass = "2">
                    <#list currentProductCategoryRollups as productCategoryRollup>
                    <#assign suffix = "_o_" + productCategoryRollup_index>
                    <#assign curCategory = productCategoryRollup.getRelatedOne("ParentProductCategory", false)>
                    <#assign hasntStarted = false>
                    <#if productCategoryRollup.fromDate?? && nowTimestamp.before(productCategoryRollup.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
                    <#assign hasExpired = false>
                    <#if productCategoryRollup.thruDate?? && nowTimestamp.after(productCategoryRollup.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
                    <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
                        <td><#if curCategory?has_content>
                                <a href="<@ofbizUrl>EditCategory?productCategoryId=${curCategory.productCategoryId}</@ofbizUrl>" >
                                    <#assign catContentWrapper = Static["org.apache.ofbiz.product.category.CategoryContentWrapper"].makeCategoryContentWrapper(curCategory, request)!>
                                    <#if catContentWrapper?has_content>
                                        ${catContentWrapper.get("CATEGORY_NAME", "html")!catContentWrapper.get("DESCRIPTION", "html")!curCategory.categoryName!curCategory.description!}
                                    <#else>
                                        ${curCategory.categoryName!curCategory.description!}
                                    </#if>
                                    [${curCategory.productCategoryId}]
                                </a>
                            </#if>
                        </td>
                        <td <#if hasntStarted>style="color: red;"</#if>>${productCategoryRollup.fromDate}</td>
                        <td align="center">
                            <input type="hidden" name="showProductCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}" />
                            <input type="hidden" name="productCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}" />
                            <input type="hidden" name="parentProductCategoryId${suffix}" value="${productCategoryRollup.parentProductCategoryId}" />
                            <input type="hidden" name="fromDate${suffix}" value="${productCategoryRollup.fromDate}" />
                            <@htmlTemplate.renderDateTimeField name="thruDate${suffix}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${productCategoryRollup.thruDate!''}" size="25" maxlength="30" id="thruDate${suffix}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                            <input type="text" size="5" name="sequenceNum${suffix}" value="${productCategoryRollup.sequenceNum!}" />
                        </td>
                        <td>
                            <a href="javascript:document.removeProductCategoryFromCategory_${productCategoryRollup_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                        </td>
                    </tr>
                    <#-- toggle the row color -->
                    <#if "2" == rowClass>
                        <#assign rowClass = "1">
                    <#else>
                        <#assign rowClass = "2">
                    </#if>
                    </#list>
                    <tr valign="middle">
                        <td colspan="4" align="center">
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;" />
                            <input type="hidden" value="${currentProductCategoryRollups.size()}" name="_rowCount" />
                        </td>
                    </tr>
        </table>
     </form>
                <#list currentProductCategoryRollups as productCategoryRollup>
                    <form name="removeProductCategoryFromCategory_${productCategoryRollup_index}" method="post" action="<@ofbizUrl>removeProductCategoryFromCategory</@ofbizUrl>">
                        <input type="hidden" name="showProductCategoryId" value="${productCategoryId}"/>
                        <input type="hidden" name="productCategoryId" value="${productCategoryRollup.productCategoryId}"/>
                        <input type="hidden" name="parentProductCategoryId" value="${productCategoryRollup.parentProductCategoryId}"/>
                        <input type="hidden" name="fromDate" value="${productCategoryRollup.fromDate}"/>
                    </form>
                </#list>
    </#if>      
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductAddCategoryParent} ${uiLabelMap.ProductCategorySelectCategoryAndEnterFromDate}:</h3>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr><td>
                <form method="post" action="<@ofbizUrl>addProductCategoryToCategory</@ofbizUrl>" style="margin: 0;" name="addParentForm">
                    <input type="hidden" name="productCategoryId" value="${productCategoryId}" />
                    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}" />
                    <@htmlTemplate.lookupField value="${requestParameters.SEARCH_CATEGORY_ID!}" formName="addParentForm" name="parentProductCategoryId" id="parentProductCategoryId" fieldFormName="LookupProductCategory"/>
                    <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>

                    <input type="submit" value="${uiLabelMap.CommonAdd}" />
                </form>
            </td></tr>
        </table>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductCategoryRollupChildCategories}</h3>
    </div>
    <div class="screenlet-body">
        <#if parentProductCategoryRollups.size() == 0>
            <table cellspacing="0" class="basic-table">
                <tr class="header-row">
                    <td><b>${uiLabelMap.ProductChildCategoryId}</b></td>
                    <td><b>${uiLabelMap.CommonFromDate}</b></td>
                    <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                    <td><b>&nbsp;</b></td>
                </tr>
                <tr valign="middle">
                    <td colspan="4">${uiLabelMap.ProductNoChildCategoriesFound}.</td>
                </tr>
            </table>
        <#else>
            <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryToCategoryChild">
            <input type="hidden" name="showProductCategoryId" value="${productCategoryId}" />
            <table cellspacing="0" class="basic-table">
                <tr class="header-row">
                    <td><b>${uiLabelMap.ProductChildCategoryId}</b></td>
                    <td><b>${uiLabelMap.CommonFromDate}</b></td>
                    <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                    <td><b>&nbsp;</b></td>
                </tr>           
                    <#assign lineChild = 0>
                    <#assign rowClass = "2">
                    <#list parentProductCategoryRollups as productCategoryRollup>
                    <#assign suffix = "_o_" + lineChild>
                    <#assign lineChild = lineChild + 1>
                    <#assign curCategory = productCategoryRollup.getRelatedOne("CurrentProductCategory", false)>
                    <#assign hasntStarted = false>
                    <#if productCategoryRollup.fromDate?? && nowTimestamp.before(productCategoryRollup.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
                    <#assign hasExpired = false>
                    <#if productCategoryRollup.thruDate?? && nowTimestamp.after(productCategoryRollup.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
                        <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
                            <td><#if curCategory?has_content>
                                    <a href="<@ofbizUrl>EditCategory?productCategoryId=${curCategory.productCategoryId}</@ofbizUrl>" >
                                        <#assign catContentWrapper = Static["org.apache.ofbiz.product.category.CategoryContentWrapper"].makeCategoryContentWrapper(curCategory, request)!>
                                        <#if catContentWrapper?has_content>
                                            ${catContentWrapper.get("CATEGORY_NAME", "html")!catContentWrapper.get("DESCRIPTION", "html")!curCategory.categoryName!curCategory.description!}
                                        <#else>
                                            ${curCategory.categoryName!curCategory.description!}
                                        </#if>
                                        [${curCategory.productCategoryId}]
                                    </a>
                                </#if>
                            </td>
                            <td <#if hasntStarted>style="color: red"</#if>>${productCategoryRollup.fromDate}</td>
                            <td align="center">
                                <input type="hidden" name="productCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}" />
                                <input type="hidden" name="parentProductCategoryId${suffix}" value="${productCategoryRollup.parentProductCategoryId}" />
                                <input type="hidden" name="fromDate${suffix}" value="${productCategoryRollup.fromDate}" />
                                <@htmlTemplate.renderDateTimeField name="thruDate${suffix}" event="" action="" className="" alert="" title="Thru Date" value="${productCategoryRollup.thruDate!''}" size="25" maxlength="50" id="thruDatefromDate${suffix}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>

                                <input type="text" size="5" name="sequenceNum${suffix}" value="${productCategoryRollup.sequenceNum!}" />
                            </td>
                            <td>
                                <a href="javascript:document.removeProductCategoryFromCategory_1_${productCategoryRollup_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                            </td>
                        </tr>
                        <#-- toggle the row color -->
                        <#if "2" == rowClass>
                            <#assign rowClass = "1">
                        <#else>
                            <#assign rowClass = "2">
                        </#if>
                    </#list>
                    <tr valign="middle">
                        <td colspan="4" align="center">
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;" />
                            <input type="hidden" value="${lineChild}" name="_rowCount" />
                        </td>
                    </tr>
            </table>
            </form>
            <#list parentProductCategoryRollups as productCategoryRollup>
               <form name="removeProductCategoryFromCategory_1_${productCategoryRollup_index}" method="post" action="<@ofbizUrl>removeProductCategoryFromCategory</@ofbizUrl>">
                   <input type="hidden" name="showProductCategoryId" value="${productCategoryId}"/>
                   <input type="hidden" name="productCategoryId" value="${productCategoryRollup.productCategoryId}"/>
                   <input type="hidden" name="parentProductCategoryId" value="${productCategoryRollup.parentProductCategoryId}"/>
                   <input type="hidden" name="fromDate" value="${productCategoryRollup.fromDate}"/>
               </form>
            </#list>
        </#if>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductAddCategoryChild} ${uiLabelMap.ProductCategorySelectCategoryAndEnterFromDate}:</h3>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr><td>
                <form method="post" action="<@ofbizUrl>addProductCategoryToCategory</@ofbizUrl>" style="margin: 0;" name="addChildForm">
                    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}" />
                    <input type="hidden" name="parentProductCategoryId" value="${productCategoryId}" />
                    <@htmlTemplate.lookupField value="${requestParameters.SEARCH_CATEGORY_ID!}"  formName="addChildForm" name="productCategoryId" id="productCategoryId" fieldFormName="LookupProductCategory"/>
                    <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate_2" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <input type="submit" value="${uiLabelMap.CommonAdd}" />
                </form>
            </td></tr>
        </table>
    </div>
</div>
</#if>
