<script language="javascript1.2">
function submit_add() {
    window.close();
    document.addSubSite.submit();
}
function win_cancel() {
    window.close();
}
</script>

<form name="addSubSite" method="post" action="<@ofbizUrl>postNewSubSite?rootForumId=${requestParameters.rootForumId}</@ofbizUrl>">
<table cellspacing="8">
  <tr>
    <td align="right">Site Name:</td>
    <td align="left"><input type="text" size="20" name="contentName"/></td>
  </tr>
  <tr>
    <td align="right">Site Description:</td>
    <td align="left"><input type="text" size="40" name="description"/></td>
  </tr>
  <tr>
    <td align="right">Posted Msg Default Status:</td>
    <td align="left">
      <select name="statusId">
        <option value="BLOG_DRAFT">Draft - not attached to any site</option>
        <option value="BLOG_SUBMITTED">Submitted - but must be approve (moderated)</option>
        <option value="BLOG_PUBLISHED">Publish immediately</option>
      </select>
    </td>
  </tr>
  <tr>
    <td colspan="2"><input type="submit" name="submitBtn" value="Create"/></td>
    <#--
    <td align="right"><a href="javascript:submit_add()">Create</a></td>
    <td align="right"><a href="javascript:win_cancel()">Cancel</a></td>
    -->
  </tr>
</table>
<input type="hidden" name="contentIdTo" value="${requestParameters.parentForumId}" />
<input type="hidden" name="ownerContentId" value="${requestParameters.parentForumId}" />
<input type="hidden" name="contentTypeId" value="WEB_SITE_PUB_PT" />
<input type="hidden" name="contentAssocTypeId" value="SUBSITE" />

</form>
