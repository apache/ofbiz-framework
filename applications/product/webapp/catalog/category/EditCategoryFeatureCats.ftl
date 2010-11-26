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
<form method="post" action="<@ofbizUrl>attachProductFeaturesToCategory</@ofbizUrl>" name="attachProductFeaturesToCategory">
    <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}" />
</form>
<a href="javascript:document.attachProductFeaturesToCategory.submit()" class="buttontext">${uiLabelMap.ProductFeatureCategoryAttach}</a>

<#if productCategoryId?exists && productCategory?exists>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.PageTitleEditCategoryFeatureCategories}</h3>
    </div>
        <div class="screenlet-body">
            <#-- Feature Groups -->
            <table cellspacing="0" class="basic-table">
                <tr class="header-row">
                    <td><b>${uiLabelMap.ProductFeatureGroup}</b></td>
                    <td><b>${uiLabelMap.CommonFromDateTime}</b></td>
                    <td align="center"><b>${uiLabelMap.CommonThruDateTime}</b></td>
                    <td><b>&nbsp;</b></td>
                </tr>
                <#assign line = 0>
                <#assign rowClass = "2">
                <#list productFeatureCatGrpAppls as productFeatureCatGrpAppl>
                <#assign line = line + 1>
                <#assign productFeatureGroup = (productFeatureCatGrpAppl.getRelatedOne("ProductFeatureGroup"))?default(null)>
                <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                    <td><a href="<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}</@ofbizUrl>" class="buttontext"><#if productFeatureGroup?exists>${(productFeatureGroup.description)?if_exists}</#if> [${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}]</a></td>
                    <#assign hasntStarted = false>
                    <#if (productFeatureCatGrpAppl.getTimestamp("fromDate"))?exists && nowTimestamp.before(productFeatureCatGrpAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
                    <td><div<#if hasntStarted> style="color: red;</#if>>${(productFeatureCatGrpAppl.fromDate)?if_exists}</div></td>
                    <td align="center">
                        <form method="post" action="<@ofbizUrl>updateProductFeatureCatGrpAppl</@ofbizUrl>" name="lineFormGrp${line}">
                            <#assign hasExpired = false>
                            <#if (productFeatureCatGrpAppl.getTimestamp("thruDate"))?exists && nowTimestamp.after(productFeatureCatGrpAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                            <input type="hidden" name="productCategoryId" value="${(productFeatureCatGrpAppl.productCategoryId)?if_exists}" />
                            <input type="hidden" name="productFeatureGroupId" value="${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}" />
                            <input type="hidden" name="fromDate" value="${(productFeatureCatGrpAppl.fromDate)?if_exists}" />
                            <input type="text" size="25" name="thruDate" value="${(productFeatureCatGrpAppl.thruDate)?if_exists}" <#if hasExpired>style="color: red;"</#if> />
                            <a href="javascript:call_cal(document.lineFormGrp${line}.thruDate, '${(productFeatureCatGrpAppl.thruDate)?default(nowTimestamp?string)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar" /></a>
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;" />
                        </form>
                    </td>
                    <td align="center">
                        <a href="javascript:document.removeProductFeatureCatGrpApplForm_${productFeatureCatGrpAppl_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                        <form method="post" action="<@ofbizUrl>removeProductFeatureCatGrpAppl</@ofbizUrl>" name="removeProductFeatureCatGrpApplForm_${productFeatureCatGrpAppl_index}">
                            <input type="hidden" name="productFeatureGroupId" value="${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}" />
                            <input type="hidden" name="productCategoryId" value="${(productFeatureCatGrpAppl.productCategoryId)?if_exists}" />
                            <input type="hidden" name="fromDate" value="${(productFeatureCatGrpAppl.fromDate)?if_exists}" />
                        </form>
                    </td>
                </tr>
                <#-- toggle the row color -->
                <#if rowClass == "2">
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
                </#list>
            </table>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductApplyFeatureGroupFromCategory}</h3>
        </div>
        <div class="screenlet-body">
            <#if productFeatureGroups?has_content>
            <table cellspacing="0" class="basic-table">
                <tr><td>
                    <form method="post" action="<@ofbizUrl>createProductFeatureCatGrpAppl</@ofbizUrl>" style="margin: 0;" name="addNewGroupForm">
                    <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}" />
                    <select name="productFeatureGroupId">
                    <#list productFeatureGroups as productFeatureGroup>
                        <option value="${(productFeatureGroup.productFeatureGroupId)?if_exists}">${(productFeatureGroup.description)?if_exists} [${(productFeatureGroup.productFeatureGroupId)?if_exists}]</option>
                    </#list>
                    </select>
                    <input type="text" size="25" name="fromDate" />
                    <a href="javascript:call_cal(document.addNewGroupForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar" /></a>
                    <input type="submit" value="${uiLabelMap.CommonAdd}" />
                    </form>
                </td></tr>
            </table>
            <#else>
                &nbsp;
            </#if>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductApplyFeatureGroupFromCategory}</h3>
        </div>
        <div class="screenlet-body">
            <#-- Feature Categories -->
            <table cellspacing="0" class="basic-table">
                <tr class="header-row">
                    <td><b>${uiLabelMap.ProductFeatureCategory}</b></td>
                    <td><b>${uiLabelMap.CommonFromDateTime}</b></td>
                    <td align="center"><b>${uiLabelMap.CommonThruDateTime}</b></td>
                    <td><b>&nbsp;</b></td>
                </tr>
                <#assign line = 0>
                <#assign rowClass = "2">
                <#list productFeatureCategoryAppls as productFeatureCategoryAppl>
                <#assign line = line + 1>
                <#assign productFeatureCategory = (productFeatureCategoryAppl.getRelatedOne("ProductFeatureCategory"))?default(null)>
                <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                    <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}</@ofbizUrl>" class="buttontext"><#if productFeatureCategory?exists>${(productFeatureCategory.description)?if_exists}</#if> [${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}]</a></td>
                    <#assign hasntStarted = false>
                    <#if (productFeatureCategoryAppl.getTimestamp("fromDate"))?exists && nowTimestamp.before(productFeatureCategoryAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
                    <td <#if hasntStarted> style="color: red;"</#if>>${(productFeatureCategoryAppl.fromDate)?if_exists}</td>
                    <td align="center">
                        <form method="post" action="<@ofbizUrl>updateProductFeatureCategoryAppl</@ofbizUrl>" name="lineForm${line}">
                            <#assign hasExpired = false>
                            <#if (productFeatureCategoryAppl.getTimestamp("thruDate"))?exists && nowTimestamp.after(productFeatureCategoryAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                            <input type="hidden" name="productCategoryId" value="${(productFeatureCategoryAppl.productCategoryId)?if_exists}" />
                            <input type="hidden" name="productFeatureCategoryId" value="${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}" />
                            <input type="hidden" name="fromDate" value="${(productFeatureCategoryAppl.fromDate)?if_exists}" />
                            <input type="text" size="25" name="thruDate" value="${(productFeatureCategoryAppl.thruDate)?if_exists}" <#if hasExpired>style="color: red;"</#if> />
                            <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productFeatureCategoryAppl.thruDate)?default(nowTimestamp?string)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar" /></a>
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;" />
                        </form>
                    </td>
                    <td align="center">
                    <a href="javascript:document.removeProductFeatureCategoryApplForm_${productFeatureCategoryAppl_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                    <form method="post" action="<@ofbizUrl>removeProductFeatureCategoryAppl</@ofbizUrl>" name="removeProductFeatureCategoryApplForm_${productFeatureCategoryAppl_index}">
                        <input type="hidden" name="productFeatureCategoryId" value="${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}" />
                        <input type="hidden" name="productCategoryId" value="${(productFeatureCategoryAppl.productCategoryId)?if_exists}" />
                        <input type="hidden" name="fromDate" value="${(productFeatureCategoryAppl.fromDate)?if_exists}" />
                    </form>
                    </td>
                </tr>
                <#-- toggle the row color -->
                <#if rowClass == "2">
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
                </#list>
            </table>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductApplyFeatureGroupToCategory}</h3>
        </div>
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
                <tr><td>
                    <form method="post" action="<@ofbizUrl>createProductFeatureCategoryAppl</@ofbizUrl>" style="margin: 0;" name="addNewCategoryForm">
                        <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}" />
                        <select name="productFeatureCategoryId">
                        <#list productFeatureCategories as productFeatureCategory>
                            <option value="${(productFeatureCategory.productFeatureCategoryId)?if_exists}">${(productFeatureCategory.description)?if_exists} [${(productFeatureCategory.productFeatureCategoryId)?if_exists}]</option>
                        </#list>
                        </select>
                        <input type="text" size="25" name="fromDate" />
                        <a href="javascript:call_cal(document.addNewCategoryForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar" /></a>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" />
                    </form>
                </td></tr>
            </table>
        </div>
    </div>
</#if>