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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductAddProductFeatureInBulk} ${uiLabelMap.CommonFor} ${featureCategory.description}</h3>
    </div>
    <div class="screenlet-body">
      <form method='post' action='<@ofbizUrl>BulkAddProductFeatures</@ofbizUrl>' name="selectAllForm">
        <table cellspacing="0" class="basic-table">
          <input type="hidden" name="_useRowSubmit" value="Y" />
          <input type="hidden" name="_checkGlobalScope" value="N" />
          <input type="hidden" name="productFeatureCategoryId" value="${productFeatureCategoryId}" />
          <tr class="header-row">
            <td><b>${uiLabelMap.CommonDescription}</b></td>
            <td><b>${uiLabelMap.ProductFeatureType}</b></td>
            <td><b>${uiLabelMap.ProductIdSeqNum}</b></td>
            <td><b>${uiLabelMap.ProductIdCode}</b></td>
            <td align="right"><b><label>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="Y" checked="checked" class="selectAll" onclick="highlightAllRows(this, 'productFeatureTypeId_tableRow_', 'selectAllForm');" /></label></b></td>
          </tr>
        <#assign rowClass = "2">
        <#list 0..featureNum-1 as feature>
          <tr id="productFeatureTypeId_tableRow_${feature_index}" valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
              <td><input type="text" size='15' name="description_o_${feature_index}" /></td>
              <td><select name='productFeatureTypeId_o_${feature_index}' size="1">
                  <#list productFeatureTypes as productFeatureType>
                  <option value='${productFeatureType.productFeatureTypeId}'>${productFeatureType.get("description",locale)!}</option>
                  </#list>
                  </select>
                  <input name='productFeatureCategoryId_o_${feature_index}' type="hidden" value="${productFeatureCategoryId}" />
              </td>
              <td><input type="text" size='5' name="defaultSequenceNum_o_${feature_index}"" /></td>
              <td><input type="text" size='5' name="idCode_o_${feature_index}" /></td>
              <td align="right"><input type="checkbox" name="_rowSubmit_o_${feature_index}" value="Y" onclick="highlightRow(this,'productFeatureTypeId_tableRow_${feature_index}');" /></td>
          </tr>
          <#-- toggle the row color -->
          <#if rowClass == "2">
            <#assign rowClass = "1">
          <#else>
            <#assign rowClass = "2">
          </#if>
        </#list>
        <input type="hidden" name="_rowCount" value="${featureNum}" />
        <tr><td colspan="11" align="center"><input type="submit" value='${uiLabelMap.CommonCreate}'/></td></tr>
        </table>
      </form>
    </div>
</div>


