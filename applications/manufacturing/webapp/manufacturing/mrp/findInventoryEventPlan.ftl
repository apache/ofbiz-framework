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

<script language="JavaScript" type="text/javascript">
<!-- //
function lookupInventory() {
    document.lookupinventory.submit();
}
// -->
</script>

<form method="post" name="lookupinventory" action="<@ofbizUrl>FindInventoryEventPlan</@ofbizUrl>">
<input type="hidden" name="lookupFlag" value="Y"/>
<input type="hidden" name="hideFields" value="Y"/>
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td><div class='boxhead'></div></td>
          <td align='right'>
            <p>
              <#if requestParameters.hideFields?default("N") == "Y">
                <a href="<@ofbizUrl>FindInventoryEventPlan?hideFields=N${paramList}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonShowLookupFields}</a>
              <#else>
                <#if inventoryList?exists>
                    <a href="<@ofbizUrl>FindInventoryEventPlan?hideFields=Y${paramList}</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonHideFields}</a>
                </#if>
                <a href="javascript:lookupInventory();" class="submenutextright">${uiLabelMap.CommonLookup}</a>                
              </#if>
            </p>
          </td>
        </tr>
      </table>
      <#if requestParameters.hideFields?default("N") != "Y">
      <table width='100%' border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <td align='center' width='100%'>
            <table border='0' cellspacing='0' cellpadding='2'>
              <tr>
                <th width='20%' align='right'>${uiLabelMap.ManufacturingProductId}:</th>
                <td width='5%'>&nbsp;</td>
                <td>
                    <input type='text' size='25' name='productId' value='${requestParameters.productId?if_exists}'/>
                    <span class='tabletext'>
                      <a href="javascript:call_fieldlookup2(document.lookupinventory.productId,'LookupProduct');">
                        <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
                      </a> 
                    </span>
                    <input type='text' size='25' readonly name='productId_description' value=''/>
                 </td>
              </tr>
              <tr>
                <th width='20%' align='right'>${uiLabelMap.CommonFromDate}:</th>
                <td width='5%'>&nbsp;</td>
                <td>
                  <input type='text' size='25' name='eventDate' value='${requestParameters.eventDate?if_exists}'/>
                    <a href="javascript:call_cal(document.lookupinventory.eventDate,'');">
                       <img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/>
                     </a>
                </td>
              </tr>        
              <tr>
                <td width="25%" align="center" valign="top">
                <td width="5">&nbsp;</td>
                <td width="75%"> <a href="javascript:lookupInventory();" class="smallSubmit">&nbsp; ${uiLabelMap.CommonLookup} &nbsp;</a></td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
      </#if>
    </td>
  </tr>
</table>
</form>

