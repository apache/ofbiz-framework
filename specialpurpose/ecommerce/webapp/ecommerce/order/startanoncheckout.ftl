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

<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
<#if shoppingCart?has_content>
    <#assign shoppingCartSize = shoppingCart.size()>
<#else>
    <#assign shoppingCartSize = 0>
</#if>

<#if (shoppingCartSize > 0)>
  <div class="screenlet">
    <h3>${uiLabelMap.CommonCheckoutAnonymous}</h3>
    <p>${uiLabelMap.CommonCheckoutAnonymousMsg}:<p>
    <form method="post" action="<@ofbizUrl>setCustomer</@ofbizUrl>" style="margin: 0;">
      <div>
        <input type="submit" class="button" value="${uiLabelMap.OrderCheckout}"/>
      </div>
    </form>
    <form method="post" action="<@ofbizUrl>quickAnonCheckout</@ofbizUrl>">
      <div>
        <input type="submit" class="button" value="${uiLabelMap.OrderCheckoutQuick}"/>
      </div>
    </form>
    <form method="post" action="<@ofbizUrl>anonOnePageCheckout</@ofbizUrl>">
      <div>
        <input type="submit" class="button" value="${uiLabelMap.EcommerceOnePageCheckout}"/>
      </div>
    </form>
    <form method="post" action="<@ofbizUrl>googleCheckout</@ofbizUrl>">
      <div>
        <input type="submit" class="button" value="${uiLabelMap.EcommerceCartToGoogleCheckout}"/>
      </div>
    </form>
  </div>
</#if>

