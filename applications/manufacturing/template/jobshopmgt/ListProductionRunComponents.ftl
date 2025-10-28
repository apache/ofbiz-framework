<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ManufacturingMaterials}</li>
      <li>
        <button
          type="submit"
          form="materialActionForm"
          formaction="<@ofbizUrl>releaseProductionRunTaskComponent</@ofbizUrl>"
          class="small-button">
          ${uiLabelMap.CommonRelease}
        </button>
        <button
          type="submit"
          form="materialActionForm"
          formaction="<@ofbizUrl>reserveProductionRunTaskComponent</@ofbizUrl>"
          class="small-button">
          ${uiLabelMap.CommonReserve}
        </button>


        <button
          type="submit"
          form="materialActionForm"
          formaction="<@ofbizUrl>issueProductionRunComponent</@ofbizUrl>"
          class="small-button">
          ${uiLabelMap.ProductIssue}
        </button>
      </li>
    </ul>
    <br class="clear"/>
  </div>

  <div class="screenlet-body">
    <form method="post" action="" id="materialActionForm" name="selectAllForm">
      <input type="hidden" name="_useRowSubmit" value="Y"/>
      <input type="hidden" name="_checkGlobalScope" value="N"/>
      <input type="hidden" name="productionRunId" value="${parameters.productionRunId!}"/>
      <table class="basic-table hover-bar" cellspacing="0">
        <thead>
          <tr class="header-row">
            <th>${uiLabelMap.ManufacturingRoutingTaskId}</th>
            <th>${uiLabelMap.ProductProductName}</th>
            <th>${uiLabelMap.ProductInventoryItem}</th>
            <th>${uiLabelMap.ManufacturingQuantity}</th>
            <th>${uiLabelMap.CommonReservedQty}</th>
            <th>${uiLabelMap.ProductIssuedQuantity}</th>
            <th>
              <input type="checkbox"
                  name="selectAll"
                  value="Y"
                  checked="checked"
                  class="selectAll"
                  onclick="highlightAllRows(this, 'component_tableRow_', 'selectAllForm');"/>
              ${uiLabelMap.CommonAll}
            </th>
          </tr>
        </thead>

        <tbody>
          <#list productionRunComponents as component>
            <#assign product = (delegator.findOne("Product", {"productId", component.productId!}, false))! />
            <#assign workEffort = (delegator.findOne("WorkEffort", {"workEffortId", component.workEffortId!}, false))! />
            <tr id="component_tableRow_${component_index}">
              <td>${workEffort.workEffortName!} [${workEffort.workEffortId}]</td>
              <td>${product.internalName!} [${component.productId}]</td>
              <td>
                <a href="<@ofbizUrl>SetInventoryItem?productionRunId=${parameters.productionRunId!}&workEffortId=${component.workEffortId!}&productId=${component.productId!}&wegsProductId=${component.productId!}&requiredQuantity=${component.estimatedQuantity!}</@ofbizUrl>"
                   class="buttontext">
                  ${uiLabelMap.CommonSet} ${uiLabelMap.ProductInventoryItem}
                </a>
              </td>
              <td>${component.estimatedQuantity}</td>
              <td>0</td>
              <td>0</td>
              <td>
                <input type="checkbox"
                       name="_rowSubmit_o_${component_index}"
                       value="Y"
                       onclick="highlightRow(this, 'component_tableRow_${component_index}');"/>
                <input type="hidden" name="workEffortId_o_${component_index}" value="${component.workEffortId!}"/>
                <input type="hidden" name="productId_o_${component_index}" value="${component.productId!}"/>
                <input type="hidden" name="wegsProductId_o_${component_index}" value="${component.productId!}"/>
                <input type="hidden" name="requiredQuantity_o_${component_index}" value="${component.estimatedQuantity!}"/>
                <input type="hidden" name="facilityId_o_${component_index}" value="${workEffort.facilityId!}"/>
              </td>
            </tr>
          </#list>
        </tbody>

        <input type="hidden" name="_rowCount" value="${productionRunComponents?size}"/>
      </table>
    </form>
  </div>
</div>
