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
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td><b>${uiLabelMap.ProductParentCategoryId}</b></td>
                <td><b>${uiLabelMap.CommonFromDate}</b></td>
                <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                <td><b>&nbsp;</b></td>
            </tr>
            <#if currentProductCategoryRollups.size() != 0>
                <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryForm">
                    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
                    <#assign rowClass = "2">
                    <#list currentProductCategoryRollups as productCategoryRollup>
                    <#assign suffix = "_o_" + productCategoryRollup_index>
                    <#assign curCategory = productCategoryRollup.getRelatedOne("ParentProductCategory")>
                    <#assign hasntStarted = false>
                    <#if productCategoryRollup.fromDate?exists && nowTimestamp.before(productCategoryRollup.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
                    <#assign hasExpired = false>
                    <#if productCategoryRollup.thruDate?exists && nowTimestamp.after(productCategoryRollup.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
                    <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                        <td><#if curCategory?has_content><a href="<@ofbizUrl>EditCategory?productCategoryId=${curCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${curCategory.description?if_exists} [${curCategory.productCategoryId}]</a></#if></td>
                        <td <#if hasntStarted>style="color: red;"</#if>>${productCategoryRollup.fromDate}</td>
                        <td align="center">
                            <input type="hidden" name="showProductCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}">
                            <input type="hidden" name="productCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}">
                            <input type="hidden" name="parentProductCategoryId${suffix}" value="${productCategoryRollup.parentProductCategoryId}">
                            <input type="hidden" name="fromDate${suffix}" value="${productCategoryRollup.fromDate}">
                            <input type="text" size="25" name="thruDate${suffix}" value="${productCategoryRollup.thruDate?if_exists}" <#if hasExpired>style="color: red"</#if>>
                            <a href="javascript:call_cal(document.updateProductCategoryForm.thruDate${suffix}, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                            <input type="text" size="5" name="sequenceNum${suffix}" value="${productCategoryRollup.sequenceNum?if_exists}">
                        </td>
                        <td>
                            <a href="javascript:document.removeProductCategoryFromCategory_${productCategoryRollup_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                        </td>
                    </tr>
                    <#-- toggle the row color -->
                    <#if rowClass == "2">
                        <#assign rowClass = "1">
                    <#else>
                        <#assign rowClass = "2">
                    </#if>
                    </#list>
                    <tr valign="middle">
                        <td colspan="4" align="center">
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                            <input type="hidden" value="${currentProductCategoryRollups.size()}" name="_rowCount">
                        </td>
                    </tr>
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
            <#if currentProductCategoryRollups.size() == 0>
                <tr valign="middle">
                    <td colspan="4">${uiLabelMap.ProductNoParentCategoriesFound}.</td>
                </tr>
            </#if>
        </table>
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
                    <input type="hidden" name="productCategoryId" value="${productCategoryId}">
                    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
                    <input type="text" name="parentProductCategoryId" size="20" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/>
                    <a href="javascript:call_fieldlookup2(document.addParentForm.parentProductCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                    <input type="text" size="25" name="fromDate">
                    <a href="javascript:call_cal(document.addParentForm.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                    <input type="submit" value="${uiLabelMap.CommonAdd}">
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
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td><b>${uiLabelMap.ProductChildCategoryId}</b></td>
                <td><b>${uiLabelMap.CommonFromDate}</b></td>
                <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                <td><b>&nbsp;</b></td>
            </tr>
            <#if parentProductCategoryRollups.size() != 0>
                <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryToCategoryChild">
                    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
                    <#assign lineChild = 0>
                    <#assign rowClass = "2">
                    <#list parentProductCategoryRollups as productCategoryRollup>
                    <#assign suffix = "_o_" + lineChild>
                    <#assign lineChild = lineChild + 1>
                    <#assign curCategory = productCategoryRollup.getRelatedOne("CurrentProductCategory")>
                    <#assign hasntStarted = false>
                    <#if productCategoryRollup.fromDate?exists && nowTimestamp.before(productCategoryRollup.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
                    <#assign hasExpired = false>
                    <#if productCategoryRollup.thruDate?exists && nowTimestamp.after(productCategoryRollup.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
                        <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                            <td><#if curCategory?has_content><a href="<@ofbizUrl>EditCategory?productCategoryId=${curCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${curCategory.description?if_exists} [${curCategory.productCategoryId}]</a></#if>
                            <td <#if hasntStarted>style="color: red"</#if>>${productCategoryRollup.fromDate}</td>
                            <td align="center">
                                <input type="hidden" name="productCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}">
                                <input type="hidden" name="parentProductCategoryId${suffix}" value="${productCategoryRollup.parentProductCategoryId}">
                                <input type="hidden" name="fromDate${suffix}" value="${productCategoryRollup.fromDate}">
                                <input type="text" size="25" name="thruDate${suffix}" value="${productCategoryRollup.thruDate?if_exists}" <#if hasExpired>style="color: red;"</#if>>
                                <a href="javascript:call_cal(document.updateProductCategoryToCategoryChild.thruDate${suffix}, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                                <input type="text" size="5" name="sequenceNum${suffix}" value="${productCategoryRollup.sequenceNum?if_exists}">
                            </td>
                            <td>
                                <a href="javascript:document.removeProductCategoryFromCategory_1_${productCategoryRollup_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                            </td>
                        </tr>
                        <#-- toggle the row color -->
                        <#if rowClass == "2">
                            <#assign rowClass = "1">
                        <#else>
                            <#assign rowClass = "2">
                        </#if>
                    </#list>
                    <tr valign="middle">
                        <td colspan="4" align="center">
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                            <input type="hidden" value="${lineChild}" name="_rowCount">
                        </td>
                    </tr>
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
            <#if parentProductCategoryRollups.size() == 0>
                <tr valign="middle">
                    <td colspan="4">${uiLabelMap.ProductNoChildCategoriesFound}.</td>
                </tr>
            </#if>
        </table>
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
                    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
                    <input type="hidden" name="parentProductCategoryId" value="${productCategoryId}">
                    <input type="text" name="productCategoryId" size="20" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/>
                    <a href="javascript:call_fieldlookup2(document.addChildForm.productCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                    <input type="text" size="25" name="fromDate">
                    <a href="javascript:call_cal(document.addChildForm.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                    <input type="submit" value="${uiLabelMap.CommonAdd}">
                </form>
            </td></tr>
        </table>
    </div>
</div>
</#if>