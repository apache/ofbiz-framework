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
        <div class="boxhead">&nbsp;Return Items</div>
    </div>
    <div class="screenlet-body">
        <#-- DO NOT CHANGE THE NAME OF THIS FORM, it will break the some of the multi-service pattern features -->
        <#assign selectAllFormName = "selectAllForm"/>
        <form name="selectAllForm" method="post" action="<@ofbizUrl>makeQuickReturn</@ofbizUrl>">
          <input type="hidden" name="_checkGlobalScope" value="Y"/>
          <input type="hidden" name="_useRowSubmit" value="Y"/>
          <input type="hidden" name="fromPartyId" value="${party.partyId}"/>
          <input type="hidden" name="toPartyId" value="${toPartyId?if_exists}"/>
          <input type="hidden" name="orderId" value="${orderId}"/>
          <input type="hidden" name="needsInventoryReceive" value="Y"/>
          <input type="hidden" name="destinationFacilityId" value="${destinationFacilityId}"/>
          <input type="hidden" name="returnHeaderTypeId" value="CUSTOMER_RETURN"/>
          <#if (orderHeader?has_content) && (orderHeader.currencyUom?has_content)>
          <input type="hidden" name="currencyUomId" value="${orderHeader.currencyUom}"/>
          </#if>
          <#include "returnItemInc.ftl"/>
          <table border="0" width="100%" cellpadding="2" cellspacing="0">
            <tr><td colspan="8"><hr class="sepbar"></td></tr>
            <tr>
              <td colspan="8"><div class="head3">${uiLabelMap.OrderReturnShipFromAddress}:</td>
            </tr>
            <tr><td colspan="8"><hr class="sepbar"></td></tr>
            <tr>
              <td colspan="8">
                <table cellspacing="1" cellpadding="2" width="100%">
                  <#list shippingContactMechList as shippingContactMech>
                    <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress")>
                    <tr>
                      <td align="right" width="1%" valign="top" nowrap>
                        <input type="radio" name="originContactMechId" value="${shippingAddress.contactMechId}"  <#if (shippingContactMechList?size == 1)>checked</#if>>
                      </td>
                      <td align="left" width="99%" valign="top" nowrap>
                        <div class="tabletext">
                          <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b>&nbsp;${shippingAddress.toName}<br/></#if>
                          <#if shippingAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b>&nbsp;${shippingAddress.attnName}<br/></#if>
                          <#if shippingAddress.address1?has_content>${shippingAddress.address1}<br/></#if>
                          <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br/></#if>
                          <#if shippingAddress.city?has_content>${shippingAddress.city}</#if>
                          <#if shippingAddress.stateProvinceGeoId?has_content><br/>${shippingAddress.stateProvinceGeoId}</#if>
                          <#if shippingAddress.postalCode?has_content><br/>${shippingAddress.postalCode}</#if>
                          <#if shippingAddress.countryGeoId?has_content><br/>${shippingAddress.countryGeoId}</#if>
                          <#--<a href="<@ofbizUrl>editcontactmech?DONE_PAGE=checkoutoptions&contactMechId=${shippingAddress.contactMechId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonUpdate}]</a>-->
                        </div>
                      </td>
                    </tr>
                  </#list>
                </table>
              </td>
            </tr>
          </table>
        </form>
    </div>
</div>
    
