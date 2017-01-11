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

<script language="JavaScript" type="text/javascript">
  <!--
  var clicked = 0;
  function processOrder() {
    if (clicked == 0) {
      clicked++;
      //window.location.replace("<@ofbizUrl>processorder</@ofbizUrl>
      document.${parameters.formNameValue}.processButton.value = "${uiLabelMap.OrderSubmittingOrder}";
      document.${parameters.formNameValue}.processButton.disabled = true;
      document.${parameters.formNameValue}.submit();
    } else {
      showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.YoureOrderIsBeingProcessed}");
    }
  }
  // -->
</script>

<h1>${uiLabelMap.OrderFinalCheckoutReview}</h1>
<#if !isDemoStore?? && isDemoStore>
  <p>${uiLabelMap.OrderDemoFrontNote}.</p>
</#if>

<#if cart?? && 0 < cart.size()>
  ${screens.render("component://ecommerce/widget/OrderScreens.xml#orderheader")}
  <br/>
  ${screens.render("component://ecommerce/widget/OrderScreens.xml#orderitems")}
  <table border="0" cellpadding="1" width="100%">
    <tr>
      <td colspan="4">
        &nbsp;
      </td>
      <td align="right">
        <form type="post" action="<@ofbizUrl>processorder</@ofbizUrl>" name="${parameters.formNameValue}">
          <#if (requestParameters.checkoutpage)?has_content>
            <input type="hidden" name="checkoutpage" value="${requestParameters.checkoutpage}"/>
          </#if>
          <#if (requestAttributes.issuerId)?has_content>
            <input type="hidden" name="issuerId" value="${requestAttributes.issuerId}"/>
          </#if>
          <input type="button" name="processButton" value="${uiLabelMap.OrderSubmitOrder}" onclick="processOrder();"
                 class="mediumSubmit"/>
        </form>
      <#-- doesn't work with Safari, seems to work with IE, Mozilla
           <a href="#" onclick="processOrder();" class="buttontextbig">[${uiLabelMap.OrderSubmitOrder}]&nbsp;</a> -->
      </td>
    </tr>
  </table>
<#else>
  <h3>${uiLabelMap.OrderErrorShoppingCartEmpty}.</h3>
</#if>
