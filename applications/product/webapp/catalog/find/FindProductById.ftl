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
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.ProductFindProductWithIdValue}</div>
    </div>
    <div class="screenlet-body">
        <form name="idsearchform" method="post" action="<@ofbizUrl>FindProductById</@ofbizUrl>" style="margin: 0;">
          <div class="tabletext">${uiLabelMap.CommonId} ${uiLabelMap.CommonValue}: <input type="text" name="idValue" size="20" maxlength="50" value="${idValue?if_exists}">&nbsp;<a href="javascript:document.idsearchform.submit()" class="buttontext">${uiLabelMap.CommonFind}</a></div>
        </form>
    </div>
</div>


<div class="head1">${uiLabelMap.ProductSearchResultsWithIdValue}: [${idValue?if_exists}]</div>


<#if !goodIdentifications?has_content && !idProduct?has_content>
    <br/>
    <div class="head2">&nbsp;${uiLabelMap.ProductNoResultsFound}.</div>
<#else/>
  <table cellpadding="2">
    <#if idProduct?has_content>
        <td>
          <div class="tabletext"><b>[${idProduct.productId}]</b></div>
        </td>
        <td>&nbsp;&nbsp;</td>
        <td>
            <a href="<@ofbizUrl>EditProduct?productId=${idProduct.productId}</@ofbizUrl>" class="buttontext">${(idProduct.internalName)?if_exists}</a>
            <span class="tabletext">(${uiLabelMap.ProductSearchResultsFound})</span>
        </td>
    </#if>
    <#list goodIdentifications as goodIdentification>
        <#assign product = goodIdentification.getRelatedOneCache("Product")/>
        <#assign goodIdentificationType = goodIdentification.getRelatedOneCache("GoodIdentificationType")/>
        <tr>
            <td>
                <div class="tabletext"><b>[${product.productId}]</b></div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td>
                <a href="<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>" class="buttontext">${(product.internalName)?if_exists}</a>
                <span class="tabletext">(${uiLabelMap.ProductSearchResultsFound} <b>${goodIdentificationType.get("description",locale)?default(goodIdentification.goodIdentificationTypeId)}</b>.)</span>
            </td>
        </tr>
    </#list>
  </table>
</#if>
