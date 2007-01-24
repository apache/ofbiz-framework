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

<div class="head1">${uiLabelMap.ProductAddProductFeatureInBulk} ${uiLabelMap.CommonFor} ${featureCategory.description}</div>

<table border="1" cellpadding='2' cellspacing='0'>
  <form method='POST' action='<@ofbizUrl>BulkAddProductFeatures</@ofbizUrl>' name="selectAllForm">
  <input type="hidden" name="_useRowSubmit" value="Y">
  <input type="hidden" name="_checkGlobalScope" value="N">
  <input type="hidden" name="productFeatureCategoryId" value="${productFeatureCategoryId}">
  <tr class='viewOneTR1'>
    <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFeatureType}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductIdSeqNum}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductIdCode}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="Y" checked="checked" onclick="javascript:toggleAll(this, 'selectAllForm');"></div></td>
  </tr>
<#list 0..featureNum-1 as feature>
  <tr valign="middle" class='viewOneTR1'>
      <td><input type="text" class='inputBox' size='15' name="description_o_${feature_index}"></td>
      <td><select name='productFeatureTypeId_o_${feature_index}' size="1" class='selectBox'>
        <#list productFeatureTypes as productFeatureType>
          <option value='${productFeatureType.productFeatureTypeId}'>${productFeatureType.get("description",locale)?if_exists}</option>
        </#list>
      </select></td>
      <input name='productFeatureCategoryId_o_${feature_index}' type="hidden" value="${productFeatureCategoryId}">
      <td><input type="text" class='inputBox' size='5' name="defaultSequenceNum_o_${feature_index}""></td>
      <td><input type="text" class='inputBox' size='5' name="idCode_o_${feature_index}"></td>
      <td align="right"><input type="checkbox" name="_rowSubmit_o_${feature_index}" value="Y" checked="checked" onclick="javascript:checkToggle(this, 'selectAllForm');"></td>
  </tr>

</#list>
<input type="hidden" name="_rowCount" value="${featureNum}">
<tr><td colspan="11" align="center"><input type="submit" value='${uiLabelMap.CommonCreate}'/></td></tr>
</form>
</table>
