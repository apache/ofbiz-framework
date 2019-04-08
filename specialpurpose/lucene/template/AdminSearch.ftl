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
<hr />
    <form method="post"  action="/content/control/AdminSearch"  name="searchQuery" style="margin: 0;">
<table border="0" cellpadding="2" cellspacing="0">

<tr>
<td width="20%" align="right">
<span class="tableheadtext">${uiLabelMap.ContentQueryLine}</span>
</td>
<td>&nbsp;</td>
<td width="80%">
<input type="text" class="inputBox" name="queryLine" size="60"/>
</td>
</tr>

<tr>
<td width="20%" align="right">
<span class="tableheadtext">${uiLabelMap.CommonSelect} ${uiLabelMap.ContentCategory}</span>
</td>
<td>&nbsp;</td>
<td width="80%">
<select name="lcSiteId">
  <option value=""></option>
  <@listSiteIds contentId="WebStoreCONTENT" indentIndex=0/>
</select>
</td>
</tr>


<!-- category form -->
<tr>
<td>
  <table border="0" width="100%">
    <tr>
      <td align="right" valign="middle">
        <div>${uiLabelMap.ProductFeatures}:</div>
      </td>
      <td align="right" valign="middle">
        <div>
          ${uiLabelMap.CommonAll} <input type="radio" name="any_or_all" value="all" checked="checked"/>
          ${uiLabelMap.CommonAny} <input type="radio" name="any_or_all" value="any"/>
        </div>
      </td>
    </tr>
<#--
    <tr>
      <td align="right" valign="middle">
        <div>Feature IDs:</div>
      </td>
      <td valign="middle">
        <div>
          <input type="text" class="inputBox" name="SEARCH_FEAT" size="15" value="${requestParameters.SEARCH_FEAT!}"/>&nbsp;
          <input type="text" class="inputBox" name="SEARCH_FEAT2" size="15" value="${requestParameters.SEARCH_FEAT!}"/>&nbsp;
          <input type="text" class="inputBox" name="SEARCH_FEAT3" size="15" value="${requestParameters.SEARCH_FEAT!}"/>&nbsp;
        </div>
      </td>
    </tr>
-->
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findOne("ProductFeatureType", findPftMap, true)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <tr>
        <td align="right" valign="middle">
          <div>${(productFeatureType.description)!}:</div>
        </td>
        <td valign="middle">
          <div>
            <select name="pft_${productFeatureTypeId}">
              <option value=""></option>
              <#list productFeatures as productFeature>
              <option value="${productFeature.productFeatureId}">${productFeature.description?default("No Description")} [${productFeature.productFeatureId}]</option>
              </#list>
            </select>
          </div>
        </td>
      </tr>
    </#list>
    <#if searchConstraintStrings?has_content>
      <tr>
        <td align="right" valign="top">
          <div>${uiLabelMap.CommonLast} ${uiLabelMap.CommonSearch}:</div>
        </td>
        <td valign="top">
            <#list searchConstraintStrings as searchConstraintString>
                <div>&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div>${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
            <div>
              ${uiLabelMap.CommonNew} ${uiLabelMap.CommonSearch} <input type="radio" name="clearSearch" value="Y" checked="checked"/>
              ${uiLabelMap.CommonRefineSearch} <input type="radio" name="clearSearch" value="N"/>
            </div>
        </td>
      </tr>
    </#if>
    </table>
    </td>
</tr>
<tr>
<td width="20%" align="right">
&nbsp;</td>
<td>&nbsp;</td>
<td width="80%" colspan="4">
<input type="submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonFind}"/>
</td>

</tr>
</table>
</form>


<hr />

<#macro listSiteIds contentId indentIndex=0>
  <#assign dummy=Static["org.apache.ofbiz.base.util.Debug"].logInfo("in listSiteIds, contentId:" + contentId,"")/>
  <#assign dummy=Static["org.apache.ofbiz.base.util.Debug"].logInfo("in listSiteIds, indentIndex:" + indentIndex,"")/>
  <#local indent = ""/>
  <#if 0 < indentIndex >
    <#list 0..(indentIndex - 1) as idx>
      <#local indent = indent + "&nbsp;&nbsp;"/>
    </#list>
  </#if>
<@loopSubContent contentId=contentId viewIndex=0 viewSize=9999 contentAssocTypeId="SUBSITE" returnAfterPickWhen="1==1";>
  <option value="${content.contentId?lower_case}">${indent}${content.description}</option>
  <@listSiteIds contentId=content.contentId indentIndex=indentIndex + 1 />
</@loopSubContent>
</#macro>
