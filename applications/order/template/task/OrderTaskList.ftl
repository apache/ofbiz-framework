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

<script type="application/javascript">
<!-- //
    function viewOrder(form) {
        if (form.taskStatus.value == "WF_NOT_STARTED") {
            if (form.delegate.checked) {
                form.action = "<@ofbizUrl>acceptassignment</@ofbizUrl>";
            } else {
                form.action = "<@ofbizUrl>orderview</@ofbizUrl>";
            }
        } else {
            if (form.delegate.checked) {
                form.action = "<@ofbizUrl>delegateassignment</@ofbizUrl>";
            } else {
                form.action = "<@ofbizUrl>orderview</@ofbizUrl>";
            }
        }
        form.submit();
    }
// -->
</script>

<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
<#assign tasksFound = false>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="h3">${uiLabelMap.OrderOrderNeedingAttention}</div>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr>
              <td width='100%'>
                <#if poList?has_content>
                  <#assign tasksFound = true>
                  <table cellspacing="0" class="basic-table">
                    <tr>
                      <td>
                        <h3>${uiLabelMap.OrderOrderPurchaseToBeScheduled}</h3>
                        <table cellspacing="0" class="basic-table hover-bar">
                          <tr class="header-row">
                            <td>${uiLabelMap.OrderOrderNumber}</td>
                            <td>${uiLabelMap.CommonName}</td>
                            <td>${uiLabelMap.OrderOrderDate}</td>
                            <td>${uiLabelMap.CommonStatus}</td>
                            <td width="1" align="right">${uiLabelMap.OrderOrderItems}</td>
                            <td width="1" align="right">${uiLabelMap.OrderItemTotal}</td>
                            <td width="1">&nbsp;&nbsp;</td>
                            <td width="1">&nbsp;&nbsp;</td>
                          </tr>
                          <#assign alt_row = false>
                          <#list poList as orderHeaderAndRole>
                            <#assign orh = Static["org.apache.ofbiz.order.order.OrderReadHelper"].getHelper(orderHeaderAndRole)>
                            <#assign statusItem = orderHeaderAndRole.getRelatedOne("StatusItem", true)>
                            <#assign placingParty = orh.getPlacingParty()!>
                            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                              <td><a href="<@ofbizUrl>orderview?orderId=${orderHeaderAndRole.orderId}</@ofbizUrl>" class='buttontext'>${orderHeaderAndRole.orderId}</a></td>
                              <td>
                                <div>
                                  <#assign partyId = "_NA_">
                                  <#if placingParty?has_content>
                                    <#assign partyId = placingParty.partyId>
                                    <#if "Person" == placingParty.getEntityName()>
                                      <#if placingParty.lastName??>
                                        ${placingParty.lastName}<#if placingParty.firstName??>, ${placingParty.firstName}</#if>
                                      <#else>
                                        ${uiLabelMap.CommonNA}
                                      </#if>
                                    <#else>
                                      <#if placingParty.groupName??>
                                        ${placingParty.groupName}
                                      <#else>
                                        ${uiLabelMap.CommonNA}
                                      </#if>
                                    </#if>
                                  <#else>
                                    ${uiLabelMap.CommonNA}
                                  </#if>
                                </div>
                              </td>
                              <td><span style="white-space: nowrap;">${orderHeaderAndRole.getString("orderDate")}</span></td>
                              <td>${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</td>
                              <td align="right">${orh.getTotalOrderItemsQuantity()?string.number}</td>
                              <td align="right"><@ofbizCurrency amount=orh.getOrderGrandTotal() isoCode=orderHeaderAndRole.currencyUom!/></td>
                              <td width="1">&nbsp;&nbsp;</td>
                              <td align='right'>
                                <a href="<@ofbizUrl>OrderDeliveryScheduleInfo?orderId=${orderHeaderAndRole.orderId}</@ofbizUrl>" class='buttontext'>Schedule&nbsp;Delivery</a>
                              </td>
                            </tr>
                            <#-- toggle the row color -->
                            <#assign alt_row = !alt_row>
                          </#list>
                        </table>
                      </td>
                    </tr>
                  </table>
                </#if>

                <#if partyTasks?has_content>
                  <#assign tasksFound = true>
                  <table cellspacing="0" class="basic-table hover-bar">
                    <tr>
                      <td>
                        <h3>${uiLabelMap.OrderWorkflow}</h3>
                        <table cellspacing="0" class="basic-table">
                          <tr class="header-row">
                            <td><a href="<@ofbizUrl>tasklist?sort=orderId</@ofbizUrl>">${uiLabelMap.OrderOrderNumber}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=name</@ofbizUrl>">${uiLabelMap.CommonName}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=orderDate</@ofbizUrl>">${uiLabelMap.OrderOrderDate}</a></td>
                            <td width="1" align="right"><a href="<@ofbizUrl>tasklist?sort=grandTotal</@ofbizUrl>">Total</a></td>
                            <td width="1">&nbsp;&nbsp;</td>
                            <td><a href="<@ofbizUrl>tasklist?sort=actualStartDate</@ofbizUrl>">${uiLabelMap.OrderStartDateTime}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=priority</@ofbizUrl>">${uiLabelMap.CommonPriority}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=currentStatusId</@ofbizUrl>">${uiLabelMap.CommonMyStatus}</a></td>
                          </tr>
                          <#assign alt_row = false>
                          <#list partyTasks as task>
                            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                              <td>
                                <#assign orderStr = "orderId=" + task.orderId + "&amp;partyId=" + userLogin.partyId + "&amp;roleTypeId=" + task.roleTypeId + "&amp;workEffortId=" + task.workEffortId + "&amp;fromDate=" + task.get("fromDate").toString()>
                                <a href="<@ofbizUrl>orderview?${orderStr}</@ofbizUrl>" class="buttontext">
                                  ${task.orderId}
                                </a>
                              </td>
                              <td>
                                <div>
                                  <#if task.customerPartyId??>
                                    <a href="${customerDetailLink}${task.customerPartyId}${StringUtil.wrapString(externalKeyParam)}" target="partymgr" class="buttontext">${Static["org.apache.ofbiz.order.task.TaskWorker"].getCustomerName(task)}</a>
                                  <#else>
                                    N/A
                                  </#if>
                                </div>
                              </td>
                              <td>
                                <div>
                                  ${task.get("orderDate").toString()}
                                </div>
                              </td>
                              <td width="1" align="right"><@ofbizCurrency amount=task.grandTotal isoCode=orderCurrencyMap.get(task.orderId)/></td>
                              <td width="1">&nbsp;&nbsp;</td>
                              <td>
                                <#if task.actualStartDate??>
                                  <#assign actualStartDate = task.get("actualStartDate").toString()>
                                <#else>
                                  <#assign actualStartDate = "N/A">
                                </#if>
                                <div>${actualStartDate}</div>
                              </td>
                              <td>${task.priority?default("0")}</td>
                              <td>
                                <a href="/workeffort/control/activity?workEffortId=${task.workEffortId}${StringUtil.wrapString(externalKeyParam)}" target="workeffort" class="buttontext">
                                  ${Static["org.apache.ofbiz.order.task.TaskWorker"].getPrettyStatus(task)}
                                </a>
                              </td>
                            </tr>
                            <#-- toggle the row color -->
                            <#assign alt_row = !alt_row>
                          </#list>
                        </table>
                      </td>
                    </tr>
                  </table>
                </#if>

                <#if roleTasks?has_content>
                  <#assign tasksFound = true>
                  <table cellspacing="0" class="basic-table">
                    <tr>
                      <td>
                        <h3>${uiLabelMap.CommonWorkflowActivityUserRole}</h3>
                        <table cellspacing="0" class="basic-table hover-bar">
                          <tr class="header-row">
                            <td><a href="<@ofbizUrl>tasklist?sort=orderId</@ofbizUrl>">${uiLabelMap.OrderOrderNumber}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=name</@ofbizUrl>">${uiLabelMap.CommonName}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=orderDate</@ofbizUrl>">${uiLabelMap.OrderOrderDate}</a></td>
                            <td width="1" align="right"><a href="<@ofbizUrl>tasklist?sort=grandTotal</@ofbizUrl>">${uiLabelMap.CommonTotal}</a></td>
                            <td width="1">&nbsp;&nbsp;</td>
                            <td><a href="<@ofbizUrl>tasklist?sort=actualStartDate</@ofbizUrl>">${uiLabelMap.CommonStartDateTime}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=wepaPartyId</@ofbizUrl>">${uiLabelMap.PartyParty}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=roleTypeId</@ofbizUrl>">${uiLabelMap.PartyRole}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=priority</@ofbizUrl>">${uiLabelMap.CommonPriority}</a></td>
                            <td><a href="<@ofbizUrl>tasklist?sort=currentStatusId</@ofbizUrl>">${uiLabelMap.CommonStatus}</a></td>
                            <td>&nbsp;</td>
                          </tr>
                          <#assign alt_row = false>
                          <#list roleTasks as task>
                            <form method="get" name="F${task.workEffortId}">
                              <input type="hidden" name="orderId" value="${task.orderId}" />
                              <input type="hidden" name="workEffortId" value="${task.workEffortId}" />
                              <input type="hidden" name="taskStatus" value="${task.currentStatusId}" />
                              <#if task.statusId?? && "CAL_SENT" == task.statusId>
                                <input type="hidden" name="partyId" value="${userLogin.partyId}" />
                                <input type="hidden" name="roleTypeId" value="${task.roleTypeId}" />
                                <input type="hidden" name="fromDate" value="${task.get("fromDate").toString()}" />
                              <#else>
                                <input type="hidden" name="partyId" value="${userLogin.partyId}" />
                                <input type="hidden" name="roleTypeId" value="${task.roleTypeId}" />
                                <input type="hidden" name="fromDate" value="${task.get("fromDate").toString()}" />
                                <input type="hidden" name="fromPartyId" value="${task.wepaPartyId}" />
                                <input type="hidden" name="fromRoleTypeId" value="${task.roleTypeId}" />
                                <input type="hidden" name="fromFromDate" value="${task.get("fromDate").toString()}" />
                                <input type="hidden" name="toPartyId" value="${userLogin.partyId}" />
                                <input type="hidden" name="toRoleTypeId" value="${task.roleTypeId}" />
                                <input type="hidden" name="toFromDate" value="${now}" />
                                <input type="hidden" name="startActivity" value="true" />
                              </#if>
                              <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                                <td>
                                  <a href="javascript:viewOrder(document.F${task.workEffortId});" class="buttontext">
                                    ${task.orderId}
                                  </a>
                                </td>
                                <td>
                                  <#if task.customerPartyId??>
                                  <a href="${customerDetailLink}${task.customerPartyId}${StringUtil.wrapString(externalKeyParam)}" target="partymgr" class="buttontext">${Static["org.apache.ofbiz.order.task.TaskWorker"].getCustomerName(task)}</a>
                                  <#else>
                                  &nbsp;
                                  </#if>
                                </td>
                                <td>
                                  <div>
                                    ${task.get("orderDate").toString()}
                                  </div>
                                </td>
                                <td width="1" align="right"><@ofbizCurrency amount=task.grandTotal isoCode=orderCurrencyMap.get(task.orderId)/></td>
                                <td width="1">&nbsp;&nbsp;</td>
                                <td>
                                  <#if task.actualStartDate??>
                                    <#assign actualStartDate = task.get("actualStartDate").toString()>
                                  <#else>
                                    <#assign actualStartDate = "N/A">
                                  </#if>
                                  <div>${actualStartDate}</div>
                                </td>
                                <td>
                                  <#if "_NA_" == task.wepaPartyId>
                                    <div>N/A</div>
                                  <#else>
                                    <a href="${customerDetailLink}${task.wepaPartyId}${StringUtil.wrapString(externalKeyParam)}" target="partymgr" class="buttontext">${task.wepaPartyId}</a>
                                  </#if>
                                </td>
                                <td>${Static["org.apache.ofbiz.order.task.TaskWorker"].getRoleDescription(task)}</td>
                                <td>${task.priority?default("0")}</td>
                                <td>
                                  <a href="/workeffort/control/activity?workEffortId=${task.workEffortId}" target="workeffort" class="buttontext">
                                    ${Static["org.apache.ofbiz.order.task.TaskWorker"].getPrettyStatus(task)}
                                  </a>
                                </td>
                                <#if task.statusId?? && "CAL_SENT" == task.statusId>
                                  <td align="right"><input type="checkbox" name="delegate" value="true" checked="checked" /></td>
                                <#else>
                                  <td align="right"><input type="checkbox" name="delegate" value="true" /></td>
                                </#if>
                              </tr>
                            </form>
                            <#-- toggle the row color -->
                            <#assign alt_row = !alt_row>
                          </#list>
                        </table>
                      </td>
                    </tr>
                  </table>
                </#if>
                <#if !tasksFound>
                  <div>${uiLabelMap.CommonNoTaskAssigned}</div>
                </#if>
              </td>
            </tr>
        </table>
    </div>
</div>
<#else>
  <h3>You do not have permission to view this page. ("ORDERMGR_VIEW" or "ORDERMGR_ADMIN" needed)</h3>
</#if>