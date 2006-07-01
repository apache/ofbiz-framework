<#--
 *  Copyright (c) 2001-2006 The Open For Business Project - www.ofbiz.org
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
 *@since      2.1
-->

<div class="screenlet">
    <div class="screenlet-header">
        <#--
        <div style="float: right;">
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="lightbuttontext">[${uiLabelMap.OrderBackHome}]</a>
        </div>
        -->
        <div class="boxhead">${uiLabelMap.OrderHistory}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
          <tr>
            <td width="30%">
              <div class="tabletext"><b>${uiLabelMap.CommonDate}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%">
              <div class="tabletext"><b><nobr>${uiLabelMap.OrderOrder} ${uiLabelMap.OrderNbr}</nobr></b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%">
              <div class="tabletext"><b>${uiLabelMap.CommonAmount}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%">
              <div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%"><b></b></td>                
          </tr>
          <#list orderHeaderList as orderHeader>
            <#assign status = orderHeader.getRelatedOneCache("StatusItem")>                               
            <tr><td colspan="9"><hr class="sepbar"/></td></tr>
            <tr>
              <td>
                <div class="tabletext"><nobr>${orderHeader.orderDate.toString()}</nobr></div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${orderHeader.orderId}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext"><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orderHeader.currencyUom/></div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${status.get("description",locale)}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td align="right">
                <a href="<@ofbizUrl>orderstatus?orderId=${orderHeader.orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
              </td>
            </tr>
          </#list>
          <#if !orderHeaderList?has_content>
            <tr><td colspan="9"><div class="head3">${uiLabelMap.OrderNoOrderFound}</div></td></tr>
          </#if>
        </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.EcommerceDownloadsAvailableTitle}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
          <tr>
            <td width="10%">
              <div class="tabletext"><b><nobr>${uiLabelMap.OrderOrder} ${uiLabelMap.OrderNbr}</nobr></b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="20%">
              <div class="tabletext"><b>${uiLabelMap.ProductProductName}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="10%">
              <div class="tabletext"><b>${uiLabelMap.CommonName}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="40%">
              <div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="10%"><b></b></td>                
          </tr>
          <#list downloadOrderRoleAndProductContentInfoList as downloadOrderRoleAndProductContentInfo>
            <tr><td colspan="9"><hr class="sepbar"/></td></tr>
            <tr>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.orderId}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.productName}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.contentName?if_exists}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.description?if_exists}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td align="right">
                <a href="<@ofbizUrl>downloadDigitalProduct/${downloadOrderRoleAndProductContentInfo.contentName?if_exists}?dataResourceId=${downloadOrderRoleAndProductContentInfo.dataResourceId}</@ofbizUrl>" class="buttontext">Download</a>
              </td>
            </tr>
          </#list>
          <#if !downloadOrderRoleAndProductContentInfoList?has_content>
            <tr><td colspan="9"><div class="head3">${uiLabelMap.EcommerceDownloadNotFound}</div></td></tr>
          </#if>
        </table>
    </div>
</div>
