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

<hr/>
    <form method="post"  action="<@ofbizUrl>searchContent</@ofbizUrl>"  name="searchQuery" style="margin: 0;">
<table border="0" cellpadding="2" cellspacing="0">

<tr>
<td width="20%" align="right">
<span class="tableheadtext">${uiLabelMap.EcommerceEnterQueryParameters}</span>
</td>
<td>&nbsp;</td>
<td width="80%" align="left">
<input type="text" class="inputBox" name="queryLine" size="60"/>
</td>
</tr>
<tr>


<!-- category form -->
<tr>
  <table border="0" wdith="100%">
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.ProductFeatures}:</div>
      </td>
      <td align="right" valign="middle">
        <div class="tabletext">
          ${uiLabelMap.CommonAll} <input type="radio" name="any_or_all" value="all" checked>
          ${uiLabelMap.CommonAny} <input type="radio" name="any_or_all" value="any">
        </div>
      </td>
    </tr>
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findByPrimaryKeyCache("ProductFeatureType", findPftMap)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <tr>
        <td align="right" valign="middle">
          <div class="tabletext">${(productFeatureType.get("description",locale))?if_exists}:</div>
        </td>
        <td valign="middle">
          <div class="tabletext">
            <select class="selectBox" name="pft_${productFeatureTypeId}">
              <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
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
          <div class="tabletext">${uiLabelMap.ProductLastSearch}:</div>
        </td>
        <td valign="top">
            <#list searchConstraintStrings as searchConstraintString>
                <div class="tabletext">&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div class="tabletext">${uiLabelMap.ProductSortedBy}: ${searchSortOrderString}</div>
            <div class="tabletext">
              ${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked>
              ${uiLabelMap.ProductRefineSearch}<input type="radio" name="clearSearch" value="N">
            </div>
        </td>
      </tr>
    </#if>
<td width="20%" align="right">
&nbsp;</td>
<td>&nbsp;</td>
<td width="80%" align="left" colspan="4">
<input type="submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonQuery}"/>
</td>

</tr>
</table>
</form>


<hr/>
    ${listWrapper.renderFormString()}

<#macro listSiteIds contentId indentIndex=0>
  <#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in listSiteIds, contentId:" + contentId,"")/>
  <#assign dummy=Static["org.ofbiz.base.util.Debug"].logInfo("in listSiteIds, indentIndex:" + indentIndex,"")/>
  <#local indent = ""/>
  <#if 0 < indentIndex >
    <#list 0..(indentIndex - 1) as idx>
      <#local indent = indent + "&nbsp;&nbsp;"/>
    </#list>
  </#if>
<@loopSubContentCache subContentId=contentId
    viewIndex=0
    viewSize=9999
    contentAssocTypeId="SUBSITE"
    returnAfterPickWhen="1==1";
>
  <option value="${content.contentId?lower_case}">${indent}${content.description}</option>
  <@listSiteIds contentId=content.contentId indentIndex=indentIndex + 1 />
</@loopSubContentCache >
</#macro>
