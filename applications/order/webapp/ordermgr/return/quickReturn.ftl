<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
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
    
