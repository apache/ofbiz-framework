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

<div class="head1">${uiLabelMap.ProductFeatureGroup}</div>

<br/>
<table border="1" cellpadding='2' cellspacing='0'>
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.CommonId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>

  <#list productFeatureGroups as productFeatureGroup>
    <tr valign="middle">
      <FORM method='POST' action='<@ofbizUrl>UpdateProductFeatureGroup</@ofbizUrl>'>
        <input type='hidden' name="productFeatureGroupId" value="${productFeatureGroup.productFeatureGroupId}">
        <td><a href='<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId}</@ofbizUrl>' class="buttontext">${productFeatureGroup.productFeatureGroupId}</a></td>
        <td><input type='text' class='inputBox' size='30' name="description" value="${productFeatureGroup.description?if_exists}"></td>
        <td><INPUT type="submit" value="${uiLabelMap.CommonUpdate}"></td>
        <td><a href='<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonEdit}]</a></td>
      </FORM>
    </tr>
  </#list>
</table>
<br/>

<form method="post" action="<@ofbizUrl>CreateProductFeatureGroup</@ofbizUrl>" style='margin: 0;'>
  <div class='head2'>${uiLabelMap.ProductCreateProductFeatureGroup}:</div>
  <br/>
  <table>
    <tr>
      <td><div class='tabletext'>${uiLabelMap.CommonDescription}:</div></td>
      <td><input type="text" class='inputBox' size='30' name='description' value=''></td>
    </tr>
    <tr>
      <td colspan='2'><input type="submit" value="${uiLabelMap.CommonCreate}"></td>
    </tr>
  </table>
</form>
<br/>
