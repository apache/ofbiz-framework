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
    <#-- this needs to be fully qualified to appear in email; the server must also be available -->
    <link rel="stylesheet" href="${baseUrl}/images/maincss.css" type="text/css"/>
  </head>

  <body>
    <h1>${title}</h1>
    <#-- custom logo or text can be inserted here -->
    <br/>
    <div class="screenlet-header">
      <div class="boxhead">${uiLabelMap.OagisInventoryDescription}<b></b></div>
    </div>
    <div class="screenlet-body">
      <table class="basic-table" cellspacing="0">
        <tr valign="bottom" class="header-row">
          <td width="10%"><b>${uiLabelMap.ProductProduct}</b></td>
          <td width="10%" align="center"><b>${uiLabelMap.OagisInventoryLevelDateTime}</b></td>
          <td width="10%" align="right"><b>${uiLabelMap.OrderReturnItemInventoryStatus}</b></td>
          <td width="10%" align="right"><b>${uiLabelMap.OrderQuantity} (Inventory)</b></td>
          <td width="10%" align="right"><b>${uiLabelMap.OrderQuantity} (Message)</b></td>
          <td width="10%" align="right"><b>${uiLabelMap.OagisQuantityDiff}.</b></td>
        </tr>
        <tr><td colspan="10"><hr/></td></tr>
        <#assign alt_row = false>
        <#list inventoryMapList as inventoryMap>
          <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td valign="top"> ${inventoryMap.productId?if_exists}</td>
            <td align="right" valign="top"> ${inventoryMap.timestamp?if_exists?if_exists}</td>
            <td align="right" valign="top"> ${inventoryMap.statusId?if_exists?if_exists}</td>
            <td align="center" valign="top"> ${inventoryMap.quantityOnHandTotal?if_exists?if_exists}</td>
            <td align="center" valign="top"> ${inventoryMap.quantityFromMessage?if_exists?if_exists}</td>
            <td align="right" valign="top"> ${inventoryMap.quantityDiff?if_exists?if_exists}</td>
          </tr>
          <#-- toggle the row color -->
          <#assign alt_row = !alt_row>
        </#list>
        <tr><td colspan="10"><hr/></td></tr>
      </table>
    </div>
  </body>
</html>