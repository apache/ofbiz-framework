  <div id="partyContentList">
      <#if commEventContent?has_content>
        <table class="basic-table" cellspacing="0">
          <#list commEventContent as commContent>
              <#list partyContent as pContent>
                   <#assign partyId = pContent.partyId/>
                   <#if commContent.contentId == pContent.contentId>
                       <#assign content = pContent.getRelatedOne("Content")>
                       <#assign fromDate = commContent.fromDate>
                       <#assign ptype = pContent.partyContentTypeId/>
                   </#if>
               </#list>
            <tr>
              <td width="30%">
              <#if content?has_content>
                <#if (content.contentName?has_content)>
                    <a href="<@ofbizUrl>img/${content.contentName}?imgId=${content.dataResourceId}</@ofbizUrl>" target="blank"> ${content.contentName?if_exists}</a>
                </#if>
               </#if>
               </td>
              <td class="button-col" width="20%">
              <form name="contentRemoveAttachFile" method="post" action="<@ofbizUrl>removeAttachFile/EditCommunicationEvent</@ofbizUrl>">
                <input type="hidden" name="contentId" value="${commContent.contentId}">
                <input type="hidden" name="communicationEventId" value="${commContent.communicationEventId}">
                <input type="hidden" name="fromDate" value="${commContent.fromDate}">
                <input type="hidden" name="partyId" value="${partyId}">
                <input type="submit" value='${uiLabelMap.CommonDelete}'>
              </form>
              </td>
              <td  width="20%">${commContent.fromDate?if_exists}</td>
              <td  width="30%">&nbsp;</td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoContent}
      </#if>
  </div>