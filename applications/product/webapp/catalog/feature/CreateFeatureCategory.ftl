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

<form method="post" action="<@ofbizUrl>/CreateFeatureCategory</@ofbizUrl>">
  <br/>
  <table cellspacing="0" class="basic-table">
    <tr>
      <td class="label">${uiLabelMap.CommonDescription}:</td>
      <td><input type="text" size="30" name="description" value=""></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.ProductParentCategory}:</td>
      <td><select name="parentCategoryId" size="1">
        <option value="">&nbsp;</option>
        <#list productFeatureCategories as productFeatureCategory>
          <option value="${productFeatureCategory.productFeatureCategoryId}">${productFeatureCategory.description?if_exists} [${productFeatureCategory.productFeatureCategoryId}]</option>
        </#list>
      </select></td>
    </tr>
    <tr>
      <td colspan="2"><input type="submit" value="${uiLabelMap.CommonCreate}"></td>
    </tr>
  </table>
</form>
<br/>