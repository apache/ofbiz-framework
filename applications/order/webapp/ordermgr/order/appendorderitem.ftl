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
                        <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
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
                      <a href="javascript:call_cal(document.quickaddform.itemDesiredDeliveryDate,'${toDayDate} 00:00:00.0');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
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
