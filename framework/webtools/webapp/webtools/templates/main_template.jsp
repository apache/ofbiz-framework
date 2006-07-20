<%--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
--%>
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
