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

<#if (shoppingCart.getOrderType() == "SALES_ORDER")>
    <#assign associatedProducts = Static["org.apache.ofbiz.order.shoppingcart.product.ProductDisplayWorker"].getRandomCartProductAssoc(request, true)!>
</#if>

<#if associatedProducts?has_content>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <div class="h3">${uiLabelMap.OrderHelpAlsoInterestedIn}</div>
    </div>
    <div class="screenlet-body">
      <table cellspacing="0" cellpadding="1" border="0">
        <#-- random complementary products -->
        <#list associatedProducts as assocProduct>
          <tr>
            <td>
              ${setRequestAttribute("optProduct", assocProduct)}
              ${setRequestAttribute("listIndex", assocProduct_index)}
              ${screens.render(productsummaryScreen)}
            </td>
          </tr>
          <#if assocProduct_has_next>
            <tr><td><hr/></td></tr>
          </#if>
        </#list>
      </table>
    </div>
  </div>
</#if>
