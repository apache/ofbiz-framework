  <div id="partyContentList">   
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.MyPageAttachFile}</h3>
    </div>
    <div class="screenlet-body">
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
                <a href="<@ofbizUrl>removeAttachFile/EditCommunicationEvent?partyId=${partyId}&communicationEventTypeId=EMAIL_COMMUNICATION&communicationEventId=${commContent.communicationEventId}&contentId=${commContent.contentId}&fromDate=${fromDate}</@ofbizUrl>">${uiLabelMap.CommonRemove}</a>
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
  </div>