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
<#if productId?exists>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.PageTitleEditProductFeatures}</h3>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>UpdateFeatureToProductApplication</@ofbizUrl>" name="selectAllForm">
      <input type="hidden" name="_useRowSubmit" value="Y"/>
      <input type="hidden" name="_checkGlobalScope" value="Y"/>
      <input type="hidden" name="productId" value="${productId}"/>
      <table cellspacing="0" class="basic-table">
        <tr class="header-row">
          <td><b>${uiLabelMap.CommonId}</b></td>
          <td><b>${uiLabelMap.CommonDescription}</b></td>
          <td><b>${uiLabelMap.ProductUomId}</b></td>
          <td><b>${uiLabelMap.ProductType}</b></td>
          <td><b>${uiLabelMap.ProductCategory}</b></td>
          <td><b>${uiLabelMap.CommonFromDate}</b></td>
          <td><b>${uiLabelMap.ProductThruDateAmountSequenceApplicationType}</b></td>
          <td><b>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');highlightAllRows(this, 'productFeatureId_tableRow_', 'selectAllForm');"/></b></td>
        </tr>
  <#assign rowClass = "2">
  <#list productFeatureAndAppls as productFeatureAndAppl>
    <#if productFeatureAndAppl.uomId?exists>
        <#assign curProductFeatureUom = delegator.findOne("Uom",{"uomId",productFeatureAndAppl.uomId}, true)>
    </#if>
    <#assign curProductFeatureType = productFeatureAndAppl.getRelatedOneCache("ProductFeatureType")>
    <#assign curProductFeatureApplType = productFeatureAndAppl.getRelatedOneCache("ProductFeatureApplType")>
    <#assign curProductFeatureCategory = (productFeatureAndAppl.getRelatedOneCache("ProductFeatureCategory")?if_exists)>
        <tr id="productFeatureId_tableRow_${productFeatureAndAppl_index}" valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
          <td>
          <input type="hidden" name="productId_o_${productFeatureAndAppl_index}" value="${(productFeatureAndAppl.productId)?if_exists}" />
          <input type="hidden" name="productFeatureId_o_${productFeatureAndAppl_index}" value="${(productFeatureAndAppl.productFeatureId)?if_exists}" />
          <input type="hidden" name="fromDate_o_${productFeatureAndAppl_index}" value="${(productFeatureAndAppl.fromDate)?if_exists}" />
          <a href="<@ofbizUrl>EditFeature?productFeatureId=${(productFeatureAndAppl.productFeatureId)?if_exists}</@ofbizUrl>" class="buttontext">
              ${(productFeatureAndAppl.productFeatureId)?if_exists}</a></td>
          <td>${(productFeatureAndAppl.get("description",locale))?if_exists}</td>
          <td><#if productFeatureAndAppl.uomId?exists>${curProductFeatureUom.abbreviation!}</#if></td>
          <td>${(curProductFeatureType.get("description",locale))?default((productFeatureAndAppl.productFeatureTypeId)?if_exists)}</td>
          <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${(productFeatureAndAppl.productFeatureCategoryId)?if_exists}&amp;productId=${(productFeatureAndAppl.productId)?if_exists}</@ofbizUrl>" class="buttontext">
              ${(curProductFeatureCategory.description)?if_exists}
              [${(productFeatureAndAppl.productFeatureCategoryId)?if_exists}]</a></td>
    <#assign hasntStarted = false>
    <#if (productFeatureAndAppl.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productFeatureAndAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
          <td <#if hasntStarted> style='color: red;'</#if>>${(productFeatureAndAppl.fromDate)?if_exists}</td>
          <td>
    <#assign hasExpired = false>
    <#if (productFeatureAndAppl.getTimestamp("thruDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productFeatureAndAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
            <#if hasExpired><#assign class="alert"></#if>
            <@htmlTemplate.renderDateTimeField name="thruDate_o_${productFeatureAndAppl_index}" event="" action="" className="${class!''}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(productFeatureAndAppl.thruDate)?if_exists}" size="25" maxlength="30" id="thruDate_o_${productFeatureAndAppl_index}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
            <input type="text" size='6' name='amount_o_${productFeatureAndAppl_index}' value='${(productFeatureAndAppl.amount)?if_exists}' />
            <input type="text" size='5' name='sequenceNum_o_${productFeatureAndAppl_index}' value='${(productFeatureAndAppl.sequenceNum)?if_exists}' />
            <select name='productFeatureApplTypeId_o_${productFeatureAndAppl_index}' size="1">
    <#if (productFeatureAndAppl.productFeatureApplTypeId)?exists>
              <option value='${(productFeatureAndAppl.productFeatureApplTypeId)?if_exists}'><#if curProductFeatureApplType?exists> ${(curProductFeatureApplType.get("description",locale))?if_exists} <#else> [${productFeatureAndAppl.productFeatureApplTypeId}]</#if></option>
              <option value='${productFeatureAndAppl.productFeatureApplTypeId}'> </option>
    </#if>
      <#list productFeatureApplTypes as productFeatureApplType>
              <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}'>${(productFeatureApplType.get("description",locale))?if_exists} </option>
    </#list>
            </select>
          </td>
          <td align="right">
            <input type="checkbox" name="_rowSubmit_o_${productFeatureAndAppl_index}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');highlightRow(this,'productFeatureId_tableRow_${productFeatureAndAppl_index}');" />
          </td>
          <td>
            <a href="javascript:document.RemoveFeatureFromProduct_o_${productFeatureAndAppl_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
          </td>
        </tr>
    <#-- toggle the row color -->
    <#if rowClass == "2">
      <#assign rowClass = "1">
    <#else>
      <#assign rowClass = "2">
    </#if>
  </#list>

        <tr>
          <td colspan="8" align="center">
            <input type="hidden" name="_rowCount" value="${productFeatureAndAppls.size()}"/>
            <input type="submit" value='${uiLabelMap.CommonUpdate}'/>
          </td>
        </tr>
      </table>
    </form>
  <#list productFeatureAndAppls as productFeatureAndAppl>
    <form name= "RemoveFeatureFromProduct_o_${productFeatureAndAppl_index}" method= "post" action= "<@ofbizUrl>RemoveFeatureFromProduct</@ofbizUrl>">
      <input type= "hidden" name= "productId" value= "${(productFeatureAndAppl.productId)?if_exists}"/>
      <input type= "hidden" name= "productFeatureId" value= "${(productFeatureAndAppl.productFeatureId)?if_exists}"/>
      <input type= "hidden" name= "fromDate" value= "${(productFeatureAndAppl.fromDate)?if_exists}"/>
    </form>
  </#list>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ProductAddProductFeatureFromCategory}</h3>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>ApplyFeaturesFromCategory</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="productId" value="${productId}"/>
      <select name='productFeatureCategoryId' size="1">
        <option value='' selected="selected">${uiLabelMap.ProductChooseFeatureCategory}</option>
  <#list productFeatureCategories as productFeatureCategory>
        <option value='${(productFeatureCategory.productFeatureCategoryId)?if_exists}'>${(productFeatureCategory.description)?if_exists} [${(productFeatureCategory.productFeatureCategoryId)?if_exists}]</option>
  </#list>
      </select>
      <select name='productFeatureGroupId' size="1">
        <option value='' selected="selected">${uiLabelMap.ProductChooseFeatureGroup}</option>
  <#list productFeatureGroups as productFeatureGroup>
        <option value='${(productFeatureGroup.productFeatureGroupId)?if_exists}'>${(productFeatureGroup.description)?if_exists} [${(productFeatureGroup.productFeatureGroupId)?if_exists}]</option>
  </#list>
      </select>
      <span class='label'>${uiLabelMap.ProductFeatureApplicationType}: </span>
      <select name='productFeatureApplTypeId' size="1">
  <#list productFeatureApplTypes as productFeatureApplType>
        <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}'
    <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'Y' && productFeatureApplType.productFeatureApplTypeId =="SELECTABLE_FEATURE")>selected="selected"</#if>
    <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'N' && productFeatureApplType.productFeatureApplTypeId?if_exists =="STANDARD_FEATURE")>selected="selected"</#if>
            >${(productFeatureApplType.get("description",locale))?if_exists} </option>
  </#list>
      </select>
      <input type="submit" value='${uiLabelMap.CommonAdd}'/>
    </form>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ProductAddProductFeatureTypeId}</h3>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>ApplyFeatureToProductFromTypeAndCode</@ofbizUrl>" name='addFeatureByTypeIdCode'>
      <input type="hidden" name="productId" value="${productId}"/>
      <span class='label'>${uiLabelMap.ProductFeatureType}: </span>
      <select name='productFeatureTypeId' size="1">
  <#list productFeatureTypes as productFeatureType>
        <option value='${(productFeatureType.productFeatureTypeId)?if_exists}'>${(productFeatureType.get("description",locale))?if_exists} </option>
  </#list>
      </select>
      <span class='label'>${uiLabelMap.CommonIdCode}: </span><input type="text" size='10' name='idCode' value=''/>
      <br />
      <span class='label'>${uiLabelMap.ProductFeatureApplicationType}: </span>
      <select name='productFeatureApplTypeId' size="1">
  <#list productFeatureApplTypes as productFeatureApplType>
        <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}'
    <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'Y' && productFeatureApplType.productFeatureApplTypeId =="SELECTABLE_FEATURE")>selected="selected"</#if>
    <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'N' && productFeatureApplType.productFeatureApplTypeId =="STANDARD_FEATURE")>selected="selected"</#if>
            >${(productFeatureApplType.get("description",locale))?if_exists} </option>
  </#list>
      </select>
      <br />
      <span class='label'>${uiLabelMap.CommonFrom} : </span>
      <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
      <span class='label'>${uiLabelMap.CommonThru} : </span>
      <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="thruDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
      <span class='label'>${uiLabelMap.CommonSequence} : </span><input type="text" size='5' name='sequenceNum'/>
      <input type="submit" value="${uiLabelMap.CommonAdd}"/>
    </form>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ProductAddProductFeatureID}</h3>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>ApplyFeatureToProduct</@ofbizUrl>" name="addFeatureById">
      <input type="hidden" name="productId" value="${productId}"/>
      <span class="label">${uiLabelMap.CommonId}: </span>
      <span class='label'>
        <@htmlTemplate.lookupField formName="addFeatureById" name="productFeatureId" id="productFeatureId" fieldFormName="LookupProductFeature"/>
      </span>
      <span class="label">${uiLabelMap.ProductFeatureApplicationType}: </span>
      <select name="productFeatureApplTypeId" size="1">
  <#list productFeatureApplTypes as productFeatureApplType>
        <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}'
    <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'Y' && productFeatureApplType.productFeatureApplTypeId =="SELECTABLE_FEATURE")>selected="selected"</#if>
    <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'N' && productFeatureApplType.productFeatureApplTypeId =="STANDARD_FEATURE")>selected="selected"</#if>
            >${(productFeatureApplType.get("description",locale))?if_exists} </option>
  </#list>
      </select>
      <br />
      <span class="label">${uiLabelMap.CommonFrom} : </span><@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate2" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
      <span class="label">${uiLabelMap.CommonThru} : </span><@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="thruDate2" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
      <span class="label">${uiLabelMap.CommonSequence} : </span><input type="text" size="5" name="sequenceNum"/>
      <input type="submit" value="${uiLabelMap.CommonAdd}"/>
    </form>
  </div>
</div>
</#if>