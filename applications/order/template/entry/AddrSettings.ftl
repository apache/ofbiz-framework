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

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>

<form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
  <input type="hidden" name="finalizeMode" value="ship"/>
  <table width="100%" border="0" align="center" cellspacing='0' cellpadding='0' class='boxoutside'>
    <tr><td>

      <#-- header table -->

      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle">
            <div class="boxhead">${uiLabelMap.OrderSelectAShippingAddress}</div>
          </td>
        </tr>
      </table>

      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr><td colspan="3"><hr /></td></tr>

        <#-- postal addresses for chosen id -->

        <#if partyContactMechPurposes??>
          <#list partyContactMechPurposes as partyContactMechPurpose>
            <#assign shippingAddress = partyContactMechPurpose.getRelatedOne("PostalAddress", false)/>

            <#-- skip non-postal addresses -->

            <#if shippingAddress.toName??>
              <tr>
                <td valign="top" nowrap="nowrap">
                  <input type="radio" name="shipping_contact_mech_id" value="${partyContactMechPurpose.contactMechId}" />
                </td>
                <td nowrap="nowrap">&nbsp;&nbsp;&nbsp;&nbsp;</td>
                <td width="100%">
                  <div>
                    <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonToName}:</b>&nbsp;${shippingAddress.toName}<br /></#if>
                    <#if shippingAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b>&nbsp;${shippingAddress.attnName}<br /></#if>
                    <#if shippingAddress.address1?has_content>${shippingAddress.address1}<br /></#if>
                    <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br /></#if>
                    <#if shippingAddress.city?has_content>${shippingAddress.city}</#if>
                    <#if shippingAddress.stateProvinceGeoId?has_content><br />${shippingAddress.stateProvinceGeoId}</#if>
                    <#if shippingAddress.postalCode?has_content><br />${shippingAddress.postalCode}</#if>
                    <#if shippingAddress.countryGeoId?has_content><br />${shippingAddress.countryGeoId}</#if>
                  </div>
                </td>
              </tr>
              <tr><td colspan="3"><hr /></td></tr>
            </#if>
          </#list>
        </#if>

      </table>

    </td></tr>
  </table>
</form>

<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
