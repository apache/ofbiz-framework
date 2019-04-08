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
        <h3>${uiLabelMap.PageTitleEditProductFeatureGroups}</h3>
    </div>
    <div class="screenlet-body">
        <br />
        <table cellspacing="0" class="basic-table">
          <tr class="header-row">
            <td><b>${uiLabelMap.CommonId}</b></td>
            <td><b>${uiLabelMap.CommonDescription}</b></td>
            <td><b>&nbsp;</b></td>
            <td><b>&nbsp;</b></td>
          </tr>
          <#assign rowClass = "2">
          <#list productFeatureGroups as productFeatureGroup>
            <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                <td><a href='<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId}</@ofbizUrl>' class="buttontext">${productFeatureGroup.productFeatureGroupId}</a></td>
                <td>
                    <form method='post' action='<@ofbizUrl>UpdateProductFeatureGroup</@ofbizUrl>'>
                    <input type='hidden' name="productFeatureGroupId" value="${productFeatureGroup.productFeatureGroupId}" />
                    <input type='text' size='30' name="description" value="${productFeatureGroup.description!}" />
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                    </form>
                </td>
                <td><a href='<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId}</@ofbizUrl>' class="buttontext">${uiLabelMap.ProductFeatureGroupAppls}</a></td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "2">
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
        <h3>${uiLabelMap.ProductCreateProductFeatureGroup}</h3>
    </div>
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>CreateProductFeatureGroup</@ofbizUrl>">
          <br />
          <table cellspacing="0" class="basic-table">
            <tr>
              <td class="label">${uiLabelMap.CommonDescription}:</td>
              <td><input type="text" size='30' name='description' value='' /></td>
            </tr>
            <tr>
              <td colspan='2'><input type="submit" value="${uiLabelMap.CommonCreate}" /></td>
            </tr>
          </table>
        </form>
        <br />
    </div>
</div>