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
<#if !requestParameters.productId?has_content && requestParameters.editMode! == "N">
  <div class="alert alert-light" role="alert">
    Please enter product id.
  </div>
</#if>
<#if allocationPlanInfo.isPlanAlreadyExists>
  <div class="alert alert-light" role="alert">
    Allocation Plan Already Exists.
  </div>
</#if>
<#if requestParameters.productId?has_content && (allocationPlanInfo.itemList.size() == 0)>
  <div class="alert alert-light" role="alert">
    ${uiLabelMap.OrderNoOrderFound}
  </div>
</#if>

<#if requestParameters.editMode?exists>
  <#assign editMode=parameters.editMode>
<#else>
  <#assign editMode="N">
</#if>
<form class="basic-form" method="post" name="CreateAllocationPlan" id="CreateAllocationPlan" 
    action="<@ofbizUrl><#if !requestParameters.productId?has_content || (allocationPlanInfo.itemList.size() == 0)>CreateAllocationPlan?editMode=N<#else>createAllocationPlanAndItems</#if></@ofbizUrl>">
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">
          <#if requestParameters.productId?has_content>
            ${uiLabelMap.OrderPlanHeader}
          <#else>
            ${uiLabelMap.PageTitleCreateAllocationPlan}
          </#if>
        </li>
        <#if (allocationPlanInfo.itemList.size() &gt; 0)>
          <li><input type="submit" id="saveItemsButton" class="buttontext" value="${uiLabelMap.OrderCreatePlan}"></li>
        </#if>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <table class="basic-table" cellspacing="0">
        <tr>
          <td align="center" width="100%">
            <table class="basic-table" cellspacing="0">
              <tr>
                <td class="label">${uiLabelMap.ProductProductId}</td>
                <td>
                  <@htmlTemplate.lookupField value="${requestParameters.productId!}" formName="CreateAllocationPlan" name="productId" id="productId" fieldFormName="LookupProduct"/>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.OrderPlanName}</td>
                <td ><input type="text" name="planName" id="planName" value="${requestParameters.planName!}"/></td>
              </tr>
              <tr>
                <td class="label"/>
                <td>
                  <#if !requestParameters.productId?has_content || allocationPlanInfo.itemList.size() == 0 || allocationPlanInfo.isPlanAlreadyExists>
                    <input type="submit" value="${uiLabelMap.CommonNext}"/>
                  </#if>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
    </div>
  </div>
  <#if requestParameters.productId?has_content && allocationPlanInfo.itemList.size() &gt; 0 && !allocationPlanInfo.isPlanAlreadyExists>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.OrderPlanItems}</li>
          <#if (allocationPlanInfo.itemList.size() &gt; 0)>
            <#if editMode=="Y">
              <li><a href="/ordermgr/control/CreateAllocationPlan?productId=${requestParameters.productId!}&planName=${requestParameters.planName!}&editMode=N" class="buttontext">${uiLabelMap.OrderCancelEdit}</a></li>
           <#else>
              <li><a href="/ordermgr/control/CreateAllocationPlan?productId=${requestParameters.productId!}&planName=${requestParameters.planName!}&editMode=Y" class="buttontext">${uiLabelMap.CommonEdit}</a></li>
            </#if>
          </#if>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td width="10%">${uiLabelMap.OrderSalesChannel}</td>
            <td width="10%">${uiLabelMap.OrderCustomer}</td>
            <td width="10%">${uiLabelMap.FormFieldTitle_orderId}</td>
            <td width="10%">${uiLabelMap.FormFieldTitle_orderItemSeqId}</td>
            <td width="10%">${uiLabelMap.FormFieldTitle_estimatedShipDate}</td>
            <td align="right" width="10%">${uiLabelMap.OrderOrdered}</td>
            <td align="right" width="10%">${uiLabelMap.ProductReserved}</td>
            <td align="right" width="10%">${uiLabelMap.OrderExtValue}</td>
            <td align="right" width="10%">${uiLabelMap.OrderAllocated}</td>
            <#if editMode=="Y">
              <td align="right" width="5%">${uiLabelMap.FormFieldTitle_actionEnumId}</td>
            </#if>
          </tr>
          <input type="hidden" name="itemListSize" value="${allocationPlanInfo.itemList.size()}"/>
          <#list allocationPlanInfo.itemList as item>
            <tr>
              <input type="hidden" name="ioim_${item_index}" value="${item.orderId}"/>
              <input type="hidden" name="ioisim_${item_index}" value="${item.orderItemSeqId}"/>
              <input type="hidden" name="ipsim_${item_index}" value="${item_index+1}"/>
              <td>${item.salesChannel!}</td>
              <td><a href="/partymgr/control/viewprofile?partyId=${item.partyId!}" title="${item.partyId!}">${item.partyName!}</a></td>
              <td><a href="/ordermgr/control/orderview?orderId=${item.orderId!}" title="${item.orderId!}">${item.orderId!}</a></td>
              <td>${item.orderItemSeqId!}</td>
              <td>${item.estimatedShipDate!}</td>
              <td align="right">${item.orderedQuantity!}</td>
              <td align="right">${item.reservedQuantity!}</td>
              <td align="right">${item.orderedValue!}</td>
              <#if editMode=="Y">
                <td><input type="text" name="iaqm_${item_index}" value="${item.allocatedQuantity!}"></td>
                <td align="right">
                  <a href="#" class="up"><img src="/images/arrow-single-up-green.png"/></a>
                  <a href="#" class="down"><img src="/images/arrow-single-down-green.png"/></a>
                </td>
              <#else>
                <td align="right">${item.allocatedQuantity!}</td>
                <input type="hidden" name="iaqm_${item_index}" value="${item.allocatedQuantity!}"/>
              </#if>
            </tr>
          </#list>
          <tr>
            <td colspan="5"><b>${uiLabelMap.CommonTotal}</b></td>
            <td align="right"><b>${allocationPlanInfo.orderedQuantityTotal!}</b></td>
            <td align="right"><b>${allocationPlanInfo.reservedQuantityTotal!}</b></td>
            <td align="right"><b>${allocationPlanInfo.orderedValueTotal!}</b></td>
            <td align="right"></td>
            <#if editMode=="Y">
              <td></td>
            </#if>
          </tr>
        </table>
      </div>
    </div>
  </#if>
</form>