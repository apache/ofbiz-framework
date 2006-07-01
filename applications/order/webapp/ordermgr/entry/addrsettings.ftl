<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>

<form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform"> 
  <input type="hidden" name="finalizeMode" value="ship"/>
  <table width="100%" border="0" align="center" cellspacing='0' cellpadding='0' class='boxoutside'>
    <tr><td>

      <#-- header table -->

      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">${uiLabelMap.OrderSelectAShippingAddress}</div>
          </td>
        </tr>
      </table>

      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr><td colspan="3"><hr class="sepbar"/></td></tr>

        <#-- postal addresses for chosen id -->

        <#if partyContactMechPurposes?exists>
          <#list partyContactMechPurposes as partyContactMechPurpose>
            <#assign shippingAddress = partyContactMechPurpose.getRelatedOne("PostalAddress")/>
                    
            <#-- skip non-postal addresses -->

            <#if shippingAddress.toName?exists>
              <tr> 
                <td valign="top" nowrap>
                  <input type="radio" name="shipping_contact_mech_id" value="${partyContactMechPurpose.contactMechId}">
                </td>
                <td nowrap>&nbsp;&nbsp;&nbsp;&nbsp;</td>
                <td width="100%">
                  <div class="tabletext">
                    <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonToName}:</b>&nbsp;${shippingAddress.toName}<br/></#if>
                    <#if shippingAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b>&nbsp;${shippingAddress.attnName}<br/></#if>
                    <#if shippingAddress.address1?has_content>${shippingAddress.address1}<br/></#if>
                    <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br/></#if>
                    <#if shippingAddress.city?has_content>${shippingAddress.city}</#if>
                    <#if shippingAddress.stateProvinceGeoId?has_content><br/>${shippingAddress.stateProvinceGeoId}</#if>
                    <#if shippingAddress.postalCode?has_content><br/>${shippingAddress.postalCode}</#if>
                    <#if shippingAddress.countryGeoId?has_content><br/>${shippingAddress.countryGeoId}</#if>
                  </div>
                </td>
              </tr>
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
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
