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
  <div class="head1">${uiLabelMap.ManufacturingCreateProductionRun}</div>
  <form name="productionRunform" method="post" action="<@ofbizUrl>CreateProductionRunGo</@ofbizUrl>">

  <br/>
  <table width="90%" border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td width='26%' align='right' valign='top'>
        <div class="tableheadtext">${uiLabelMap.ProductProductId}</div>
      </td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="16" name="productId" value="${productionRunData.productId?if_exists}">
        <a href="javascript:call_fieldlookup2(document.productionRunform.productId,'LookupProduct');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'></a>
      </td>
    </tr>
    <tr>
      <td width='26%' align='right' valign='top'><div class="tableheadtext">${uiLabelMap.ManufacturingQuantity}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="6" name="pRQuantity" value="${productionRunData.pRQuantity?if_exists}"></td>
    </tr>
    <tr>
      <td width='26%' align='right' valign='top'><div class="tableheadtext">${uiLabelMap.ManufacturingStartDate}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="19" name="startDate" value="${productionRunData.startDate?if_exists}">
        <a href="javascript:call_cal(document.productionRunform.startDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Click here For Calendar"></a>
      </td>
    </tr>

    <tr>
      <td width='26%' align='right' valign='top'>
        <div class="tableheadtext">${uiLabelMap.ProductFacilityId}</div>
      </td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name='facilityId' class='selectBox'>
          <#if currentFacilityId?has_content>
            <option value="${currentFacilityId.facilityId}">${currentFacilityId.facilityName} [${currentFacilityId.facilityId}]</option>
            <option value="${currentFacilityId.facilityId}">---</option>
          </#if>
          <#list warehouses as warehouse>
            <option value="${warehouse.facilityId}">${warehouse.facilityName} [${warehouse.facilityId}]</option>
          </#list>
        </select>
      </td>
    </tr>


    <tr>
      <td width='26%' align='right' valign='top'><div class="tableheadtext">${uiLabelMap.ManufacturingRoutingId}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="16" name="routingId" value="${productionRunData.routingId?if_exists}">
        <a href="javascript:call_fieldlookup2(document.productionRunform.routingId,'LookupRouting');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'></a>
      </td>
    </tr>
    <tr>
      <td width='26%' align='right' valign='top'><div class="tableheadtext">${uiLabelMap.ManufacturingProductionRunName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="30" name="workEffortName" value="${productionRunData.workEffortName?if_exists}"></td>
    </tr>
    <tr>
      <td width='26%' align='right' valign='top'><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="50" name="description" value="${productionRunData.description?if_exists}"></td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top">
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="submit" value="${uiLabelMap.CommonSubmit}" class="smallSubmit"></td>
    </tr>
  </table>
</form>