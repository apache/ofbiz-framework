<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
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
            <div class="tabletext">
              <#if requestParameters.hideFields?default("N") == "Y">
                <a href="<@ofbizUrl>FindInventoryEventPlan?hideFields=N${paramList}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonShowLookupFields}</a>
              <#else>
                <#if inventoryList?exists>
                    <a href="<@ofbizUrl>FindInventoryEventPlan?hideFields=Y${paramList}</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonHideFields}</a>
                </#if>
                <a href="javascript:lookupInventory();" class="submenutextright">${uiLabelMap.CommonLookup}</a>                
              </#if>
            </div>
          </td>
        </tr>
      </table>
      <#if requestParameters.hideFields?default("N") != "Y">
      <table width='100%' border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <td align='center' width='100%'>
            <table border='0' cellspacing='0' cellpadding='2'>
              <tr>
                <td width='20%' align='right'><div class='tableheadtext'>${uiLabelMap.ManufacturingProductId}:</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                    <input type='text' size='25' class='inputBox' name='productId' value='${requestParameters.productId?if_exists}'/>
                    <span class='tabletext'>
                      <a href="javascript:call_fieldlookup3(document.lookupinventory.productId,document.lookupinventory.productId_description,'LookupProduct');">
                        <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
                      </a> 
                    </span>
                    <input type='text' size='25' readonly class='inputBox' name='productId_description' value=''/>
                 </td>
              </tr>
              <tr>
                <td width='20%' align='right'><div class='tableheadtext'>${uiLabelMap.CommonFromDate}:</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <input type='text' size='25' class='inputBox' name='eventDate' value='${requestParameters.eventDate?if_exists}'/>
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
          <td align="left"><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
          <td align="left"><div class="tableheadtext">${uiLabelMap.CommonEventDate}</div></td>
          <td align="center">&nbsp</td>
          <td align="right"><div class="tableheadtext">${uiLabelMap.CommonQuantity}</div></td>
          <td align="right"><div class="tableheadtext">${uiLabelMap.ManufacturingTotalQuantity}</div></td>
        </tr>
        <tr>
          <td colspan='7'><hr class='sepbar'></td>
        </tr>
        <#assign count = lowIndex>
        <#assign countProd = 0>
        <#assign productTmp = "">
        <#list inventoryList[lowIndex..highIndex-1] as inven>
            <#assign product = inven.getRelatedOne("Product")>
            <#if ! product.equals( productTmp )>
                <tr bgcolor="lightblue">  
                  <td colspan='4' align="left">
                    <div class='tabletext'><b>&nbsp&nbsp&nbsp&nbsp&nbsp[${inven.productId}]&nbsp/&nbsp${product.productName?if_exists}</b></div>
                  </td>
                  <td colspan='3' align="right">
                    <#assign qoh = qohProduct[countProd]>
                    <#assign countProd = countProd+1>
                    <big><b><div class='tabletext'>${qoh}</div></b></big>
                  </td>
                </tr>
            </#if>
            <#assign productTmp = product>
            <#assign inventoryEventPlannedType = inven.getRelatedOne("InventoryEventPlannedType")>
            <tr class="${rowClass}">
              <td><div class='tabletext'>${inventoryEventPlannedType.get("description",locale)}</div></td>
              <td><div class='tabletext'>${inven.getString("eventDate")}</div></td>
              <td>&nbsp</td>
              <td><div class='tabletext'align="right"> ${inven.getString("eventQuantity")}</div></td>
              <td align="right">
              <#list numberProductList[count..count] as atpDate> 
                <div class='tabletext'>${atpDate}&nbsp&nbsp</div>
              </#list>
              <#assign count=count+1>
              </td>
            </tr>
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
