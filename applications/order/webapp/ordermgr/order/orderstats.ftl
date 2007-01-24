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

<#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session)>

<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <TD align="left" width='70%' >
            <div class='boxhead'>&nbsp;${uiLabelMap.OrderOrderStatisticsPage}</div>
          </TD>          
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
          
            <!--<div class='head3'>${uiLabelMap.OrderOrderStatisticsPage}</div>-->
            <TABLE width='100%' cellpadding='2' cellspacing='0' border='0'>
              <TR>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td><div class="tableheadtext">${uiLabelMap.CommonToday}</div></td>
                <td><div class="tableheadtext">${uiLabelMap.OrderWTD}</div></td>
                <TD><div class="tableheadtext">${uiLabelMap.OrderMTD}</div></td>
                <TD><div class="tableheadtext">${uiLabelMap.OrderYTD}</div></td>              
              </TR>
              <tr><td colspan="8"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="7">
                  <div class="tableheadtext">${uiLabelMap.OrderOrdersTotals}</div>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderGrossDollarAmountsIncludesAdjustmentsAndPendingOrders}</div></td>
                <td><div class="tabletext">${dayItemTotal}</div></td>
                <td><div class="tabletext">${weekItemTotal}</div></td>
                <td><div class="tabletext">${monthItemTotal}</div></td>
                <td><div class="tabletext">${yearItemTotal}</div></td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderPaidDollarAmountsIncludesAdjustments}</div></td>
                <td><div class="tabletext">${dayItemTotalPaid}</div></td>
                <td><div class="tabletext">${weekItemTotalPaid}</div></td>
                <td><div class="tabletext">${monthItemTotalPaid}</div></td>
                <td><div class="tabletext">${yearItemTotalPaid}</div></td>
              </tr>

              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderPendingPaymentDollarAmountsIncludesAdjustments}</div></td>
                <td><div class="tabletext">${dayItemTotalPending}</div></td>
                <td><div class="tabletext">${weekItemTotalPending}</div></td>
                <td><div class="tabletext">${monthItemTotalPending}</div></td>
                <td><div class="tabletext">${yearItemTotalPending}</div></td>
              </tr>
              <tr><td colspan="8"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="7">
                  <div class="tableheadtext">${uiLabelMap.OrderOrdersItemCounts}</div>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderGrossItemsSoldIncludesPromotionsAndPendingOrders}</div></td>
                <td><div class="tabletext">${dayItemCount?string.number}</div></td>
                <td><div class="tabletext">${weekItemCount?string.number}</div></td>
                <td><div class="tabletext">${monthItemCount?string.number}</div></td>
                <td><div class="tabletext">${yearItemCount?string.number}</div></td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderPaidItemsSoldIncludesPromotions}</div></td>
                <td><div class="tabletext">${dayItemCountPaid?string.number}</div></td>
                <td><div class="tabletext">${weekItemCountPaid?string.number}</div></td>
                <td><div class="tabletext">${monthItemCountPaid?string.number}</div></td>
                <td><div class="tabletext">${yearItemCountPaid?string.number}</div></td>
              </tr>              
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderPendingPaymentItemsSoldIncludesPromotions}</div></td>
                <td><div class="tabletext">${dayItemCountPending?string.number}</div></td>
                <td><div class="tabletext">${weekItemCountPending?string.number}</div></td>
                <td><div class="tabletext">${monthItemCountPending?string.number}</div></td>
                <td><div class="tabletext">${yearItemCountPending?string.number}</div></td>
              </tr>              
              <tr><td colspan="8"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="7">
                  <div class="tableheadtext">${uiLabelMap.OrderOrdersPending}</div>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderWaitingPayment}</div></td>
                <td><div class="tabletext">${waitingPayment?default(0)?string.number}</div></td>
                <td><div class="tabletext">--</div></td>
                <td><div class="tabletext">--</div></td>
                <td><div class="tabletext">--</div></td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderWaitingApproval}</div></td>
                <td><div class="tabletext">${waitingApproval?default(0)?string.number}</div></td>
                <td><div class="tabletext">--</div></td>
                <td><div class="tabletext">--</div></td>
                <td><div class="tabletext">--</div></td>
              </tr> 
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderWaitingCompletion}</div></td>
                <td><div class="tabletext">${waitingComplete?default(0)?string.number}</div></td>
                <td><div class="tabletext">--</div></td>
                <td><div class="tabletext">--</div></td>
                <td><div class="tabletext">--</div></td>
              </tr>                     
              <tr><td colspan="8"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="7">
                  <div class="tableheadtext">${uiLabelMap.OrderStatusChanges}</div>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderOrdered}</div></td>
                <td><div class="tabletext">${dayOrder?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${weekOrder?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${monthOrder?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${yearOrder?size?default(0)?string.number}</div></td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderApproved}</div></td>
                <td><div class="tabletext">${dayApprove?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${weekApprove?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${monthApprove?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${yearApprove?size?default(0)?string.number}</div></td>
              </tr>              
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderCompleted}</div></td>
                <td><div class="tabletext">${dayComplete?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${weekComplete?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${monthComplete?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${yearComplete?size?default(0)?string.number}</div></td>
              </tr>      
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderCancelled}</div></td>
                <td><div class="tabletext">${dayCancelled?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${weekCancelled?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${monthCancelled?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${yearCancelled?size?default(0)?string.number}</div></td>
              </tr>  
              <tr>
                <td>&nbsp;</td>
                <td><div class="tabletext">${uiLabelMap.OrderRejected}</div></td>
                <td><div class="tabletext">${dayRejected?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${weekRejected?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${monthRejected?size?default(0)?string.number}</div></td>
                <td><div class="tabletext">${yearRejected?size?default(0)?string.number}</div></td>
              </tr>                                         
                    
            </TABLE>                    
          </td>
        </tr>
      </table>
    </TD>
  </TR>
</TABLE>

<br/>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
