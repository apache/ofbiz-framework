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

<!-- begin addressMatchMap.ftl -->
<div id="address-match-map" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <h3>${uiLabelMap.PageTitleAddressMatchMap}</h3>
      <li><a href="<@ofbizUrl>findAddressMatch</@ofbizUrl>">${uiLabelMap.PageTitleFindMatches}</a></li>
    </ul>
    <br class="clear" />
  </div>
  <div class="screenlet-body">
    <table class="basic-table" cellspacing="0">
      <form name="addaddrmap" method="post" action="<@ofbizUrl>createAddressMatchMap</@ofbizUrl>">
        <tr>
          <td class="label">${uiLabelMap.PartyAddressMatchKey}</td>
          <td><input type="text" name="mapKey"/></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.PartyAddressMatchValue}</td>
          <td><input type="text" name="mapValue"/></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonSequence}</td>
          <td><input type="text" size="5" name="sequenceNum" value="0"/></td>
        </tr>
        <tr>
          <td></td>
          <td><a href="javascript:document.addaddrmap.submit()" class="smallSubmit">${uiLabelMap.CommonCreate}</a></td>
        </tr>
      </form>
      <tr><td colspan="2">&nbsp;</td></tr>
      <form name="importaddrmap"method="post" enctype="multipart/form-data" action="<@ofbizUrl>importAddressMatchMapCsv</@ofbizUrl>">
        <tr>
          <td></td>
          <td>
            <input type="file" name="uploadedFile" size="14"/>
            <input type="submit" value="${uiLabelMap.CommonUpload} CSV"/>
            <p>${uiLabelMap.PartyAddressMatchMessage1}</p>
          </td>
        </tr>
      </form>
      <#if addressMatchMaps?has_content>
        <tr><td colspan="2">&nbsp;</td></tr>
        <tr>
          <td colspan="2">
            <table class="basic-table dark-grid" cellspacing="0">
              <tr class="header-row">
                <td>${uiLabelMap.PartyAddressMatchKey}</td>
                <td>=></td>
                <td>${uiLabelMap.PartyAddressMatchValue}</td>
                <td>${uiLabelMap.CommonSequence}</td>
                <td class="button-col"><a href="<@ofbizUrl>clearAddressMatchMap</@ofbizUrl>">${uiLabelMap.CommonClear} ${uiLabelMap.CommonAll}</a></td>
              </tr>
              <#list addressMatchMaps as map>
                <tr>
                  <td>${map.mapKey}</td>
                  <td>=></td>
                  <td>${map.mapValue}</td>
                  <td>${map.sequenceNum?if_exists}</td>
                  <td class="button-col"><a href="<@ofbizUrl>removeAddressMatchMap?mapKey=${map.mapKey}&mapValue=${map.mapValue}</@ofbizUrl>">${uiLabelMap.CommonDelete}</a></td>
                </tr>
              </#list>
            </table>
          </td>
        </tr>
      </#if>
    </table>
  </div>
</div>
<!-- end addressMatchMap.ftl -->
