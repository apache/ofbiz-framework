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
<div id="searchProductsResults" style="display:none">
  <form method="post" action="javascript:void(0);" id="SearchProductsResultsForm" name="SearchProductsResultsForm">
    <table>
      <tr>
        <td width="25%">
          <label for="searchByProductIdValue"><b>&nbsp;${uiLabelMap.ProductGoodIdentification}</b></label>
        </td>
        <td width="25%">
          <input type="hidden" id="goodIdentificationTypeId" name="goodIdentificationTypeId" value="" />
          <input type="text" id="searchByProductIdValue" name="searchByProductIdValue"/>
        </td>
        <td width="25%">
          <label for="searchByProductName"><b>&nbsp;${uiLabelMap.ProductProductName}</b></label>
        </td>
        <td width="25%">
          <input type="text" id="searchByProductName" name="searchByProductName"/>
        </td>
      </tr>
      <tr>
        <td width="25%">
          <label for="searchByProductDescription"><b>&nbsp;${uiLabelMap.ProductProductDescription}</b></label>
        </td>
        <td width="25%">
          <input type="text" id="searchByProductDescription" name="searchByProductDescription"/>
        </td>
        <td width="50%" colspan="2" style="text-align:center">
          <input type="submit" value="${uiLabelMap.CommonSearch}" id="searchProductsResultsSearch"/>
          &nbsp;
          <input type="submit" value="${uiLabelMap.CommonCancel}" id="searchProductsResultsCancel"/>
        </td>
      </tr>
    </table>
    <table cellspacing="0" cellpadding="2" class="basic-table">
      <thead class="searchProductsResultsHead">
        <tr class="header-row">
          <td nowrap>&nbsp;</td>
          <td nowrap><b>${uiLabelMap.ProductProductId}</b></td>
          <td nowrap><b>${uiLabelMap.ProductProductName}</b></td>
          <td nowrap><b>${uiLabelMap.ProductProductDescription}</b></td>
        </tr>
      </thead>
      <tbody id="searchProductsResultsList" class="searchProductsResultsCartBody">
        <tr>
          <td colspan="3">
          </td>
        </tr>
      <tbody>
    </table>
  </form>
</div>