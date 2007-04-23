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
          <td class='boxhead'></td>
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
                    <span>
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
           <td width="50%" class="boxhead">${uiLabelMap.CommonElementsFound}</td>
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
          <td align="left"><b>${uiLabelMap.CommonType}</b></td>
          <td align="center"><b>&nbsp</b></td>
          <td align="left"><b>${uiLabelMap.CommonDescription}</b></td>
          <td align="left"><b>${uiLabelMap.CommonDate}</b></td>
          <td align="center"><b>&nbsp</b></td>
          <td align="right"><b>${uiLabelMap.CommonQuantity}</b></td>
          <td align="right"><b>${uiLabelMap.ManufacturingTotalQuantity}</b></td>
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
                <#assign errorEvents = delegator.findByAnd("InventoryEventPlanned", Static["org.ofbiz.base.util.UtilMisc"].toMap("inventoryEventPlanTypeId", "ERROR", "productId", inven.productId))>
                <#assign qohEvents = delegator.findByAnd("InventoryEventPlanned", Static["org.ofbiz.base.util.UtilMisc"].toMap("inventoryEventPlanTypeId", "INITIAL_QOH", "productId", inven.productId))>
                <#assign additionalErrorMessage = "">
                <#assign initialQohEvent = null>
                <#assign productFacility = null>
                <#if qohEvents?has_content>
                    <#assign initialQohEvent = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(qohEvents)>
                </#if>
                <#if initialQohEvent != null>
                    <#if initialQohEvent.eventQuantity?has_content>
                        <#assign quantityAvailableAtDate = initialQohEvent.eventQuantity>
                    </#if>
                    <#if initialQohEvent.facilityId?has_content>
                        <#assign productFacility = delegator.findByPrimaryKey("ProductFacility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", initialQohEvent.facilityId, "productId", inven.productId))?if_exists>
                    </#if>
                <#else>
                    <#assign additionalErrorMessage = "No QOH information found, assuming 0.">
                </#if>
                <tr bgcolor="lightblue">  
                  <th align="left">
                      <b>[${inven.productId}]</b>&nbsp;&nbsp;${product.internalName?if_exists}
                  </th>
                  <td align="left">
                    <#if productFacility != null && productFacility?has_content>
                      <div>
                      <b>${uiLabelMap.ProductFacility}:</b>&nbsp;${productFacility.facilityId?if_exists}
                      </div>
                      <div>
                      <b>${uiLabelMap.ProductMinimumStock}:</b>&nbsp;${productFacility.minimumStock?if_exists}
                      </div>
                      <div>
                      <b>${uiLabelMap.ProductReorderQuantity}:</b>&nbsp;${productFacility.reorderQuantity?if_exists}
                      </div>
                      <div>
                      <b>${uiLabelMap.ProductDaysToShip}:</b>&nbsp;${productFacility.daysToShip?if_exists}
                      </div>
                      </#if>
                  </td>
                  <td colspan="5" align="right">
                    <big><b>${quantityAvailableAtDate}</b></big>
                  </td>
                </tr>
                <#if additionalErrorMessage?has_content>
                <tr>
                    <th colspan="7"><font color="red">${additionalErrorMessage}</font></th>
                </tr>
                </#if>
                <#list errorEvents as errorEvent>
                <tr>
                    <th colspan="7"><font color="red">${errorEvent.eventName?if_exists}</font></td>
                </tr>
                </#list>
            </#if>
            <#assign quantityAvailableAtDate = quantityAvailableAtDate?default(0) + inven.getDouble("eventQuantity")>
            <#assign productTmp = product>
            <#assign inventoryEventPlannedType = inven.getRelatedOne("InventoryEventPlannedType")>
            <tr class="${rowClass}">
              <td>${inventoryEventPlannedType.get("description",locale)}</td>
              <td>&nbsp</td>
              <td>${inven.eventName?if_exists}</td>
              <td><font <#if inven.isLate?default("N") == "Y">color='red'</#if>>${inven.getString("eventDate")}</font></td>
              <td>&nbsp</td>
              <td align="right">${inven.getString("eventQuantity")}</td>
              <td align="right">${quantityAvailableAtDate?if_exists}</td>
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
