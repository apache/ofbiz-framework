<hr/>
    <form method="post"  action="/content/control/AdminSearch"  name="searchQuery" style="margin: 0;">
<table border="0" cellpadding="2" cellspacing="0">

<tr>
<td width="20%" align="right">
<span class="tableheadtext">${uiLabelMap.ContentQueryLine}</span>
</td>
<td>&nbsp;</td>
<td width="80%" align="left">
<input type="text" class="inputBox" name="queryLine" size="60"/>
</td>
</tr>
<tr>
<tr>
<td width="20%" align="right">
<span class="tableheadtext">${uiLabelMap.CommonSelect} ${uiLabelMap.ContentCategory}</span>
</td>
<td>&nbsp;</td>
<td width="80%" align="left">
<select name="lcSiteId">
  <option value=""></option>
  <@listSiteIds contentId="WebStoreCONTENT" indentIndex=0/>
</select>
</td>
</tr>


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
<#--
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">Feature IDs:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="SEARCH_FEAT" size="15" value="${requestParameters.SEARCH_FEAT?if_exists}">&nbsp;
          <input type="text" class="inputBox" name="SEARCH_FEAT2" size="15" value="${requestParameters.SEARCH_FEAT?if_exists}">&nbsp;
          <input type="text" class="inputBox" name="SEARCH_FEAT3" size="15" value="${requestParameters.SEARCH_FEAT?if_exists}">&nbsp;
        </div>
      </td>
    </tr>
-->
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findByPrimaryKeyCache("ProductFeatureType", findPftMap)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <tr>
        <td align="right" valign="middle">
          <div class="tabletext">${(productFeatureType.description)?if_exists}:</div>
        </td>
        <td valign="middle">
          <div class="tabletext">
            <select class="selectBox" name="pft_${productFeatureTypeId}">
              <option value="">- ${uiLabelMap.CommonAny} -</option>
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
          <div class="tabletext">${uiLabelMap.CommonLast} ${uiLabelMap.CommonSearch}:</div>
        </td>
        <td valign="top">
            <#list searchConstraintStrings as searchConstraintString>
                <div class="tabletext">&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div class="tabletext">${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
            <div class="tabletext">
              ${uiLabelMap.CommonNew} ${uiLabelMap.CommonSearch} <input type="radio" name="clearSearch" value="Y" checked>
              ${uiLabelMap.CommonRefine} ${uiLabelMap.CommonSearch} <input type="radio" name="clearSearch" value="N">
            </div>
        </td>
      </tr>
    </#if>
<td width="20%" align="right">
&nbsp;</td>
<td>&nbsp;</td>
<td width="80%" align="left" colspan="4">
<input type="submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonFind}"/>
</td>

</tr>
</table>
</form>


<hr/>

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
