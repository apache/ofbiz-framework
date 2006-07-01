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
 *@version    $Rev$
 *@since      3.0
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <div class="tabletext">
              ${screens.render(anonymoustrailScreen)}
            </div>
        </div>
        <div class='boxhead'>&nbsp;${uiLabelMap.OrderShippingInformation}</div>
    </div>
    <div class="screenlet-body">
        <#if useEntityFields?default("N") == "Y">
          <form method="post" action="<@ofbizUrl>changeShippingAddress</@ofbizUrl>" name="shipsetupform">
            <input type="hidden" name="contactMechId" value="${(postalFields.contactMechId)?if_exists}"/>
        <#else>
          <form method="post" action="<@ofbizUrl>enterShippingAddress</@ofbizUrl>" name="shipsetupform">
            <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS"/>
            <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
        </#if>
        <input type="hidden" name="partyId" value="${cart.getPartyId()?default("_NA_")}"/>
        <input type="hidden" name="finalizeMode" value="ship"/>

        <table width="100%" border="0" cellpadding="1" cellspacing="0">
          <tr>
            <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.OrderShippingAddress}</div></td>
            <td width="5">&nbsp;</td>
            <td width="74%">&nbsp;</td>
          </tr>
          ${screens.render("component://ecommerce/widget/OrderScreens.xml#genericaddress")}
          <tr>
            <td colspan="3" align="center"><input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}"/></td>
          </tr>
        </table>
        </form>
    </div>
</div>
