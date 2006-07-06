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
 *@since      3.5
-->

<#if orderHeader?has_content>
  <table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
    <tr>
      <td width='100%'>
        <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
          <tr>
            <td valign="middle" align="left">
              <div class="boxhead">&nbsp;${uiLabelMap.OrderAddToOrder}</div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
    <tr>
      <td width='100%'>
        <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
          <tr>
            <td>
              <form method="post" action="<@ofbizUrl>appendItemToOrder?${paramString}</@ofbizUrl>" name="appendItemForm" style="margin: 0;">
              <#-- TODO: Presently, this is the ofbiz way of getting the prodCatalog, which is not generic. Replace with a selecatble list defaulting to this instead -->
              <input type="hidden" name="prodCatalogId" value="${Static["org.ofbiz.product.catalog.CatalogWorker"].getCurrentCatalogId(request)}"/>
              <table border="0">
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.ProductProductId} :</div></td>
                  <td><input type="text" class="inputBox" size="25" name="productId" value="${requestParameters.productId?if_exists}"/>
                    <span class='tabletext'>
                      <a href="javascript:call_fieldlookup2(document.appendItemForm.productId,'LookupProduct');">
                        <img src="/images/fieldlookup.gif" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                      </a>
                    </span>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderPrice} :</div></td>
                  <td>
                    <input type="text" class="inputBox" size="6" name="basePrice" value="${requestParameters.price?if_exists}"/>
                    <input type="checkbox" name="overridePrice" value="Y"/>
                    <span class="tabletext">&nbsp;${uiLabelMap.OrderOverridePrice}</span>
                  </td>
                <tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderQuantity} :</div></td>
                  <td><input type="text" class="inputBox" size="6" name="quantity" value="${requestParameters.quantity?default("1")}"/></td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderShipGroup} :</div></td>
                  <td><input type="text" class="inputBox" size="6" name="shipGroupSeqId" value="00001"/></td>
                </tr>
                <tr>
                  <td colspan="2">&nbsp;</td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderDesiredDeliveryDate} :</div></td>
                  <td>
                    <div class="tabletext">
                      <input type="text" class="inputBox" size="25" maxlength="30" name="itemDesiredDeliveryDate"/>
                      <a href="javascript:call_cal(document.quickaddform.itemDesiredDeliveryDate,'${toDayDate} 00:00:00.0');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.CommonComment} :</div></td>
                  <td>
                    <div class="tabletext">
                      <input type="text" class="inputBox" size="25" name="itemComment"/>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td></td>
                  <td><input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToOrder}"/></td>
                </tr>
              </table>
            </form>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</#if>
