<#--
 *  Copyright (c) 2005-2005 The Open For Business Project - www.ofbiz.org
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
-->

<script language="JavaScript">
	var checkBoxNameStart = "view";
	var formName = "findorder";


	function setCheckboxes() {
		// This would be clearer with camelCase variable names
		var allCheckbox = document.forms[formName].elements[checkBoxNameStart + "all"];
		for(i = 0;i < document.forms[formName].elements.length;i++) {
			var elem = document.forms[formName].elements[i];
			if (elem.name.indexOf(checkBoxNameStart) == 0 && elem.name.indexOf("_") < 0 && elem.type == "checkbox") {
				elem.checked = allCheckbox.checked;
			}
		}
	}
	
</script>


<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <form method="post" name="findorder" action="<@ofbizUrl>orderlist</@ofbizUrl>">
        <table border="0" cellspacing="0" cellpadding="0" class="boxbottom">
          <tr>
            <td><div class="tableheadtext">${uiLabelMap.CommonStatus}:</div></td>
            <td>&nbsp;&nbsp;</td>
            <td nowrap>
                <div class="tabletext">
                    <input type="checkbox" name="viewall" value="Y" onclick="javascript:setCheckboxes()" <#if viewall?exists>checked="checked"</#if> />${uiLabelMap.CommonAll}
                    <input type="checkbox" name="viewcreated" value="Y" <#if viewcreated?exists>checked="checked"</#if> />${uiLabelMap.CommonCreated}
                    <input type="checkbox" name="viewprocessing" value="Y" <#if viewprocessing?exists>checked="checked"</#if> />${uiLabelMap.CommonProcessing}
                    <input type="checkbox" name="viewapproved" value="Y" <#if viewapproved?exists>checked="checked"</#if> />${uiLabelMap.CommonApproved}
                    <input type="checkbox" name="viewcompleted" value="Y" <#if viewcompleted?exists>checked="checked"</#if> />${uiLabelMap.CommonCompleted}
                    <input type="checkbox" name="viewsent" value="Y" <#if viewsent?exists>checked="checked"</#if> />${uiLabelMap.CommonSent}
                    <input type="checkbox" name="viewrejected" value="Y" <#if viewrejected?exists>checked="checked"</#if> />${uiLabelMap.CommonRejected}
                    <input type="checkbox" name="viewcancelled" value="Y" <#if viewcancelled?exists>checked="checked"</#if> />${uiLabelMap.CommonCancelled}
                </div>
            </td>            
            <td rowspan="2">&nbsp;&nbsp;</td>
            <td rowspan="2">
              <a href="javascript:document.findorder.submit()" class="buttontext">${uiLabelMap.CommonSubmit}</a><br/>
            </td>
          </tr>
          <tr>
            <td><div class="tableheadtext">${uiLabelMap.CommonType}:</div></td>
            <td>&nbsp;&nbsp;</td>
            <td nowrap>
                <div class="tabletext">
                    <input type="checkbox" name="view_SALES_ORDER" value="Y" <#if view_SALES_ORDER?exists>checked="checked"</#if>/>
                    ${descr_SALES_ORDER}
                    <input type="checkbox" name="view_PURCHASE_ORDER" value="Y" <#if view_PURCHASE_ORDER?exists>checked="checked"</#if>/>
                    ${descr_PURCHASE_ORDER}
                </div>
            </td>            
          </tr>
        </table>
        <br/>&nbsp;
      </form>
    </td>
  </tr>
<#if hasPermission>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left"><div class="boxhead">${uiLabelMap.OrderOrderList}</div></td>
        </tr>
      </table>      
    </td>
  </tr>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td>
            <table width="100%" cellpadding="1" cellspacing="0" border="0">
              <tr>
                <td width="15%">
                  <div class="tabletext"><b>${uiLabelMap.CommonDate}</b></div>
                </td>
                <td width="10%">
                  <div class="tabletext"><b>${uiLabelMap.OrderOrder} #</b></div>
                </td>
                <td width="10%">
                  <div class="tabletext"><b>${uiLabelMap.OrderOrderType}</b></div>
                </td>
                <td width="15%">
                  <div class="tabletext"><b>${uiLabelMap.OrderOrderBillFromParty}</b></div>
                </td>
                <td width="15%">
                  <div class="tabletext"><b>${uiLabelMap.OrderOrderBillToParty}</b></div>
                </td>
                <td width="10%">
                  <div class="tabletext"><b>${uiLabelMap.CommonAmount}</b></div>
                </td>
                <td width="10%">
                  <div class="tabletext"><b>${uiLabelMap.OrderTrackingCode}</b></div>
                </td>
                <td width="15%">
                  <div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div>
                </td>
              </tr>
              <#list orderHeaderList as orderHeader>
                <#assign status = orderHeader.getRelatedOneCache("StatusItem")>                               
                <#assign orh = Static["org.ofbiz.order.order.OrderReadHelper"].getHelper(orderHeader)>
                <#assign billToParty = orh.getBillToParty()?if_exists>
                <#assign billFromParty = orh.getBillFromParty()?if_exists>
                <#if billToParty?has_content>
                    <#assign billToPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", billToParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                    <#assign billTo = billToPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")/>
                    <#-- <#assign billTo = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(billToParty, true)?if_exists> -->
                </#if>
                <#if billFromParty?has_content>
                  <#assign billFrom = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(billFromParty, true)?if_exists>
                </#if>
                <tr><td colspan="8"><hr class="sepbar"/></td></tr>
                <tr>
                  <td>
                    <div class="tabletext">${orderHeader.orderDate?string.medium_short}</div>
                  </td>
                  <td>
                    <div class="tabletext"><a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class="buttontext">${orderHeader.orderId}</a>
                    </div>
                  </td>
                  <td>
                    <div class="tabletext">${orderHeader.getRelatedOneCache("OrderType").get("description",locale)}</div>
                  </td>
                  <td>
                    <div class="tabletext">
                      ${billFrom?if_exists}
                    </div>
                  </td>
                  <td>
                    <div class="tabletext">
                      ${billTo?if_exists}
                    </div>
                  </td>
                  <td>
                    <div class="tabletext"><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orderHeader.currencyUom/></div>
                  </td>
                  <td>
                    <#assign trackingCodes = orderHeader.getRelated("TrackingCodeOrder")>
                    <#list trackingCodes as trackingCode>
                      <div class="tabletext">
                        <#if trackingCode?has_content>
                            <a href="/marketing/control/FindTrackingCodeOrders?trackingCodeId=${trackingCode.trackingCodeId}&externalLoginKey=${requestAttributes.externalLoginKey?if_exists}">${trackingCode.trackingCodeId}</a><br/>
                        </#if>
                      </div>
                    </#list>
                  </td>
                  <td class="tabletext">
                  ${orderHeader.getRelatedOneCache("StatusItem").get("description",locale)}
                  </td>
                </tr>
              </#list>
              <#if !orderHeaderList?has_content>
                <tr><td colspan="8"><div class="head3">${uiLabelMap.OrderNoOrderFound}</div></td></tr>
              </#if>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
<#else>
<tr><td><div class="tableheadtext">${uiLabelMap.OrderViewPermissionError}</div></td></tr>
</#if>
</table>

