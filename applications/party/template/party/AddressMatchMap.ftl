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

<div id="address-match-map" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.PageTitleAddressMatchMap}</li>
      <li><a href="<@ofbizUrl>findAddressMatch</@ofbizUrl>">${uiLabelMap.PageTitleFindMatches}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <form name="addaddrmap" method="post" action="<@ofbizUrl>createAddressMatchMap</@ofbizUrl>">
    <table class="basic-table" cellspacing="0">
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
          <td><input type="submit" value="${uiLabelMap.CommonCreate}"/></td>
        </tr>
    </table>
    </form>
    <table class="basic-table" cellspacing="0">
      <tr><td colspan="2">&nbsp;</td></tr>
        <tr>
          <td></td>
          <td>
            <form name="importaddrmap" method="post" enctype="multipart/form-data" action="<@ofbizUrl>importAddressMatchMapCsv</@ofbizUrl>">
            <input type="file" name="uploadedFile" size="14"/>
            <input type="submit" value="${uiLabelMap.CommonUpload} CSV"/>
            <p>${uiLabelMap.PartyAddressMatchMessage1}</p>
            </form>
          </td>
        </tr>
    </table>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.PageTitleAddressMatchMap}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
      <#if addressMatchMaps?has_content>
        <table class="basic-table hover-bar" cellspacing="0">
          <tr class="header-row">
            <td>${uiLabelMap.PartyAddressMatchKey}</td>
            <td>=></td>
            <td>${uiLabelMap.PartyAddressMatchValue}</td>
            <td>${uiLabelMap.CommonSequence}</td>
            <td class="button-col"><a href="<@ofbizUrl>clearAddressMatchMap</@ofbizUrl>">${uiLabelMap.CommonClear} ${uiLabelMap.CommonAll}</a></td>
          </tr>
          <#assign alt_row = false>
          <#list addressMatchMaps as map>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>${map.mapKey}</td>
              <td>=></td>
              <td>${map.mapValue}</td>
              <td>${map.sequenceNum!}</td>
              <td class="button-col">
                <form name="removeAddressMatchMap_${map_index}" method="post" action="<@ofbizUrl>removeAddressMatchMap</@ofbizUrl>">
	              <input type="hidden" name="mapKey" value="${map.mapKey}" />
	              <input type="hidden" name="mapValue" value="${map.mapValue}" />
	              <input type="submit" value="${uiLabelMap.CommonDelete}" />
	            </form>
	          </td>
            </tr>
            <#-- toggle the row color -->

            <#assign alt_row = !alt_row>
          </#list>
        </table>
      </#if>
  </div>
</div>
<!-- end addressMatchMap.ftl -->
