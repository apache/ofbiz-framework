<SCRIPT language="javascript">
    function submitRows(rowCount) {
        var rowCountElement = document.createElement("input");
        rowCountElement.setAttribute("name", "_rowCount");
        rowCountElement.setAttribute("type", "hidden");
        rowCountElement.setAttribute("value", rowCount);
        document.forms.mostrecent.appendChild(rowCountElement);
        document.forms.mostrecent.submit();
    }
</script>

<table width="100%" border="0" >

 <form name="mostrecent" mode="POST" action="<@ofbizUrl>publishResponse</@ofbizUrl>"/>
  <#assign row=0/>
  <#list entityList as content>
    <@checkPermission entityOperation="_ADMIN" targetOperation="CONTENT_PUBLISH" subContentId=forumId >
        <tr>
          <td class="tabletext"> <b>${uiLabelMap.CommonId}:</b>${content.contentId} </td>
          <td class="tabletext"> <b>${uiLabelMap.CommonName}:</b>${content.contentName} </td>
      <@injectNodeTrailCsv subContentId=content.contentId redo="true" contentAssocTypeId="PUBLISH_LINK">
          <td>
  <a class="tabButton" href="<@ofbizUrl>showforumresponse?contentId=${content.contentId}&nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" >${uiLabelMap.CommonView}</a> 
          </td>
          <td class="tabletext">
          <b>${uiLabelMap.CommonSubmitted}:</b>
          <input type="radio" name="statusId_o_${row}" value="BLOG_SUBMITTED" checked/>
          </td>
          <td class="tabletext">
          <b>${uiLabelMap.CommonPublish}:</b>
          <input type="radio" name="statusId_o_${row}" value="BLOG_PUBLISHED"/>
          </td>
        </tr>
          <input type="hidden" name="contentId_o_${row}" value="${content.contentId}"/>
        <tr>
          <td colspan="5" class="tabletext">
          <b>${uiLabelMap.CommonContent}:</b><br/>
            <@renderSubContentCache subContentId=content.contentId/>
          </td>
        </tr>
        <tr> <td colspan="5"> <hr/> </td> </tr>
        <#assign row = row + 1/>
      </@injectNodeTrailCsv >
    </@checkPermission >
  </#list>
    <#if 0 < entityList?size >
        <tr>
          <td colspan="5">
<div class="smallSubmit" ><a href="javascript:submitRows('${row?default(0)}')">${uiLabelMap.CommonUpdate}</a></div>
          </td>
        </tr>
    </#if>
          <input type="hidden" name="forumId" value="${forumId}"/>
 </form>
</table>
