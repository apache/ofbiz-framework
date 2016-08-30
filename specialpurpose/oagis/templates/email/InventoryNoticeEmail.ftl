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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title}</title>
  </head>

  <body>
    <h1>${title}</h1>
    <#-- custom logo or text can be inserted here -->
    <br />
    <div>
      <h3>${uiLabelMap.OagisInventoryDescription}</h3>
    </div>
    <div>
      <table cellspacing="0">
        <tr valign="bottom">
          <th width="10%"><b>${uiLabelMap.ProductProduct}</b></th>
          <th width="10%" align="center">${uiLabelMap.OagisInventoryLevelDateTime}</th>
          <th width="10%" align="right">${uiLabelMap.OrderReturnItemInventoryStatus}</th>
          <th width="10%" align="right">${uiLabelMap.OrderQuantity} (Inventory)</th>
          <th width="10%" align="right">${uiLabelMap.OrderQuantity} (Message)</th>
          <th width="10%" align="right">${uiLabelMap.OagisQuantityDiff}.</th>
        </tr>
        <tr><td colspan="10"><hr /></td></tr>
        <#list inventoryMapList as inventoryMap>
          <tr valign="middle">
            <td valign="top"> ${inventoryMap.productId!}</td>
            <td align="right" valign="top"> ${inventoryMap.timestamp!!}</td>
            <td align="right" valign="top"> ${inventoryMap.statusId!!}</td>
            <td align="center" valign="top"> ${inventoryMap.quantityOnHandTotal!!}</td>
            <td align="center" valign="top"> ${inventoryMap.quantityFromMessage!!}</td>
            <td align="right" valign="top"> ${inventoryMap.quantityDiff!!}</td>
          </tr>
        </#list>
        <tr><td colspan="10"><hr /></td></tr>
      </table>
    </div>
  </body>
</html>
