<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ include file="/includes/envsetup.jsp" %>
<%@ taglib uri='ofbizTags' prefix='ofbiz' %>
<%@ taglib uri='regions' prefix='region' %>

<region:render section='header'/>
<region:render section='appbar'/>

<div class="centerarea">
  <region:render section='appheader'/>
  <div class="contentarea">
    <div style='border: 0; margin: 0; padding: 0; width: 100%;'>
      <table style='border: 0; margin: 0; padding: 0; width: 100%;' cellpadding='0' cellspacing='0'>
        <tr>
          <region:render section='leftbar'/>
          <td width='100%' valign='top' align='left'>
            <region:render section='error'/>
            <region:render section='content'/>
          </td>
          <region:render section='rightbar'/>
        </tr>
      </table>       
    </div>
    <div class='spacer'></div>
  </div>
</div>

<region:render section='footer'/>

