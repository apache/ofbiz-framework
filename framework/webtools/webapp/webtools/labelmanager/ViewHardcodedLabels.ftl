<div class="screenlet-body">
  <#if parameters.searchLabels?exists>
  <table class="basic-table hover-bar" cellspacing="3">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsLabelManagerRow}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerKey}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerReferences}</td>
    </tr>
    <#assign rowNumber = 1>
    <#assign rowNum = "2">
    <#list referencesList as reference>
      <#assign labelFound = 'N'>
      <#assign refNum = Static["org.ofbiz.webtools.labelmanager.LabelManagerFactory"].getLabelReferenceFile(reference)>
      <#if (refNum > 0)>
        <tr <#if rowNum == "1">class="alternate-row"</#if>>
          <td>${rowNumber}</td>
          <td>${reference}</td>
          <td align="center"><#if (refNum > 0)><a href="<@ofbizUrl>ViewReferences?sourceKey=${reference}</@ofbizUrl>">${refNum}</a><#else>${refNum}</#if></td>
        </tr>
        <#assign rowNumber = rowNumber + 1>
        <#if rowNum == "2">
          <#assign rowNum = "1">
        <#else>
          <#assign rowNum = "2">
        </#if>
      </#if>
    </#list>
  </table>
  </#if>
</div>
