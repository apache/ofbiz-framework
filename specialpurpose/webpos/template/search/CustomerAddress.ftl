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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">
        <a id="billingAddressSelected" href="javascript:void(0);">${uiLabelMap.WebPosBillingAddress}</a>
        &nbsp;
        <a id="shippingAddressSelected" href="javascript:void(0);">${uiLabelMap.WebPosShippingAddress}</a>
      </li>
    </ul>
    <br class="clear" />
  </div>
  <div class="screenlet-body">
    <div id="customerAddress">
      <div id="centerTopBarLeft">
        <input type="hidden" id="billingLocation" name="billingLocation" value="Y"/>
        <input type="hidden" id="shippingLocation" name="shippingLocation" value="N"/>
        <div id="billingAddress">
          <table class="basic-table" cellspacing="0">
          <#if billingPostalAddress??>
            <#if personBillTo??>
            <tr>
              <td><b><#if personBillTo.lastName?has_content>${personBillTo.lastName}</#if> <#if personBillTo.firstName?has_content>${personBillTo.firstName}</#if></b></td>
            </tr>
            </#if>
            <#assign state = billingPostalAddress.getRelatedOne("StateProvinceGeo", false)!/>
            <#assign country = billingPostalAddress.getRelatedOne("CountryGeo", false)!/>
            <tr>
              <td><#if billingPostalAddress.address1?has_content>${billingPostalAddress.address1}</#if></td>
            </tr>
            <tr>
              <td><#if billingPostalAddress.city?has_content>${billingPostalAddress.city},</#if> <#if state?? && state?has_content && state.geoCode?has_content>${state.geoCode}</#if> <#if billingPostalAddress.postalCode?has_content>${billingPostalAddress.postalCode}</#if>
              </td>
            </tr>
            <tr>
              <td><#if country?? && country?has_content &&country.get("geoName", locale)?has_content>${country.get("geoName", locale)}</#if>
              </td>
            </tr>
            <#else>
            <tr>
              <td>&nbsp;</td>
            </tr>
            <tr>
              <td align="center"><b>${uiLabelMap.WebPosNoPartyInformation}</b></td>
            </tr>
          </#if>
          </table>
        </div>
        <div id="shippingAddress" style="display:none">
          <table class="basic-table" cellspacing="0">
          <#if shippingPostalAddress??>
            <#if personShipTo??>
            <tr>
              <td><b><#if personShipTo.lastName?has_content>${personShipTo.lastName}</#if> <#if personShipTo.firstName?has_content>${personShipTo.firstName}</#if></b></td>
            </tr>
            </#if>
            <#assign state = shippingPostalAddress.getRelatedOne("StateProvinceGeo", false)!/>
            <#assign country = shippingPostalAddress.getRelatedOne("CountryGeo", false)!/>
            <tr>
              <td><#if shippingPostalAddress.address1?has_content>${shippingPostalAddress.address1}</#if></td>
            </tr>
            <tr>
              <td><#if shippingPostalAddress.city?has_content>${shippingPostalAddress.city},</#if> <#if state?? && state?has_content && state.geoCode?has_content>${state.geoCode}</#if> <#if shippingPostalAddress.postalCode?has_content>${shippingPostalAddress.postalCode}</#if>
              </td>
            </tr>
            <tr>
              <td>
                <input type="hidden" id="shipToSelected" value="Y"/>
                <#if country?? && country?has_content && country.get("geoName", locale)?has_content>${country.get("geoName", locale)}</#if>
              </td>
            </tr>
            <#else>
            <tr>
              <td>&nbsp;</td>
            </tr>
            <tr>
              <td align="center"><b>${uiLabelMap.WebPosNoPartyInformation}</b></td>
            </tr>
          </#if>
          </table>
        </div>
      </div>
      <div id="centerTopBarRight" >
        ${screens.render("component://webpos/widget/SearchScreens.xml#Parties")}
        <div id="billingPanel">
          <div>&nbsp;<br /></div>
          <div>
            &nbsp;<br />
          </div>
        </div>
        <div id="shipMethodPanel" style="display:none">
          <div id="shipMethodFormServerError"></div>
          <div>
            <label for="shipMethod"><b>${uiLabelMap.WebPosShippingMethod}</b></label>
            <div id="shipMethodSelection"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<script language="JavaScript" type="text/javascript">
  customerAddressSelected();
</script>
