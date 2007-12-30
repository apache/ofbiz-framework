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
<#if itemsList?exists>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <div class="boxhead-right">
              <#if 0 < itemsList?size>
                <#if 0 < viewIndex>
                  <a href="<@ofbizUrl>FindProductConfigItems?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonPrevious}</a>
                <#else>
                  <span class="submenutextdisabled">${uiLabelMap.CommonPrevious}</span>
                </#if>
                <#if 0 < listSize>
                  <span class="submenutextinfo">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
                </#if>
                <#if highIndex < listSize>
                  <a href="<@ofbizUrl>FindProductConfigItems?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonNext}</a>
                <#else>
                  <span class="submenutextrightdisabled">${uiLabelMap.CommonNext}</span>
                </#if>
              </#if>
              &nbsp;
            </div>
            <div class="boxhead-left">
              &nbsp;${uiLabelMap.PageTitleFindConfigItems}
            </div>
            <div class="boxhead-fill">&nbsp;</div>
        </div>
        <div class="screenlet-body"> 
            <a href="<@ofbizUrl>EditProductConfigItem</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew} ${uiLabelMap.ProductConfigItem}</a>
              <br/>
            <table cellspacing="0" class="basic-table">
              <tr class="header-row">
                <td align="left">${uiLabelMap.ProductConfigItem}</td>
                <td align="left">${uiLabelMap.CommonType}</td>
                <td align="left">${uiLabelMap.CommonDescription}</td>
                <td>&nbsp;</td>
              </tr>
              <#if itemsList?has_content>
                <#assign rowClass = "2">
                <#list itemsList[lowIndex..highIndex-1] as item>
                  <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                    <td>${item.configItemId} - ${item.configItemName?default("")}</td>
                    <td>
                      <#if item.configItemTypeId?if_exists == "SINGLE">${uiLabelMap.ProductSingleChoice}<#else>${uiLabelMap.ProductMultiChoice}</#if>
                    </td>
                    <td>${item.description?default("No Description")}</td>
                    <td align="right">
                      <a href="<@ofbizUrl>EditProductConfigItem?configItemId=${item.configItemId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                    </td>
                  </tr>
                  <#-- toggle the row color -->
                  <#if rowClass == "2">
                      <#assign rowClass = "1">
                  <#else>
                      <#assign rowClass = "2">
                  </#if>
                </#list>          
              <#else>
                <tr>
                  <td colspan="4"><h3>${uiLabelMap.CommonNo} ${uiLabelMap.ProductConfigItems} ${uiLabelMap.CommonFound}.</h3></td>
                </tr>        
              </#if>
            </table>
        </div>
    </div>
</#if>