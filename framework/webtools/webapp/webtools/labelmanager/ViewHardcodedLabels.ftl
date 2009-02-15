<div class="screenlet-body">
  <#if parameters.searchLabels?exists>
  <table class="basic-table hover-bar" cellspacing="3">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsLabelManagerRow}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerKey}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerReferences}</td>
    </tr>
    <#assign rowNum = 1>
    <#list referencesList as reference>
      <#assign labelFound = 'N'>
      <#assign refNum = Static["org.ofbiz.webtools.labelmanager.LabelManagerFactory"].getLabelReferenceFile(reference)>
      <#if (refNum > 0)>
        <tr>
          <td>${rowNum}</td>
          <td>${reference}</td>
          <td>${refNum}</td>
        </tr>
        <#assign rowNum = rowNum + 1>
      </#if>
    </#list>
  </table>
  </#if>
</div>
