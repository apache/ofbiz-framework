<#if parameters.productionRunId?exists && parameters.productionRunId?has_content>
  <#assign reqQty = (parameters.requiredQuantity!0)?number />
  <#assign totalReservedQty = 0 />
  <#if reservedInventoryItems?has_content>
    <#list reservedInventoryItems as r>
      <#assign totalReservedQty = totalReservedQty + (r.reservedQuantity!0)?number />
    </#list>
  </#if>
  <#assign remainingQty = reqQty - totalReservedQty />

  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">
          Set Inventory Item
        </li>
      </ul>
    </div>

    <div class="screenlet-body">
      <#if reservedInventoryItems?has_content>
        <div class="screenlet-title-bar">
          <ul>
            <li class="h3">
              Reserved Inventory Items
            </li>
          </ul>
        </div>
        <div class="screenlet-body">
        <table class="basic-table standard-table">
          <thead>
            <tr class="header-row-2">
              <th>${uiLabelMap.ProductProductName}</th>
              <th>${uiLabelMap.ContentAssociation}</th>
              <th>${uiLabelMap.ProductInventoryItem}</th>
              <th>${uiLabelMap.ProductAtp}</th>
              <th>${uiLabelMap.CommonLocation}</th>
              <th>${uiLabelMap.CommonReservedQty}</th>
            </tr>
          </thead>
          <tbody>
            <#list reservedInventoryItems as reserved>
              <tr>
                <td>${reserved.productName!}</td>
                <td>${reserved.association!}</td>
                <td>${reserved.inventoryItemId!}</td>
                <td>${reserved.availableToPromiseTotal!}</td>
                <td>${reserved.facilityLocation!}</td>
                <td>${reserved.reservedQuantity!}</td>
              </tr>
            </#list>
          </tbody>
        </table>
        </div>
        <br/>
      </#if>

        <#if (remainingQty <= 0)>
          <div align="right">
            <a href="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${parameters.productionRunId!}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonBack}</a>
          </div>
        </#if>
      </div>

      <#if inventoryItems?has_content && (remainingQty > 0)>
        <br/>
        <div class="screenlet-title-bar">
          <ul>
            <li class="h3">
              Available Inventory Items
            </li>
            <li>
              <button
                type="button"
                class="smallSubmit"
                onclick="window.location.href='<@ofbizUrl>ProductionRunDeclaration?productionRunId=${parameters.productionRunId!}</@ofbizUrl>'">
                ${uiLabelMap.CommonBack}
              </button>
              <button
                type="submit"
                form="reserveComponentByInventoryItem"
                formaction="<@ofbizUrl>reserveProductionRunTaskComponent</@ofbizUrl>"
                class="smallSubmit">
                ${uiLabelMap.CommonReserve}
              </button>
            </li>
          </ul>
        </div>
        <div class="screenlet-body">
        <form method="post" id="reserveComponentByInventoryItem" name="reserveComponentByInventoryItem">
          <input type="hidden" name="_useRowSubmit" value="Y"/>
          <input type="hidden" name="requiredQuantity" value="${parameters.requiredQuantity!}" id="requiredQty"/>
          <input type="hidden" name="productionRunId" value="${parameters.productionRunId!}"/>
          <input type="hidden" name="requireInventory" value="Y"/>
          <input type="hidden" name="workEffortId" value="${parameters.workEffortId!}"/>

          <table class="basic-table standard-table">
            <thead>
              <tr class="header-row-2">
                <th>${uiLabelMap.ProductProductName}</th>
                <th>${uiLabelMap.Association}</th>
                <th>${uiLabelMap.ProductInventoryItem}</th>
                <th>${uiLabelMap.ProductAtp}</th>
                <th>${uiLabelMap.CommonLocation}</th>
                <th>Reserve Qty</th>
                <th>${uiLabelMap.CommonSelect}</th>
              </tr>
            </thead>
            <tbody>
              <#list inventoryItems as item>
                <tr>
                  <td>
                    <input type="hidden" name="facilityId_o_${item_index}" value="${item.facilityId!}"/>
                    <input type="hidden" name="wegsProductId_o_${item_index}" value="${item.productId!}"/>
                    <input type="hidden" name="productId_o_${item_index}" value="${item.productId!}"/>
                    <input type="hidden" name="inventoryItemId_o_${item_index}" value="${item.inventoryItemId!}"/>
                    ${item.productName!}
                  </td>
                  <td>${item.association!}</td>
                  <td>${item.inventoryItemId!}</td>
                  <td>${item.availableToPromiseTotal!}</td>
                  <td>${item.facilityLocation!}</td>
                  <td>
                    <input type="text" name="quantity_o_${item_index}" size="4"/>
                  </td>
                  <td><input type="checkbox" class="quantity_o_${item_index}" name="_rowSubmit_o_${item_index}" value="Y"/></td>
                </tr>
              </#list>
            </tbody>
          </table>
          </div>
        </form>
      </#if>

      <#if !(reservedInventoryItems?has_content || (inventoryItems?has_content && (remainingQty > 0)))>
        <div class="screenlet-body">
          <label class="label">${uiLabelMap.CommonNoRecordFound}</label>
        </div>
      </#if>
    </div>
  </div>
</#if>
