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
<div id="chooseVariant" style="display:none">
  <form method="post" action="javascript:void(0);" id="ChooseVariantForm">
    <table border="0" width="100%">
      <tr>
        <td colspan="2">&nbsp;</td>
      </tr>
      <tr>
        <td width="100%" align="center" colspan="2">
          <div id="features" style="display: none"/>
        </td>
      </tr>
      <tr>
        <td width="100%" align="center" colspan="2">&nbsp;</td>
      </tr>
      <tr>
        <td width="100%" align="center" colspan="2">
          <b>
            <div id="variantProductDescription"></div>
            <div id="variantProductPrice"></div>
          </b>
        </td>
      </tr>
      <tr id="addAmount" style="display:none">
        <td width="50%" align="right">
          ${uiLabelMap.CommonAmount}
        </td>
        <td width="50%" align="left">
          <input type="text" id="amount" name="add_amount" size="5" value=""/>
        </td>
      </tr>
      <tr>
        <td width="50%" align="right">
          ${uiLabelMap.CommonQuantity}
        </td>
        <td width="50%" align="left">
          <input type="hidden" id="variantProductId" name="add_product_id"/>
          <input type="hidden" id="variant" name="variant" value="Y"/>
          <input type="text" id="variantQuantity" name="variantQuantity" size="5" maxlength="5" value=""/>
        </td>
      </tr>
      <tr>
        <td colspan="2">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input type="submit" value="${uiLabelMap.CommonConfirm}" id="chooseVariantConfirm"/>
          <input type="submit" value="${uiLabelMap.CommonCancel}" id="chooseVariantCancel"/>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <div class="errorPosMessage">
            <span id="chooseVariantFormServerError"/>
          </div>
        </td>
      </tr>
    </table>
  </form>
</div>