<#if requestParameters.hideFields?default("N") != "Y">
<script language="JavaScript" type="text/javascript">
<!--//
document.lookupinventory.productId.focus();
//-->
</script>
</#if>
<#if requestParameters.lookupFlag?default("N") == "Y">
<br/>
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <#if inventoryList?exists>
      <#if 0 < inventoryList?size>
       <#assign rowClass = "viewManyTR2">
         <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
          <tr>
           <td width="50%"><div class="boxhead">${uiLabelMap.CommonElementsFound}</div></td>
            <td width="50%">
             <div class="boxhead" align="right">
               
                <#if 0 < viewIndex>
                  <a href="<@ofbizUrl>FindInventoryEventPlan?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}&hideFields=${requestParameters.hideFields?default("N")}${paramList}</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonPrevious}</a>
                <#else>
                  <span class="submenutextdisabled">${uiLabelMap.CommonPrevious}</span>
                </#if>
                <#if 0 < listSize>
                  <span class="submenutextinfo">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
                </#if>
                <#if highIndex < listSize>
                  <a href="<@ofbizUrl>FindInventoryEventPlan?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}&hideFields=${requestParameters.hideFields?default("N")}${paramList}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonNext}</a>
                <#else>
                  <span class="submenutextrightdisabled">${uiLabelMap.CommonNext}</span>
                </#if>
             
              &nbsp;
            </div>
          </td>
        </tr>
      </table>

       <table width='100%' border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <th align="left">${uiLabelMap.CommonDescription}</th>
          <th align="center">&nbsp</th>
          <th align="left">${uiLabelMap.CommonEventName}</th>
          <th align="left">${uiLabelMap.CommonEventDate}</th>
          <th align="center">&nbsp</th>
          <th align="right">${uiLabelMap.CommonQuantity}</th>
          <th align="right">${uiLabelMap.ManufacturingTotalQuantity}</th>
        </tr>
        <tr>
          <td colspan="7"><hr/></td>
        </tr>
        <#assign count = lowIndex>
        <#assign productTmp = "">
        <#list inventoryList[lowIndex..highIndex-1] as inven>
            <#assign product = inven.getRelatedOne("Product")>
            <#if facilityId?exists && facilityId?has_content>
            </#if>
            <#if ! product.equals( productTmp )>
                <#assign quantityAvailableAtDate = 0>
                <#assign initialQohEvent = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("InventoryEventPlanned", Static["org.ofbiz.base.util.UtilMisc"].toMap("inventoryEventPlanTypeId", "INITIAL_QOH", "productId", inven.productId)))>
                <#if initialQohEvent?exists && initialQohEvent.eventQuantity?has_content>
                    <#assign quantityAvailableAtDate = initialQohEvent.eventQuantity>
                </#if>
                <#if initialQohEvent?exists && initialQohEvent.facilityId?has_content>
                    <#assign productFacility = delegator.findByPrimaryKey("ProductFacility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", initialQohEvent.facilityId, "productId", inven.productId))?if_exists>
                </#if>
                <tr bgcolor="lightblue">  
                  <td align="left">
                    <div class='tabletext'>
                      <b>[${inven.productId}]</b>&nbsp;&nbsp;${product.internalName?if_exists}
                    </div>
                  </td>
                  <td align="left">
                    <#if productFacility?exists && productFacility?has_content>
                      <div class='tabletext'>
                      <b>${uiLabelMap.ProductFacility}:</b>&nbsp;${productFacility.facilityId?if_exists}
                      </div>
                      <div class='tabletext'>
                      <b>${uiLabelMap.ProductMinimumStock}:</b>&nbsp;${productFacility.minimumStock?if_exists}
                      </div>
                      <div class='tabletext'>
                      <b>${uiLabelMap.ProductReorderQuantity}:</b>&nbsp;${productFacility.reorderQuantity?if_exists}
                      </div>
                      <div class='tabletext'>
                      <b>${uiLabelMap.ProductDaysToShip}:</b>&nbsp;${productFacility.daysToShip?if_exists}
                      </div>
                      </#if>
                  </td>
                  <td colspan="5" align="right">
                    <big><b><div class='tabletext'>${quantityAvailableAtDate}</div></b></big>
                  </td>
                </tr>
            </#if>
            <#assign quantityAvailableAtDate = quantityAvailableAtDate?default(0) + inven.getDouble("eventQuantity")>
            <#assign productTmp = product>
            <#assign inventoryEventPlannedType = inven.getRelatedOne("InventoryEventPlannedType")>
            <tr class="${rowClass}">
              <td><div class='tabletext'>${inventoryEventPlannedType.get("description",locale)}</div></td>
              <td>&nbsp</td>
              <td>${inven.eventName?if_exists}</td>
              <td><div class='tabletext'>${inven.getString("eventDate")}</div></td>
              <td>&nbsp</td>
              <td><div class='tabletext'align="right"> ${inven.getString("eventQuantity")}</div></td>
              <td align="right">
                <div class='tabletext'>${quantityAvailableAtDate?if_exists}</div>
              </td>
            </tr>
            <#assign count=count+1>
           </#list>
  
       </table>
      <#else>
       <br/>
       <div align="center">${uiLabelMap.CommonNoElementFound}</div>
       <br/>
      </#if>
    </#if>
    </td>
  </tr>
</table>
</#if>
