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

<#if productCategoryId?? && productCategory??>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.PageTitleEditCategoryProductCatalogs}</h3>
    </div>
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td><b>${uiLabelMap.ProductCatalogNameId}</b></td>
                <td><b>${uiLabelMap.CommonType}</b></td>
                <td><b>${uiLabelMap.CommonFromDateTime}</b></td>
                <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                <td><b>&nbsp;</b></td>
            </tr>
            <#assign line = 0>
            <#assign rowClass = "2">
            <#list prodCatalogCategories as prodCatalogCategory>
            <#assign line = line + 1>
            <#assign prodCatalog = prodCatalogCategory.getRelatedOne("ProdCatalog", false)>
            <#assign curProdCatalogCategoryType = prodCatalogCategory.getRelatedOne("ProdCatalogCategoryType", true)>
            <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
                <td><a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${(prodCatalogCategory.prodCatalogId)!}</@ofbizUrl>" class="buttontext"><#if prodCatalog??>${(prodCatalog.catalogName)!}</#if> [${(prodCatalogCategory.prodCatalogId)!}]</a></td>
                <td>
                    ${(curProdCatalogCategoryType.get("description",locale))?default(prodCatalogCategory.prodCatalogCategoryTypeId)}
                </td>
                <#assign hasntStarted = false>
                <#if (prodCatalogCategory.getTimestamp("fromDate"))?? && nowTimestamp.before(prodCatalogCategory.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
                <td <#if hasntStarted> style="color: red;"</#if>>${(prodCatalogCategory.fromDate)!}</td>
                <td align="center">
                    <form method="post" action="<@ofbizUrl>category_updateProductCategoryToProdCatalog</@ofbizUrl>" name="lineForm_update${line}">
                        <#assign hasExpired = false>
                        <#if (prodCatalogCategory.getTimestamp("thruDate"))?? && nowTimestamp.after(prodCatalogCategory.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                        <input type="hidden" name="prodCatalogId" value="${(prodCatalogCategory.prodCatalogId)!}"/>
                        <input type="hidden" name="productCategoryId" value="${(prodCatalogCategory.productCategoryId)!}"/>
                        <input type="hidden" name="prodCatalogCategoryTypeId" value="${prodCatalogCategory.prodCatalogCategoryTypeId}"/>
                        <input type="hidden" name="fromDate" value="${(prodCatalogCategory.fromDate)!}"/>
                        <#if hasExpired><#assign class="alert"><#else><#assign class=""></#if>
                        <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="${class!}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(prodCatalogCategory.thruDate)!}" size="25" maxlength="30" id="thruDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        <input type="text" size="5" name="sequenceNum" value="${(prodCatalogCategory.sequenceNum)!}"/>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
                    </form>
                </td>
                <td align="center">
                  <form method="post" action="<@ofbizUrl>category_removeProductCategoryFromProdCatalog</@ofbizUrl>" name="lineForm_delete${line}">
                    <input type="hidden" name="prodCatalogId" value="${(prodCatalogCategory.prodCatalogId)!}"/>
                    <input type="hidden" name="productCategoryId" value="${(prodCatalogCategory.productCategoryId)!}"/>
                    <input type="hidden" name="prodCatalogCategoryTypeId" value="${prodCatalogCategory.prodCatalogCategoryTypeId}"/>
                    <input type="hidden" name="fromDate" value="${(prodCatalogCategory.fromDate)!}"/>
                    <a href="javascript:document.lineForm_delete${line}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </td>
            </tr>
            <#-- toggle the row color -->
            <#if "2" == rowClass>
                <#assign rowClass = "1">
            <#else>
                <#assign rowClass = "2">
            </#if>
            </#list>
            </table>
            <br />
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductAddCatalogProductCategory}</h3>
        </div>
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
                <tr><td>
                    <form method="post" action="<@ofbizUrl>category_addProductCategoryToProdCatalog</@ofbizUrl>" style="margin: 0;" name="addNewForm">
                        <input type="hidden" name="productCategoryId" value="${productCategoryId!}"/>
                        <select name="prodCatalogId">
                        <#list prodCatalogs as prodCatalog>
                            <option value="${(prodCatalog.prodCatalogId)!}">${(prodCatalog.catalogName)!} [${(prodCatalog.prodCatalogId)!}]</option>
                        </#list>
                        </select>
                        <select name="prodCatalogCategoryTypeId" size="1">
                        <#list prodCatalogCategoryTypes as prodCatalogCategoryType>
                            <option value="${(prodCatalogCategoryType.prodCatalogCategoryTypeId)!}">${(prodCatalogCategoryType.get("description",locale))!}</option>
                        </#list>
                        </select>
                        <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        <input type="submit" value="${uiLabelMap.CommonAdd}"/>
                    </form>
                </td></tr>
            </table>
        </div>
    </div>
</#if>
